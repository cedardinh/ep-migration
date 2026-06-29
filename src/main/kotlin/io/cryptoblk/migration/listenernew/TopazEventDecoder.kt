package io.cryptoblk.migration.listenernew

import org.web3j.abi.FunctionReturnDecoder
import org.web3j.abi.datatypes.Type
import org.web3j.protocol.core.methods.response.Log
import org.web3j.utils.Numeric

object TopazEventDecoder {

    fun decode(subscription: TopazEventSubscription, chainLog: Log): TopazDecodedEvent {
        val topics = chainLog.topics ?: emptyList()
        val indexedInputs = subscription.inputs.filter { it.indexed }
        require(topics.size >= indexedInputs.size + 1) {
            "Log for ${subscription.contractName}.${subscription.eventName} has ${topics.size} topics, expected ${indexedInputs.size + 1}"
        }

        val indexedValues = subscription.event.indexedParameters.mapIndexed { index, typeReference ->
            FunctionReturnDecoder.decodeIndexedValue(topics[index + 1], typeReference)
        }
        val nonIndexedValues = FunctionReturnDecoder.decode(
            chainLog.data ?: "0x",
            subscription.event.nonIndexedParameters
        )

        var indexedIndex = 0
        var nonIndexedIndex = 0
        val fields = subscription.inputs.map { input ->
            val decodedValue = if (input.indexed) {
                indexedValues[indexedIndex++]
            } else {
                nonIndexedValues[nonIndexedIndex++]
            }
            TopazEventField(
                name = input.name,
                type = input.type,
                indexed = input.indexed,
                value = toPlainValue(decodedValue)
            )
        }

        return TopazDecodedEvent(
            contractName = subscription.contractName,
            contractAddress = subscription.contractAddress,
            eventName = subscription.eventName,
            topic0 = subscription.topic0,
            fields = fields,
            log = chainLog
        )
    }

    private fun toPlainValue(value: Type<*>): Any? {
        return when (val raw = value.value) {
            is ByteArray -> Numeric.toHexString(raw)
            is Array<*> -> raw.map { it }
            else -> raw
        }
    }
}
