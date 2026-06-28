package io.cryptoblk.migration.listener

import org.springframework.stereotype.Service
import java.util.concurrent.ConcurrentHashMap

@Service
class EventDedupService {
    private val seen = ConcurrentHashMap.newKeySet<String>()

    fun firstTime(eventId: String): Boolean = seen.add(eventId)
}
