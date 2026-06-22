package com.demo.server.epmigration.ledger.generated

import com.demo.server.epmigration.chain.generated.TopazLifecycle as GeneratedTopazLifecycle
import org.web3j.abi.TypeReference
import org.web3j.abi.datatypes.Function
import org.web3j.abi.datatypes.Type
import org.web3j.protocol.Web3j
import org.web3j.protocol.core.RemoteCall
import org.web3j.protocol.core.methods.response.TransactionReceipt
import org.web3j.tx.Contract
import org.web3j.tx.TransactionManager
import org.web3j.tx.gas.ContractGasProvider

class TopazLifecycle private constructor(
    contractAddress: String,
    web3j: Web3j,
    transactionManager: TransactionManager,
    gasProvider: ContractGasProvider
) : Contract(BINARY, contractAddress, web3j, transactionManager, gasProvider) {

    fun createProject(input: GeneratedTopazLifecycle.CreateProjectInput): RemoteCall<TransactionReceipt> =
        tx(FUNC_CREATEPROJECT, input)

    private fun tx(name: String, input: Type<*>): RemoteCall<TransactionReceipt> =
        executeRemoteCallTransaction(Function(name, listOf(input), emptyList<TypeReference<*>>()))

    companion object {
        const val BINARY = "Bin file was not provided"
        const val FUNC_CREATEPROJECT = "createProject"

        fun load(
            contractAddress: String,
            web3j: Web3j,
            transactionManager: TransactionManager,
            gasProvider: ContractGasProvider
        ): TopazLifecycle {
            return TopazLifecycle(contractAddress, web3j, transactionManager, gasProvider)
        }

        fun getProjectCreatedEvents(
            transactionReceipt: TransactionReceipt
        ): List<GeneratedTopazLifecycle.ProjectCreatedEventResponse> {
            return GeneratedTopazLifecycle.getProjectCreatedEvents(transactionReceipt)
        }
    }
}
