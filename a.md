```kotlin
private fun diagnoseProjectTransaction(
    stage: String,
    call: RemoteFunctionCall<TransactionReceipt>
) {
    if (stage != FUNC_CREATEPROJECT) {
        return
    }

    val from = credentials.address
    val to = lifecycle.contractAddress
    val data = call.encodeFunctionCall()
    val selector = data.take(10)

    log.info(
        "Web3j diagnose tx: stage={} from={} to={} selector={} dataLength={}",
        stage,
        from,
        to,
        selector,
        data.length
    )

    val code = web3j.ethGetCode(to, DefaultBlockParameterName.LATEST).send()
    log.info(
        "Web3j diagnose contract code: stage={} hasError={} codeLength={} codeHead={} rpcCode={} rpcMessage={}",
        stage,
        code.hasError(),
        code.code?.length ?: 0,
        code.code?.take(20),
        code.error?.code,
        code.error?.message
    )

    val callTx = Transaction.createEthCallTransaction(from, to, data)
    val ethCall = web3j.ethCall(callTx, DefaultBlockParameterName.LATEST).send()
    log.info(
        "Web3j diagnose eth_call: stage={} hasError={} reverted={} value={} rpcCode={} rpcMessage={} rpcData={} raw={}",
        stage,
        ethCall.hasError(),
        ethCall.isReverted,
        ethCall.value,
        ethCall.error?.code,
        ethCall.error?.message,
        ethCall.error?.data,
        ethCall.rawResponse
    )

    val estimateTx = Transaction.createFunctionCallTransaction(
        from,
        null,
        null,
        null,
        to,
        BigInteger.ZERO,
        data
    )
    val estimate = web3j.ethEstimateGas(estimateTx).send()
    log.info(
        "Web3j diagnose estimateGas: stage={} hasError={} amountUsed={} rpcCode={} rpcMessage={} rpcData={} raw={}",
        stage,
        estimate.hasError(),
        if (estimate.hasError()) null else estimate.amountUsed,
        estimate.error?.code,
        estimate.error?.message,
        estimate.error?.data,
        estimate.rawResponse
    )
}
```

---

```kotlin

```


```kotlin
import org.web3j.protocol.core.DefaultBlockParameterName
import org.web3j.protocol.core.RemoteFunctionCall
import org.web3j.protocol.core.methods.request.Transaction
import org.web3j.protocol.core.methods.response.TransactionReceipt
import java.math.BigInteger
```

diagnoseProjectTransaction(stage, call)