package com.demo.server.epmigration.chain.gateway

import com.demo.server.epmigration.chain.contract.TopazLifecycleContract
import com.demo.server.epmigration.chain.tx.ChainCallContext
import com.demo.server.epmigration.chain.tx.ContractTransactionSubmitter
import com.demo.server.epmigration.config.EpChainProperties
import com.demo.server.epmigration.observability.ChainCallReporter
import com.demo.server.epmigration.project.dto.CreateProjectRequest
import org.springframework.stereotype.Component
import org.web3j.crypto.Credentials

data class CreateProjectChainResult(
    val transactionHash: String,
    val externalProjectId: String,
    val from: String,
    val to: String,
    val nonce: String,
    val correlationId: String?
)

interface TopazLifecycleGateway {
    fun createProject(input: CreateProjectRequest, correlationId: String): CreateProjectChainResult
}

@Component
class Web3jTopazLifecycleGateway(
    private val properties: EpChainProperties,
    private val credentials: Credentials,
    private val lifecycle: TopazLifecycleContract,
    private val submitter: ContractTransactionSubmitter,
    private val reporter: ChainCallReporter
) : TopazLifecycleGateway {
    override fun createProject(input: CreateProjectRequest, correlationId: String): CreateProjectChainResult {
        val from = withHexPrefix(credentials.address)
        val call = lifecycle.createProject(input)
        val context = ChainCallContext(
            correlationId = correlationId,
            op = call.functionName,
            externalProjectId = input.externalProjectId,
            chainId = properties.chainId,
            from = from,
            to = call.to,
            phase = "encode",
            rpcCode = null,
            rpcMessage = null,
            transactionHash = null,
            nonce = null,
            httpStatus = 200
        )
        val submitted = submitter.submit(call, context)
        val result = CreateProjectChainResult(
            transactionHash = submitted.transactionHash,
            externalProjectId = input.externalProjectId,
            from = from,
            to = call.to,
            nonce = submitted.nonce,
            correlationId = correlationId
        )
        reporter.submitted(result)
        return result
    }

    private fun withHexPrefix(value: String): String {
        return if (value.startsWith("0x")) value else "0x$value"
    }
}
