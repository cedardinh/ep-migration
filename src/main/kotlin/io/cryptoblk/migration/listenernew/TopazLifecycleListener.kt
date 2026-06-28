package io.cryptoblk.migration.listenernew

import com.demo.server.epmigration.config.EpChainProperties
import io.reactivex.disposables.Disposable
import org.slf4j.LoggerFactory
import org.springframework.context.SmartLifecycle
import org.springframework.stereotype.Component
import org.web3j.abi.EventEncoder
import org.web3j.protocol.Web3j
import org.web3j.protocol.core.DefaultBlockParameterName
import org.web3j.protocol.core.methods.request.EthFilter
import org.web3j.protocol.core.methods.response.Log
import java.util.concurrent.TimeUnit

/**
 * 监听引擎: 订阅 TopazLifecycle 合约日志, 按 topic0 路由到对应订阅, 并管理生命周期.
 * 具体监听哪些事件、交给谁处理, 都在 [TopazEventRegistry] 里登记.
 */
@Component
class TopazLifecycleListener(
    private val web3j: Web3j,
    properties: EpChainProperties,
    workflow: TopazWorkflowService
) : SmartLifecycle {
    private val log = LoggerFactory.getLogger(TopazLifecycleListener::class.java)
    private val contractAddress = properties.lifecycleContractAddress.trim()
    private val subscriptions = TopazEventRegistry.subscriptions(workflow)
    private val byTopic0 = subscriptions.associateBy { EventEncoder.encode(it.event) }

    @Volatile
    private var running = false
    private var stream: Disposable? = null

    init {
        require(ADDRESS_PATTERN.matches(contractAddress)) {
            "ep.chain.lifecycle-contract-address must be a 20-byte hex address"
        }
        require(subscriptions.map { it.name }.toSet() == TopazEventRegistry.ABI_EVENTS) {
            "TopazLifecycle listener subscriptions do not match ABI events"
        }
    }

    override fun start() {
        if (running) return
        val filter = EthFilter(DefaultBlockParameterName.LATEST, DefaultBlockParameterName.LATEST, contractAddress)
        stream = web3j.ethLogFlowable(filter)
            .retryWhen { errors -> errors.delay(5, TimeUnit.SECONDS) }
            .subscribe(::route, ::subscriptionFailed)
        running = true
        println("TopazLifecycleListener started")
    }

    override fun stop() {
        stream?.dispose()
        running = false
        println("TopazLifecycleListener stopped")
    }

    override fun isRunning(): Boolean = running

    override fun getPhase(): Int = Int.MAX_VALUE

    private fun route(chainLog: Log) {
        val subscription = byTopic0[chainLog.topics.firstOrNull()] ?: return
        runCatching {
            subscription.handle(chainLog)
        }.onFailure { ex ->
            log.error(
                "TopazLifecycle listener handler failed event={} tx={} logIndex={}: {}",
                subscription.name,
                chainLog.transactionHash,
                chainLog.logIndex,
                ex.message,
                ex
            )
        }
    }

    private fun subscriptionFailed(error: Throwable) {
        log.error("TopazLifecycle listener subscription failed: {}", error.message, error)
    }

    private companion object {
        private val ADDRESS_PATTERN = Regex("^0x[0-9a-fA-F]{40}$")
    }
}
