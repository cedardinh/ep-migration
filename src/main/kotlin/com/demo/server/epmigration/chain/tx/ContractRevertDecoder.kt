package com.demo.server.epmigration.chain.tx

import org.web3j.utils.Numeric
import java.math.BigInteger
import java.nio.charset.StandardCharsets
import java.util.Locale

data class DecodedContractRevert(
    val kind: String,
    val selector: String,
    val value: String?,
    val data: String
) {
    fun toLogMessage(): String {
        return if (value.isNullOrBlank()) {
            "$kind(selector=$selector)"
        } else {
            "$kind($value)"
        }
    }
}

object ContractRevertDecoder {
    private const val ERROR_STRING_SELECTOR = "08c379a0"
    private const val PANIC_SELECTOR = "4e487b71"
    private const val MALFORMED_STRING = "<malformed string>"

    private val hexValuePattern = Regex("0x[0-9a-fA-F]+")

    fun decode(rawData: String?): DecodedContractRevert? {
        val data = extractHex(rawData) ?: return null
        val clean = Numeric.cleanHexPrefix(data).toLowerCase(Locale.US)
        if (clean.length < 8) {
            return null
        }

        val selector = clean.substring(0, 8)
        val payload = clean.substring(8)
        val dataWithPrefix = Numeric.prependHexPrefix(clean)

        return when (selector) {
            ERROR_STRING_SELECTOR -> standardError(payload, dataWithPrefix)
            PANIC_SELECTOR -> panic(payload, dataWithPrefix)
            else -> customError(selector, dataWithPrefix)
        }
    }

    fun extractHex(vararg candidates: String?): String? {
        candidates.forEach { candidate ->
            if (candidate != null) {
                val match = hexValuePattern.find(candidate)
                if (match != null) {
                    return match.value
                }
            }
        }
        return null
    }

    private fun standardError(payload: String, data: String): DecodedContractRevert =
        DecodedContractRevert(
            kind = "Error",
            selector = "0x$ERROR_STRING_SELECTOR",
            value = decodeString(payload),
            data = data
        )

    private fun panic(payload: String, data: String): DecodedContractRevert =
        DecodedContractRevert(
            kind = "Panic",
            selector = "0x$PANIC_SELECTOR",
            value = decodeUint256(payload),
            data = data
        )

    private fun customError(selector: String, data: String): DecodedContractRevert =
        DecodedContractRevert(
            kind = "CustomError",
            selector = "0x$selector",
            value = null,
            data = data
        )

    private fun decodeUint256(payload: String): String {
        return BigInteger(word(payload, 0), 16).toString()
    }

    private fun decodeString(payload: String): String {
        val offsetBytes = BigInteger(word(payload, 0), 16).toInt()
        val lengthWordStart = offsetBytes * 2
        if (payload.length < lengthWordStart + 64) {
            return MALFORMED_STRING
        }

        val length = BigInteger(payload.substring(lengthWordStart, lengthWordStart + 64), 16).toInt()
        val valueStart = lengthWordStart + 64
        val valueEnd = valueStart + length * 2
        if (payload.length < valueEnd) {
            return MALFORMED_STRING
        }

        val bytes = Numeric.hexStringToByteArray("0x${payload.substring(valueStart, valueEnd)}")
        return String(bytes, StandardCharsets.UTF_8)
    }

    private fun word(payload: String, index: Int): String {
        val start = index * 64
        val end = start + 64
        if (payload.length < end) {
            return "0".repeat(64)
        }
        return payload.substring(start, end)
    }
}
