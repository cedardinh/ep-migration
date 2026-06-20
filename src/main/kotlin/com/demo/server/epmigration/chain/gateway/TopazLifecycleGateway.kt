package com.demo.server.epmigration.chain.gateway

import com.demo.server.epmigration.chain.contract.TopazLifecycleContract
import com.demo.server.epmigration.chain.tx.ChainCallContext
import com.demo.server.epmigration.chain.tx.ResilientNonceManager
import com.demo.server.epmigration.config.EpChainProperties
import com.demo.server.epmigration.observability.ChainCallReporter
import com.demo.server.epmigration.project.dto.CreateProjectRequest
import com.demo.server.epmigration.project.dto.CreateProjectResponse
import org.springframework.stereotype.Component
import org.web3j.crypto.Credentials

@Component
class TopazLifecycleGateway(
    private val properties: EpChainProperties,
    private val credentials: Credentials,
    private val lifecycle: TopazLifecycleContract,
    private val nonceManager: ResilientNonceManager,
    private val reporter: ChainCallReporter
) {
    fun createProject(input: CreateProjectRequest): CreateProjectResponse {
        val from = withHexPrefix(credentials.address)
        val call = lifecycle.createProject(input)
        val context = ChainCallContext(
            op = call.functionName,
            externalProjectId = input.externalProjectId,
            from = from,
            to = call.to
        )
        val submitted = nonceManager.sendRawTransaction(
            to = call.to,
            data = call.data,
            gasLimit = properties.gasLimit,
            chainId = properties.chainId,
            context = context
        )
        val result = CreateProjectResponse(
            transactionHash = submitted.transactionHash,
            externalProjectId = input.externalProjectId,
            from = from,
            to = call.to,
            nonce = submitted.nonce
        )
        reporter.submitted(result)
        return result
    }

    private fun withHexPrefix(value: String): String {
        return if (value.startsWith("0x")) value else "0x$value"
    }
}
