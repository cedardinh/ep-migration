package io.cryptoblk.migration.listenernew

import com.demo.server.epmigration.config.EpChainProperties
import io.reactivex.disposables.Disposable
import org.slf4j.LoggerFactory
import org.springframework.context.SmartLifecycle
import org.springframework.stereotype.Component
import org.web3j.protocol.Web3j
import org.web3j.protocol.core.DefaultBlockParameterName
import org.web3j.protocol.core.methods.request.EthFilter
import org.web3j.protocol.core.methods.response.Log
import java.util.concurrent.TimeUnit

/**
 * Multi-contract event listening engine: a single on-chain log stream routed to
 * registered subscriptions by (contract address + topic0).
 */
@Component
class TopazContractEventListener(
    private val web3j: Web3j,
    properties: EpChainProperties,
    workflow: TopazWorkflowService
) : SmartLifecycle {
    private val log = LoggerFactory.getLogger(TopazContractEventListener::class.java)

    // Routing table and filter criteria, computed once at startup
    private val subscriptions = TopazEventRegistry.subscriptions(properties, workflow)
    private val byRoute = subscriptions.associateBy { EventRouteKey(it.contractAddress, it.topic0) }
    private val contractAddresses = subscriptions.map { it.contractAddress }.distinct()
    private val topic0s = subscriptions.map { it.topic0 }.distinct()

    @Volatile
    private var running = false
    private var stream: Disposable? = null

    init {
        require(byRoute.size == subscriptions.size) {
            "Duplicate event subscription route found. contract address + topic0 must be unique"
        }
    }

    // ---- SmartLifecycle ----

    override fun start() {
        if (running) return

        val filter = EthFilter(
            DefaultBlockParameterName.LATEST,
            DefaultBlockParameterName.LATEST,
            contractAddresses
        )
        when (topic0s.size) {
            0 -> error("No event topics configured")
            1 -> filter.addSingleTopic(topic0s.single())
            else -> filter.addOptionalTopics(*topic0s.toTypedArray())
        }

        stream = web3j.ethLogFlowable(filter)
            .retryWhen { errors -> errors.delay(5, TimeUnit.SECONDS) }
            .subscribe(::route, ::subscriptionFailed)
        running = true

        log.info(
            "Topaz contract event listener started contracts={} subscriptions={}",
            contractAddresses.size,
            subscriptions.size
        )
    }

    override fun stop() {
        stream?.dispose()
        stream = null
        running = false
        log.info("Topaz contract event listener stopped")
    }

    override fun isRunning(): Boolean = running

    override fun getPhase(): Int = Int.MAX_VALUE

    // ---- Log routing ----

    private fun route(chainLog: Log) {
        if (chainLog.isRemoved) return

        val address = chainLog.address?.let(TopazEventRegistry::normalizeAddress) ?: return
        val topic0 = chainLog.topics?.firstOrNull() ?: return
        val subscription = byRoute[EventRouteKey(address, topic0)] ?: return

        runCatching {
            val event = TopazEventRegistry.decode(subscription, chainLog)
            subscription.handle(event)
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
    }

    private fun subscriptionFailed(error: Throwable) {
        log.error("Topaz contract event listener subscription failed: {}", error.message, error)
    }

    private data class EventRouteKey(
        val contractAddress: String,
        val topic0: String
    )
}
