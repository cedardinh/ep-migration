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
        validateTransactionConfiguration()
        val function = Function(
            functionName,
            inputParameters,
            emptyList<TypeReference<*>>()
        )
        val data = FunctionEncoder.encode(function)
        val context = ChainCallContext(
            op = functionName,
            externalProjectId = externalProjectId,
            from = withHexPrefix(credentials.address),
            to = contractAddress
        )
        val submitted = nonceManager.sendRawTransaction(
            to = contractAddress,
            data = data,
            gasPrice = properties.gasPrice,
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

    private fun validateTransactionConfiguration() {
        if (properties.chainId <= 0L) {
            throw IllegalStateException("ep.chain.chain-id must be positive")
        }
        if (properties.gasPrice.signum() < 0) {
            throw IllegalStateException("ep.chain.gas-price must be greater than or equal to 0")
        }
        if (properties.gasLimit.signum() <= 0) {
            throw IllegalStateException("ep.chain.gas-limit must be positive")
        }
    }
}
