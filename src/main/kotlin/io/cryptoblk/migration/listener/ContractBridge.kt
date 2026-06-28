package io.cryptoblk.migration.listener

import org.web3j.protocol.Web3j
import org.web3j.tx.Contract
import org.web3j.tx.TransactionManager
import org.web3j.tx.gas.ContractGasProvider
import org.web3j.abi.datatypes.Event
import org.web3j.protocol.core.methods.response.Log

class ContractBridge(
    contractAddress: String,
    web3j: Web3j,
    txManager: TransactionManager,
    gasProvider: ContractGasProvider
) : Contract("", contractAddress, web3j, txManager, gasProvider) {

    fun extract(event: Event, log: Log): EventValuesWithLog? {
        return staticExtractEventParametersWithLog(event, log)
    }
}
