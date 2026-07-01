package io.cryptoblk.migration.listenernew

import org.web3j.abi.datatypes.Event
import org.web3j.protocol.core.methods.response.BaseEventResponse
import org.web3j.protocol.core.methods.response.Log

internal enum class TopazContract(
    /** Stable contract id. */
    val id: String,

    /** Workflow handler prefix. */
    val handlerPrefix: String
) {
    LIFECYCLE("lifecycle", "Lifecycle"),
    PAYMENT("payment", "Payment"),
    CONTACTS("contacts", "Contacts");
}

/** 描述一个可监听事件的合约地址, topic0, 解码器和处理函数。 */
data class TopazEventSubscription(
    /** Source contract name. */
    val contractName: String,

    /** Source contract address. */
    val contractAddress: String,

    /** Emitted event name. */
    val eventName: String,

    /** Event signature topic. */
    val topic0: String,

    /** Wrapper event metadata. */
    val event: Event,

    /** Log decoder. */
    val decode: (Log) -> BaseEventResponse,

    /** Event handler. */
    val handle: (BaseEventResponse) -> Unit
)

internal data class EventSpec(
    /** Event name. */
    val name: String,

    /** Wrapper event metadata. */
    val event: Event,

    /** Log decoder. */
    val decode: (Log) -> BaseEventResponse
)
