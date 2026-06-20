package com.demo.server.epmigration.chain.contract

import org.web3j.abi.FunctionEncoder
import org.web3j.abi.TypeReference
import org.web3j.abi.datatypes.Function
import com.demo.server.epmigration.chain.generated.TopazLifecycle
import com.demo.server.epmigration.chain.tx.ContractWriteCall
import com.demo.server.epmigration.config.EpChainProperties
import org.springframework.stereotype.Component

@Component
class TopazLifecycleContract(
    private val properties: EpChainProperties
) {
    val address: String
        get() = properties.lifecycleContractAddress

    fun createProject(input: TopazLifecycle.CreateProjectInput): ContractWriteCall {
        val function = Function(
            TopazLifecycle.FUNC_CREATEPROJECT,
            listOf(input),
            emptyList<TypeReference<*>>()
        )
        return ContractWriteCall(
            functionName = TopazLifecycle.FUNC_CREATEPROJECT,
            to = address,
            data = FunctionEncoder.encode(function)
        )
    }
}
