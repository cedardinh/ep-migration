package com.demo.server.epmigration.chain.tx

import com.demo.server.epmigration.config.EpChainProperties
import com.demo.server.epmigration.observability.ChainCallReporter
import org.springframework.stereotype.Component
import org.web3j.abi.FunctionEncoder
import org.web3j.abi.TypeReference
import org.web3j.abi.datatypes.Function
import org.web3j.abi.datatypes.Type
import org.web3j.crypto.Credentials

@Component
class ContractTransactionSender(
    private val properties: EpChainProperties,
    private val credentials: Credentials,
    private val nonceManager: ResilientNonceManager,
    private val reporter: ChainCallReporter
) {
    fun sendWriteFunction(
        contractAddress: String,
        functionName: String,
        inputParameters: List<Type<*>>,
        externalProjectId: String? = null
    ): SubmittedTransaction {
        val call = encodeWriteFunction(contractAddress, functionName, inputParameters)
        val context = ChainCallContext(
            op = functionName,
            externalProjectId = externalProjectId,
            from = withHexPrefix(credentials.address),
            to = contractAddress
        )
        val submitted = nonceManager.sendRawTransaction(
            to = call.to,
            data = call.data,
            gasLimit = properties.gasLimit,
            chainId = properties.chainId,
            context = context
        )
        reporter.submitted(submitted)
        return submitted
    }

    private fun withHexPrefix(value: String): String {
        return if (value.startsWith("0x")) value else "0x$value"
    }

    companion object {
        fun encodeWriteFunction(
            contractAddress: String,
            functionName: String,
            inputParameters: List<Type<*>>
        ): ContractWriteCall {
            val function = Function(
                functionName,
                inputParameters,
                emptyList<TypeReference<*>>()
            )
            return ContractWriteCall(
                functionName = functionName,
                to = contractAddress,
                data = FunctionEncoder.encode(function)
            )
        }
    }
}
