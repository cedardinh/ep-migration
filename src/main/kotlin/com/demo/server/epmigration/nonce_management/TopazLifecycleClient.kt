package com.demo.server.epmigration.nonce_management

import com.demo.server.epmigration.chain.generated.TopazLifecycle
import com.demo.server.epmigration.config.EpChainProperties
import org.springframework.stereotype.Component
import org.web3j.protocol.Web3j
import org.web3j.tx.TransactionManager
import org.web3j.tx.gas.StaticGasProvider

@Component
class TopazLifecycleClient(
    properties: EpChainProperties,
    web3j: Web3j,
    transactionManager: TransactionManager
) {
    private val lifecycle = TopazLifecycle.load(
        requiredAddress(
            "ep.chain.lifecycle-contract-address",
            properties.lifecycleContractAddress
        ),
        web3j,
        transactionManager,
        StaticGasProvider(properties.gasPrice, properties.gasLimit)
    )

    fun createProject(
        request: CreateProjectRequest
    ): CreateProjectResponse {
        val receipt = lifecycle.createProject(request.toContractInput()).send()
        check(receipt.isStatusOK) {
            "createProject transaction failed: " +
                "hash=${receipt.transactionHash}, status=${receipt.status}"
        }

        val event =
            TopazLifecycle.getProjectCreatedEvents(receipt).singleOrNull()
                ?: error(
                    "ProjectCreated event not found in transaction " +
                        receipt.transactionHash
                )

        return CreateProjectResponse(
            projectId = event.projectId,
            transactionHash = receipt.transactionHash,
            blockNumber = receipt.blockNumber,
            gasUsed = receipt.gasUsed
        )
    }

    private fun requiredAddress(
        propertyName: String,
        value: String
    ): String {
        val address = value.trim()
        require(ADDRESS_PATTERN.matches(address)) {
            "$propertyName must be a 20-byte hex address"
        }
        return address
    }

    companion object {
        private val ADDRESS_PATTERN = Regex("^0x[0-9a-fA-F]{40}$")
    }
}
