package io.cryptoblk.migration.listener

import org.springframework.context.SmartLifecycle
import org.springframework.stereotype.Component

@Component
class TopazLifecycleChainListener(
    private val router: TopazLifecycleEventRouter
) : SmartLifecycle {

    @Volatile private var running = false

    override fun start() {
        if (running) return
        router.start(fromLatest = true) // 启动即实时监听
        running = true
        println("TopazLifecycleChainListener started")
    }

    override fun stop() {
        router.stop()
        running = false
        println("TopazLifecycleChainListener stopped")
    }

    override fun isRunning(): Boolean = running
    override fun getPhase(): Int = Int.MAX_VALUE
}
