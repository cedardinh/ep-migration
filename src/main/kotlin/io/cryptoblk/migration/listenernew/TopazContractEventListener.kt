package io.cryptoblk.migration.listenernew

import io.reactivex.disposables.Disposable
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.SmartLifecycle
import org.springframework.stereotype.Component
import org.web3j.protocol.Web3j
import org.web3j.protocol.core.DefaultBlockParameter
import org.web3j.protocol.core.DefaultBlockParameterName
import org.web3j.protocol.core.methods.request.EthFilter
import org.web3j.protocol.core.methods.response.Log
import java.math.BigInteger
import java.util.concurrent.TimeUnit

/**
 * 多合约事件监听器, 将链上日志按合约地址和 topic0 路由到对应工作流。
 */
@Component
class TopazContractEventListener(
    private val web3j: Web3j,
    @Qualifier("lifecycleContractAddress") lifecycleContractAddress: String,
    @Qualifier("paymentContractAddress") paymentContractAddress: String,
    @Qualifier("contactsContractAddress") contactsContractAddress: String,
    workflow: TopazWorkflowService,
    private val checkpointRepository: TopazEventCheckpointRepository
) : SmartLifecycle {
    private val log = LoggerFactory.getLogger(TopazContractEventListener::class.java)

    // 启动时一次性计算好的不可变路由表和过滤条件。
    private val subscriptions = TopazEventRegistry.subscriptions(
        lifecycleAddress = lifecycleContractAddress,
        paymentAddress = paymentContractAddress,
        contactsAddress = contactsContractAddress,
        workflow = workflow
    )
    private val routes = subscriptions.associateBy { it.contractAddress to it.topic0 }
    private val contractAddresses = subscriptions.map { it.contractAddress }.distinct()
    private val topic0s = subscriptions.map { it.topic0 }.distinct()

    @Volatile
    private var running = false
    private var subscriptionStream: Disposable? = null

    init {
        require(routes.size == subscriptions.size) {
            "Duplicate event subscription route found. contract address + topic0 must be unique"
        }
    }

    /** 启动实时日志订阅并执行一次启动补偿扫描。 */
    override fun start() {
        if (running) return

        subscriptionStream = web3j.ethLogFlowable(
            eventFilter(DefaultBlockParameterName.LATEST, DefaultBlockParameterName.LATEST)
        )
            .retryWhen { errors -> errors.delay(5, TimeUnit.SECONDS) }
            .subscribe(::route) { error ->
                log.error("Topaz contract event listener subscription failed: {}", error.message, error)
            }
        running = true

        logFailure("Topaz event compensation failed on startup") {
            compensateMissingEvents()
        }

        log.info(
            "Topaz contract event listener started contracts={} subscriptions={} processedThroughBlock={}",
            contractAddresses.size,
            subscriptions.size,
            checkpointBlock()
        )
    }

    /** 停止实时日志订阅并释放当前订阅流。 */
    override fun stop() {
        subscriptionStream?.dispose()
        subscriptionStream = null
        running = false
        log.info("Topaz contract event listener stopped")
    }

    /** 返回当前监听器是否处于运行状态。 */
    override fun isRunning(): Boolean = running

    /** 让事件监听器在 Spring 生命周期最后阶段启动。 */
    override fun getPhase(): Int = Int.MAX_VALUE

    /** 从数据库记录的最大同步区块之后补扫, 没有记录时以当前链头作为初始水位。 */
    fun compensateMissingEvents(): Int {
        val latestBlock = web3j.ethBlockNumber().send().let { response ->
            if (response.hasError()) {
                error("Failed to fetch latest block: ${response.error?.message ?: "unknown JSON-RPC error"}")
            }
            response.blockNumber
        }

        val processedBlock = checkpointBlock()
        return if (processedBlock == null) {
            saveCheckpoint(latestBlock)
            log.info("Topaz event checkpoint initialized at latest block {}", latestBlock)
            0
        } else {
            scanEvents(processedBlock.add(BigInteger.ONE), latestBlock)
        }
    }

    /** 扫描指定闭区间区块并复用实时订阅的路由处理链路。 */
    fun scanEvents(startBlock: BigInteger, endBlock: BigInteger): Int {
        require(startBlock.signum() >= 0) { "startBlock must be non-negative" }
        require(endBlock.signum() >= 0) { "endBlock must be non-negative" }
        if (endBlock < startBlock) return 0

        val logs = web3j.ethGetLogs(
            eventFilter(DefaultBlockParameter.valueOf(startBlock), DefaultBlockParameter.valueOf(endBlock))
        ).send().let { response ->
            if (response.hasError()) {
                error("Failed to scan Topaz event logs: ${response.error?.message ?: "unknown JSON-RPC error"}")
            }
            (response.logs ?: emptyList()).mapNotNull { logResult ->
                logResult.get() as? Log
            }
        }

        logs.forEach(::route)
        saveCheckpoint(endBlock)

        log.info(
            "Topaz event compensation scanned blocks {}..{} logs={} processedThroughBlock={}",
            startBlock,
            endBlock,
            logs.size,
            endBlock
        )

        return logs.size
    }

    /** 将单条链上日志完成匹配、解码、派发和区块水位推进。 */
    private fun route(chainLog: Log) {
        if (chainLog.isRemoved) return

        val address = chainLog.address?.let(TopazEventRegistry::normalizeAddress) ?: return
        val topic0 = chainLog.topics?.firstOrNull() ?: return
        val subscription = routes[address to topic0] ?: return

        runCatching {
            val response = subscription.decode(chainLog)
            subscription.handle(response)
        }.onFailure { ex ->
            log.error(
                "Topaz event handler failed contract={} event={} tx={} logIndex={}: {}",
                subscription.contractName,
                subscription.eventName,
                chainLog.transactionHash,
                chainLog.logIndex,
                ex.message,
                ex
            )
        }

        chainLog.blockNumber?.let { blockNumber ->
            saveCheckpoint(blockNumber)
        }
    }

    /** 构建覆盖所有订阅合约地址和事件 topic 的 Web3j 日志过滤器。 */
    private fun eventFilter(startBlock: DefaultBlockParameter, endBlock: DefaultBlockParameter): EthFilter {
        val filter = EthFilter(startBlock, endBlock, contractAddresses)
        when (topic0s.size) {
            0 -> error("No event topics configured")
            1 -> filter.addSingleTopic(topic0s.single())
            else -> filter.addOptionalTopics(*topic0s.toTypedArray())
        }
        return filter
    }

    /** 查询数据库中当前监听器已经同步到的最大区块。 */
    private fun checkpointBlock(): BigInteger? {
        return checkpointRepository.findById(LISTENER_NAME)
            .map { it.processedBlock }
            .orElse(null)
    }

    /** 保存当前监听器已经同步到的最新区块。 */
    private fun saveCheckpoint(blockNumber: BigInteger) {
        checkpointRepository.save(
            TopazEventCheckpointEntity(
                listenerName = LISTENER_NAME,
                processedBlock = blockNumber
            )
        )
    }

    /** 执行动作并用统一格式记录失败日志。 */
    private fun logFailure(message: String, action: () -> Unit) {
        runCatching(action).onFailure { ex ->
            log.error("{}: {}", message, ex.message, ex)
        }
    }

    private companion object {
        private const val LISTENER_NAME = "topaz-contract-event-listener"
    }
}
