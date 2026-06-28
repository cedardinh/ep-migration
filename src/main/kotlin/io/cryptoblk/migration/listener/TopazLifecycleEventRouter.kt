package io.cryptoblk.migration.listener

import io.reactivex.disposables.Disposable
import org.slf4j.LoggerFactory
import org.web3j.protocol.Web3j
import org.web3j.protocol.core.DefaultBlockParameterName
import org.web3j.protocol.core.methods.request.EthFilter
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KClass

class TopazLifecycleEventRouter(
    private val web3j: Web3j,
    private val contractAddress: String
) {
    private val logInfo = LoggerFactory.getLogger(TopazLifecycleEventRouter::class.java)
    private var sub: Disposable? = null

    private val handlers: MutableMap<KClass<out TopazLifecycleEvent>, MutableList<(RoutedEvent) -> Unit>> = ConcurrentHashMap()

    fun <T : TopazLifecycleEvent> on(eventType: KClass<T>, handler: (RoutedEvent, T) -> Unit): TopazLifecycleEventRouter {
        val list = handlers.getOrPut(eventType) { mutableListOf() }
        list.add { routed ->
            @Suppress("UNCHECKED_CAST")
            handler(routed, routed.event as T)
        }
        return this
    }

    fun start(fromLatest: Boolean = true) {
        val filter = EthFilter(
            if (fromLatest) DefaultBlockParameterName.LATEST else DefaultBlockParameterName.EARLIEST,
            DefaultBlockParameterName.LATEST,
            contractAddress
        )

        sub = web3j.ethLogFlowable(filter).retryWhen { errs -> errs.delay(5, java.util.concurrent.TimeUnit.SECONDS) }
            .subscribe({ log ->
                val routed = TopazLifecycleEvents.decode(log) ?: return@subscribe
                handlers[routed.event::class]?.forEach { h ->
                    runCatching { h(routed) }
                        .onFailure { e ->
                            // 不要抛出, 避免中断订阅
                            logInfo.error("Handler failed eventId=${routed.eventId} type=${routed.event::class.simpleName}: ${e.message}")
                        }
                }
            }, { err ->
                logInfo.error("Subscription error: ${err.message}")
            })
    }

    fun stop() {
        sub?.dispose()
    }
}
