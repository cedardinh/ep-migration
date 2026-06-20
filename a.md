```kotlin
private fun hasProjectOfficerRole(): Boolean {
    val function = Function(
        "hasRole",
        listOf(
            Bytes32(PROJECT_OFFICER_ROLE),
            Address(credentials.address)
        ),
        listOf(object : TypeReference<Bool>() {})
    )

    val response = web3j.ethCall(
        Transaction.createEthCallTransaction(
            credentials.address,
            lifecycle.contractAddress,
            FunctionEncoder.encode(function)
        ),
        DefaultBlockParameterName.LATEST
    ).send()

    if (response.hasError()) {
        log.error(
            "Web3j role precheck eth_call failed: sender={} roleHash={} rpcCode={} rpcMessage={}",
            credentials.address,
            Numeric.toHexString(PROJECT_OFFICER_ROLE),
            response.error.code,
            response.error.message
        )
        return false
    }

    val decoded = FunctionReturnDecoder.decode(
        response.value,
        function.outputParameters
    )

    return decoded.isNotEmpty() && (decoded[0] as Bool).value
}
```

---

```kotlin
private fun sendProjectTransaction(stage: String, call: RemoteFunctionCall<TransactionReceipt>): TransactionReceipt {
    try {
        if (stage == FUNC_CREATEPROJECT) {
            val roleHex = Numeric.toHexString(PROJECT_OFFICER_ROLE)
            val hasRole = hasProjectOfficerRole()

            log.info(
                "Web3j role precheck: stage={} sender={} role=PROJECT_OFFICER_ROLE roleHash={} hasRole={}",
                stage,
                credentials.address,
                roleHex,
                hasRole
            )

            if (!hasRole) {
                throw IllegalStateException(
                    "Web3j transaction rejected before send: stage=$stage sender=${credentials.address} has no PROJECT_OFFICER_ROLE=$roleHex"
                )
            }
        }

        val receipt = call.send().requireSuccess()
        log.info(
            "Web3j transaction accepted: stage={} hash={} block={} gasUsed={}",
            stage,
            receipt.transactionHash,
            receipt.blockNumber,
            receipt.gasUsed
        )
        return receipt
    } catch (ex: Exception) {
        log.error("Web3j transaction failed before acceptance: stage={}", stage, ex)
        throw ex
    }
}
```


```kotlin
import org.web3j.abi.FunctionEncoder
import org.web3j.abi.FunctionReturnDecoder
import org.web3j.abi.TypeReference
import org.web3j.abi.datatypes.Address
import org.web3j.abi.datatypes.Bool
import org.web3j.abi.datatypes.Function
import org.web3j.abi.datatypes.generated.Bytes32
import org.web3j.protocol.core.DefaultBlockParameterName
import org.web3j.protocol.core.methods.request.Transaction
```