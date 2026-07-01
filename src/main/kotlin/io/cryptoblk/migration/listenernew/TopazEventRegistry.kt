package io.cryptoblk.migration.listenernew

import org.web3j.abi.EventEncoder
import java.util.Locale

/**
 * 构建 Topaz 事件订阅表, 供监听器按合约地址和 topic0 路由链上日志。
 */
object TopazEventRegistry {
    private val contractSpecs: Map<TopazContract, List<EventSpec>> by lazy(::topazContractSpecs)

    /** 根据合约地址和工作流服务构建全部可路由的事件订阅。 */
    fun subscriptions(
        lifecycleAddress: String,
        paymentAddress: String,
        contactsAddress: String,
        workflow: TopazWorkflowService
    ): List<TopazEventSubscription> {
        val addresses = linkedMapOf(
            TopazContract.LIFECYCLE to lifecycleAddress,
            TopazContract.PAYMENT to paymentAddress,
            TopazContract.CONTACTS to contactsAddress
        )
        return addresses.flatMap { (contract, rawAddress) ->
            val address = normalizeAddress(rawAddress)
            contractSpecs.getValue(contract).map { eventSpec ->
                TopazEventSubscription(
                    contractName = contract.id,
                    contractAddress = address,
                    eventName = eventSpec.name,
                    topic0 = EventEncoder.encode(eventSpec.event),
                    event = eventSpec.event,
                    decode = eventSpec.decode,
                    handle = handlerFor(contract, eventSpec.name, workflow)
                )
            }
        }
    }

    /** 规范化合约地址, 保证路由 key 与 Web3j 日志地址格式一致。 */
    internal fun normalizeAddress(address: String): String {
        return address.trim().toLowerCase(Locale.US)
    }
}
