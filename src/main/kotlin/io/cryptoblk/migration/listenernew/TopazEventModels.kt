package io.cryptoblk.migration.listenernew

import org.web3j.abi.datatypes.Event
import org.web3j.protocol.core.methods.response.BaseEventResponse
import org.web3j.protocol.core.methods.response.Log

internal enum class TopazContract(
    val id: String,
    val handlerPrefix: String
) {
    LIFECYCLE("lifecycle", "Lifecycle"),
    PAYMENT("payment", "Payment"),
    CONTACTS("contacts", "Contacts");
}

/** 描述一个可监听事件的合约地址, topic0, 解码器和处理函数。 */
data class TopazEventSubscription(
    val contractName: String,
    val contractAddress: String,
    val eventName: String,
    val topic0: String,
    val event: Event,
    val decode: (Log) -> BaseEventResponse,
    val handle: (BaseEventResponse) -> Unit
)

internal data class EventSpec(
    val name: String,
    val event: Event,
    val decode: (Log) -> BaseEventResponse
)
