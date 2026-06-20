```kotlin
private fun sendCreateProjectWithFixedSelector(
    call: RemoteFunctionCall<TransactionReceipt>
): TransactionReceipt {
    val originalData = call.encodeFunctionCall()
    val fixedData = "0x77bb68aa" + Numeric.cleanHexPrefix(originalData).drop(8)

    log.info(
        "Web3j fixed selector send: oldSelector={} newSelector={} dataLength={}",
        originalData.take(10),
        fixedData.take(10),
        fixedData.length
    )

    val sent = transactionManager.sendTransaction(
        gasProvider.getGasPrice(FUNC_CREATEPROJECT),
        gasProvider.getGasLimit(FUNC_CREATEPROJECT),
        lifecycle.contractAddress,
        fixedData,
        BigInteger.ZERO
    )

    if (sent.hasError()) {
        throw IllegalStateException(
            "send fixed createProject failed: code=${sent.error.code}, message=${sent.error.message}, data=${sent.error.data}"
        )
    }

    val receipt = PollingTransactionReceiptProcessor(web3j, 15000, 40)
        .waitForTransactionReceipt(sent.transactionHash)

    return receipt.requireSuccess()
}
```

---

```kotlin
diagnoseProjectTransaction(stage, call)

val receipt =
    if (stage == FUNC_CREATEPROJECT) {
        sendCreateProjectWithFixedSelector(call)
    } else {
        call.send().requireSuccess()
    }
```


```kotlin
import org.web3j.tx.response.PollingTransactionReceiptProcessor
import org.web3j.utils.Numeric
import java.math.BigInteger
```

diagnoseProjectTransaction(stage, call)