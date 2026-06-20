package com.demo.server.epmigration.chain.contract

import com.demo.server.epmigration.chain.generated.TopazLifecycle
import com.demo.server.epmigration.chain.tx.ContractWriteCall
import com.demo.server.epmigration.config.EpChainProperties
import org.springframework.stereotype.Component
import org.web3j.crypto.Credentials
import org.web3j.protocol.Web3j
import org.web3j.tx.gas.StaticGasProvider
import java.math.BigInteger

@Component
class TopazLifecycleContract(
    private val properties: EpChainProperties,
    web3j: Web3j,
    credentials: Credentials
) {
    private val delegate = TopazLifecycle.load(
        address,
        web3j,
        credentials,
        StaticGasProvider(BigInteger.ZERO, properties.gasLimit)
    )

    val address: String
        get() = properties.lifecycleContractAddress

    fun createProject(input: TopazLifecycle.CreateProjectInput): ContractWriteCall {
        return ContractWriteCall(
            contractName = CONTRACT_NAME,
            functionName = TopazLifecycle.FUNC_CREATEPROJECT,
            to = address,
            data = delegate.createProject(input).encodeFunctionCall()
        )
    }

    companion object {
        const val CONTRACT_NAME = "TopazLifecycle"
    }
}
