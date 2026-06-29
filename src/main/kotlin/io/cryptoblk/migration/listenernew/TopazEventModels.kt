package io.cryptoblk.migration.listenernew

import org.web3j.abi.datatypes.Event
import org.web3j.protocol.core.methods.response.Log

data class TopazEventInput(
    val name: String,
    val type: String,
    val indexed: Boolean
)

data class TopazEventField(
    val name: String,
    val type: String,
    val indexed: Boolean,
    val value: Any?
)

data class TopazDecodedEvent(
    val contractName: String,
    val contractAddress: String,
    val eventName: String,
    val topic0: String,
    val fields: List<TopazEventField>,
    val log: Log
) {
    val values: Map<String, Any?> = fields.associate { it.name to it.value }
}

data class TopazEventSubscription(
    val contractName: String,
    val contractAddress: String,
    val eventName: String,
    val topic0: String,
    val event: Event,
    val inputs: List<TopazEventInput>,
    val handle: (TopazDecodedEvent) -> Unit
)
