```kotlin
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.web3j.crypto.Hash

object AbiFunctionSelectors {
    private val objectMapper = jacksonObjectMapper()

    private val topazLifecycleAbi: JsonNode by lazy {
        val inputStream = Thread.currentThread()
            .contextClassLoader
            .getResourceAsStream("abi/TopazLifecycle.json")
            ?: error("ABI file not found: resources/abi/TopazLifecycle.json")

        objectMapper.readTree(inputStream).get("abi")
    }

    fun selector(functionName: String): String {
        val function = topazLifecycleAbi.firstOrNull {
            it.get("type")?.asText() == "function" &&
                    it.get("name")?.asText() == functionName
        } ?: error("Function not found in ABI: $functionName")

        val inputs = function.get("inputs")
        val canonicalInputs = inputs.joinToString(",") { canonicalType(it) }
        val signature = "$functionName($canonicalInputs)"

        return Hash.sha3String(signature).take(10)
    }

    private fun canonicalType(node: JsonNode): String {
        val type = node.get("type").asText()

        if (!type.startsWith("tuple")) {
            return type
        }

        val tupleBody = node.get("components").joinToString(
            prefix = "(",
            postfix = ")",
            separator = ","
        ) { component ->
            canonicalType(component)
        }

        val arraySuffix = type.removePrefix("tuple")
        return tupleBody + arraySuffix
    }
}
```

---

```kotlin
private fun tx(name: String, input: Type<*>): RemoteCall<TransactionReceipt> {
    val selector = AbiFunctionSelectors.selector(name)
    val paramsData = FunctionEncoder.encodeConstructor(listOf(input))
    val data = selector + Numeric.cleanHexPrefix(paramsData)

    return RemoteCall {
        executeTransaction(data, BigInteger.ZERO)
    }
}
```


```kotlin
import org.web3j.abi.FunctionEncoder
import org.web3j.protocol.core.RemoteCall
import org.web3j.utils.Numeric
import java.math.BigInteger
```

diagnoseProjectTransaction(stage, call)