package io.cryptoblk.migration.listenernew

import com.demo.server.epmigration.config.EpChainProperties
import io.reactivex.disposables.Disposable
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.SmartLifecycle
import org.springframework.stereotype.Component
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.TransactionTemplate
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
@ConditionalOnProperty(prefix = "ep.chain", name = ["listener-enabled"], havingValue = "true", matchIfMissing = true)
class TopazContractEventListener(
    private val web3j: Web3j,
    @Qualifier("lifecycleContractAddress") lifecycleContractAddress: String,
    @Qualifier("paymentContractAddress") paymentContractAddress: String,
    @Qualifier("contactsContractAddress") contactsContractAddress: String,
    workflow: TopazWorkflowService,
    private val checkpointRepository: TopazEventCheckpointRepository,
    private val chainProperties: EpChainProperties,
    transactionManager: PlatformTransactionManager
) : SmartLifecycle {
    private val log = LoggerFactory.getLogger(TopazContractEventListener::class.java)
    private val transactionTemplate = TransactionTemplate(transactionManager)
    private val checkpointIdentity = TopazEventCheckpointIdentity(
        listenerName = LISTENER_NAME,
        chainId = chainProperties.chainId,
        lifecycleContractAddress = TopazEventRegistry.normalizeAddress(lifecycleContractAddress),
        paymentContractAddress = TopazEventRegistry.normalizeAddress(paymentContractAddress),
        contactsContractAddress = TopazEventRegistry.normalizeAddress(contactsContractAddress)
    )

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
        require(chainProperties.listenerFromBlock == null || chainProperties.listenerFromBlock!!.signum() >= 0) {
            "ep.chain.listener-from-block must be non-negative"
        }
    }

    /** 启动实时日志订阅并执行一次启动补偿扫描。 */
    override fun start() {
        if (running) return

        subscriptionStream = web3j.ethLogFlowable(
            eventFilter(DefaultBlockParameterName.LATEST, DefaultBlockParameterName.LATEST)
        )
            .retryWhen { errors -> errors.delay(5, TimeUnit.SECONDS) }
            .subscribe({ chainLog ->
                route(chainLog)
            }) { error ->
                log.error("Topaz contract event listener subscription failed: {}", error.message, error)
            }
        running = true

        try {
            compensateMissingEvents()
        } catch (ex: Exception) {
            subscriptionStream?.dispose()
            subscriptionStream = null
            running = false
            log.error("Topaz event compensation failed on startup: {}", ex.message, ex)
            throw ex
        }

        log.info(
            "Topaz contract event listener started contracts={} subscriptions={} checkpoint={}",
            contractAddresses.size,
            subscriptions.size,
            checkpoint()
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

    /** 从数据库记录的事件游标之后补扫; 没有记录时以当前链头作为初始处理进度。 */
    fun compensateMissingEvents(): Int {
        val latestBlock = web3j.ethBlockNumber().send().let { response ->
            if (response.hasError()) {
                error("Failed to fetch latest block: ${response.error?.message ?: "unknown JSON-RPC error"}")
            }
            response.blockNumber
        }

        val checkpoint = checkpoint()
        return if (checkpoint == null) {
            compensateFromInitialBlock(latestBlock)
        } else {
            scanEvents(checkpoint.resumeBlock(), latestBlock, checkpoint)
        }
    }

    /** 没有 checkpoint 时根据配置决定是否从指定起点回扫。 */
    private fun compensateFromInitialBlock(latestBlock: BigInteger): Int {
        val fromBlock = chainProperties.listenerFromBlock
        if (fromBlock == null) {
            saveFullBlockCheckpointInTransaction(latestBlock)
            log.info("Topaz event checkpoint initialized at latest block {}", latestBlock)
            return 0
        }
        require(fromBlock.compareTo(latestBlock) <= 0) {
            "ep.chain.listener-from-block must be less than or equal to latest block $latestBlock"
        }

        log.info("Topaz event checkpoint not found; backfilling from configured block {} to {}", fromBlock, latestBlock)
        return scanEvents(fromBlock, latestBlock, null)
    }

    /** 扫描指定闭区间区块并复用实时订阅的路由处理链路。 */
    fun scanEvents(startBlock: BigInteger, endBlock: BigInteger): Int {
        return scanEvents(startBlock, endBlock, null)
    }

    /** 扫描指定闭区间区块, 并在恢复扫描时跳过已经处理过的同区块日志。 */
    private fun scanEvents(
        startBlock: BigInteger,
        endBlock: BigInteger,
        resumeAfter: TopazEventCheckpointEntity?
    ): Int {
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
        }.sortedWith(chainLogOrder)
            .filter { shouldProcessAfterCheckpoint(it, resumeAfter) }

        var handledLogCount = 0
        var failed = false
        for (chainLog in logs) {
            handledLogCount += 1
            if (route(chainLog) == RouteResult.FAILED) {
                failed = true
                break
            }
        }

        if (!failed) {
            saveFullBlockCheckpointInTransaction(endBlock)
        } else {
            log.warn(
                "Topaz event compensation left checkpoint at last successful event because one or more handlers failed"
            )
        }

        log.info(
            "Topaz event compensation scanned blocks {}..{} logs={} handled={} completedThroughBlock={}",
            startBlock,
            endBlock,
            logs.size,
            handledLogCount,
            if (failed) null else endBlock
        )

        return handledLogCount
    }

    /** 将单条链上日志完成匹配、解码、派发, 并推进事件处理进度。 */
    private fun route(chainLog: Log): RouteResult {
        if (chainLog.isRemoved) return RouteResult.IGNORED

        val address = chainLog.address?.let(TopazEventRegistry::normalizeAddress) ?: return RouteResult.IGNORED
        val topic0 = chainLog.topics?.firstOrNull() ?: return RouteResult.IGNORED
        val subscription = routes[address to topic0] ?: return RouteResult.IGNORED

        return runCatching {
            executeInTransaction {
                val response = subscription.decode(chainLog)
                subscription.handle(response)
                saveEventCheckpoint(chainLog)
                RouteResult.PROCESSED
            }
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
        }.getOrDefault(RouteResult.FAILED)
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

    /** 查询数据库中当前监听器已经同步到的最新事件游标。 */
    private fun checkpoint(): TopazEventCheckpointEntity? {
        return checkpointRepository.findByIdentity(checkpointIdentity)
            .orElse(null)
    }

    /** 在事务中保存当前监听器已经完整同步到指定区块。 */
    private fun saveFullBlockCheckpointInTransaction(blockNumber: BigInteger) {
        executeInTransaction {
            saveFullBlockCheckpoint(blockNumber)
        }
    }

    /** 保存当前监听器已经完整同步到指定区块。 */
    private fun saveFullBlockCheckpoint(blockNumber: BigInteger) {
        checkpointRepository.save(
            TopazEventCheckpointEntity(
                listenerName = checkpointIdentity.listenerName,
                processedBlock = blockNumber,
                chainId = checkpointIdentity.chainId,
                lifecycleContractAddress = checkpointIdentity.lifecycleContractAddress,
                paymentContractAddress = checkpointIdentity.paymentContractAddress,
                contactsContractAddress = checkpointIdentity.contactsContractAddress
            )
        )
    }

    /** 保存当前监听器已经处理到的最新事件。 */
    private fun saveEventCheckpoint(chainLog: Log) {
        val blockNumber = chainLog.blockNumber ?: error("Cannot checkpoint Topaz event without block number")
        val transactionHash =
            chainLog.transactionHash ?: error("Cannot checkpoint Topaz event without transaction hash")
        val logIndex = chainLog.logIndex ?: error("Cannot checkpoint Topaz event without log index")
        checkpointRepository.save(
            TopazEventCheckpointEntity(
                listenerName = checkpointIdentity.listenerName,
                processedBlock = blockNumber,
                processedTransactionHash = transactionHash,
                processedLogIndex = logIndex,
                chainId = checkpointIdentity.chainId,
                lifecycleContractAddress = checkpointIdentity.lifecycleContractAddress,
                paymentContractAddress = checkpointIdentity.paymentContractAddress,
                contactsContractAddress = checkpointIdentity.contactsContractAddress
            )
        )
    }

    /** 执行动作并加入监听器事务, 保证 handler 与 checkpoint 原子提交。 */
    private fun <T> executeInTransaction(action: () -> T): T {
        return transactionTemplate.execute { action() }
            ?: error("Topaz listener transaction completed without result")
    }

    /** 判断补扫时是否应处理这条日志。 */
    private fun shouldProcessAfterCheckpoint(
        chainLog: Log,
        checkpoint: TopazEventCheckpointEntity?
    ): Boolean {
        if (checkpoint == null) return true

        val logBlock = chainLog.blockNumber ?: return true
        val checkpointBlock = checkpoint.processedBlock

        return when {
            logBlock < checkpointBlock -> false
            logBlock > checkpointBlock -> true
            checkpoint.isCompletedBlockCheckpoint() -> false
            else -> isSameBlockLogAfterEventCheckpoint(chainLog, checkpoint)
        }
    }

    /**
     * 没有 processedLogIndex 时, checkpoint 表示 processedBlock 已完整扫描。
     * 有 processedLogIndex 时, checkpoint 表示监听器停在 processedBlock 内某条日志之后。
     */
    private fun TopazEventCheckpointEntity.isCompletedBlockCheckpoint(): Boolean {
        return processedLogIndex == null
    }

    /** 同一区块恢复时, 只处理已保存事件游标之后的日志。 */
    private fun isSameBlockLogAfterEventCheckpoint(
        chainLog: Log,
        checkpoint: TopazEventCheckpointEntity
    ): Boolean {
        val checkpointLogIndex = checkpoint.processedLogIndex ?: return false
        val logIndex = chainLog.logIndex ?: return true
        return logIndex > checkpointLogIndex
    }

    /** 有事件级游标时从同一区块恢复, 只有已完整处理区块记录时从下一区块恢复。 */
    private fun TopazEventCheckpointEntity.resumeBlock(): BigInteger {
        return if (isCompletedBlockCheckpoint()) {
            processedBlock.add(BigInteger.ONE)
        } else {
            processedBlock
        }
    }

    private companion object {
        private const val LISTENER_NAME = "topaz-contract-event-listener"

        private val chainLogOrder = compareBy<Log>(
            { it.blockNumber ?: BigInteger.ZERO },
            { it.transactionIndex ?: BigInteger.ZERO },
            { it.logIndex ?: BigInteger.ZERO }
        )
    }

    private enum class RouteResult {
        IGNORED,
        PROCESSED,
        FAILED
    }
}
