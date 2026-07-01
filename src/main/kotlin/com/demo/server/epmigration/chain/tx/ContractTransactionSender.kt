package com.demo.server.epmigration.chain.tx

import com.demo.server.epmigration.config.EpChainProperties
import com.demo.server.epmigration.observability.ChainCallReporter
import org.springframework.stereotype.Component
import org.web3j.crypto.Credentials
import org.web3j.protocol.Web3j
import org.web3j.protocol.core.DefaultBlockParameter
import org.web3j.protocol.core.RemoteFunctionCall
import org.web3j.protocol.core.methods.request.Transaction
import org.web3j.protocol.core.methods.response.EthGetCode
import org.web3j.protocol.core.methods.response.EthSendTransaction
import org.web3j.protocol.core.methods.response.TransactionReceipt
import org.web3j.tx.TransactionManager
import org.web3j.tx.exceptions.ContractCallException
import org.web3j.tx.response.NoOpProcessor
import java.math.BigInteger

@Component
class ContractTransactionSender(
    private val properties: EpChainProperties,
    private val credentials: Credentials,
    private val nonceManager: ResilientNonceManager,
    private val reporter: ChainCallReporter,
    private val web3j: Web3j
) : TransactionManager(NoOpProcessor(web3j), credentials.address) {
    private val functionName = ThreadLocal<String>()
    private val externalProjectId = ThreadLocal<String?>()
    private val submittedTransaction = ThreadLocal<SubmittedTransaction>()

    fun sendGeneratedTransaction(
        functionName: String,
        externalProjectId: String? = null,
        call: RemoteFunctionCall<TransactionReceipt>
    ): SubmittedContractTransaction {
        validateTransactionConfiguration(properties.gasPrice, properties.gasLimit)
        this.functionName.set(functionName)
        this.externalProjectId.set(externalProjectId)
        submittedTransaction.remove()
        try {
            val receipt = call.send()
            val submitted = submittedTransaction.get()
                ?: throw IllegalStateException("web3j generated wrapper did not submit a transaction")
            return SubmittedContractTransaction(receipt, submitted)
        } finally {
            this.functionName.remove()
            this.externalProjectId.remove()
            submittedTransaction.remove()
        }
    }

    override fun sendTransaction(
        gasPrice: BigInteger,
        gasLimit: BigInteger,
        to: String,
        data: String,
        value: BigInteger,
        constructor: Boolean
    ): EthSendTransaction {
        if (constructor) {
            throw UnsupportedOperationException("Contract deployment is not supported by this transaction manager")
        }
        if (value.signum() != 0) {
            throw UnsupportedOperationException("Only zero-value contract transactions are supported")
        }
        validateTransactionConfiguration(gasPrice, gasLimit)

        val context = ChainCallContext(
            op = functionName.get() ?: selector(data),
            externalProjectId = externalProjectId.get(),
            from = withHexPrefix(credentials.address),
            to = to
        )
        val submitted = nonceManager.sendRawTransaction(
            to = to,
            data = data,
            gasPrice = gasPrice,
            gasLimit = gasLimit,
            chainId = properties.chainId,
            context = context
        )
        reporter.submitted(submitted)
        submittedTransaction.set(submitted)

        return EthSendTransaction().apply {
            result = submitted.transactionHash
        }
    }

    override fun sendEIP1559Transaction(
        chainId: Long,
        maxPriorityFeePerGas: BigInteger,
        maxFeePerGas: BigInteger,
        gasLimit: BigInteger,
        to: String,
        data: String,
        value: BigInteger,
        constructor: Boolean
    ): EthSendTransaction {
        throw UnsupportedOperationException("EIP-1559 transactions are not supported by ep.chain static gas configuration")
    }

    override fun sendCall(
        to: String,
        data: String,
        defaultBlockParameter: DefaultBlockParameter
    ): String {
        val ethCall = web3j.ethCall(
            Transaction.createEthCallTransaction(getFromAddress(), to, data),
            defaultBlockParameter
        ).send()

        if (ethCall.isReverted) {
            throw ContractCallException(String.format(REVERT_ERR_STR, ethCall.revertReason))
        }
        return ethCall.value
    }

    override fun getCode(
        contractAddress: String,
        defaultBlockParameter: DefaultBlockParameter
    ): EthGetCode {
        return web3j.ethGetCode(contractAddress, defaultBlockParameter).send()
    }

    private fun withHexPrefix(value: String): String {
        return if (value.startsWith("0x")) value else "0x$value"
    }

    private fun validateTransactionConfiguration(gasPrice: BigInteger, gasLimit: BigInteger) {
        if (properties.chainId <= 0L) {
            throw IllegalStateException("ep.chain.chain-id must be positive")
        }
        if (gasPrice.signum() < 0) {
            throw IllegalStateException("ep.chain.gas-price must be greater than or equal to 0")
        }
        if (gasLimit.signum() <= 0) {
            throw IllegalStateException("ep.chain.gas-limit must be positive")
        }
    }

    private fun selector(data: String): String {
        return if (data.length >= SELECTOR_LENGTH) data.substring(0, SELECTOR_LENGTH) else "transaction"
    }

    companion object {
        private const val SELECTOR_LENGTH = 10
    }
}

data class SubmittedContractTransaction(
    val receipt: TransactionReceipt,
    val submitted: SubmittedTransaction
)
