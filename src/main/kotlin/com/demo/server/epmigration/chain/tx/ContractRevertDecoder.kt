package com.demo.server.epmigration.chain.tx

import org.web3j.crypto.Hash
import org.web3j.crypto.Keys
import org.web3j.utils.Numeric
import java.math.BigInteger
import java.nio.charset.StandardCharsets
import java.util.Locale

data class DecodedContractRevert(
    val name: String,
    val signature: String,
    val args: List<Pair<String, String>>,
    val data: String
) {
    fun toLogMessage(): String {
        val renderedArgs = args.joinToString(", ") { "${it.first}=${it.second}" }
        return if (renderedArgs.isEmpty()) {
            "$name()"
        } else {
            "$name($renderedArgs)"
        }
    }
}

object ContractRevertDecoder {
    private const val ERROR_STRING_SELECTOR = "08c379a0"
    private const val PANIC_SELECTOR = "4e487b71"

    private val customErrors = listOf(
        customError("AccessControlUnauthorizedAccount(address,bytes32)", listOf("account", "neededRole")) {
            listOf(decodeAddress(it, 0), decodeBytes32(it, 1))
        },
        customError("AccessControlBadConfirmation()", emptyList()) { emptyList() },
        customError("UnauthorizedCaller(address)", listOf("caller")) {
            listOf(decodeAddress(it, 0))
        },
        customError("DuplicateProjectId(string)", listOf("externalProjectId")) {
            listOf(decodeString(it, 0))
        },
        customError("UnknownProject(uint256)", listOf("projectId")) {
            listOf(decodeUint256(it, 0))
        },
        customError("UnknownClaim(uint256)", listOf("claimId")) {
            listOf(decodeUint256(it, 0))
        },
        customError("UnknownInvoice(uint256)", listOf("invoiceId")) {
            listOf(decodeUint256(it, 0))
        },
        customError("UnknownPaymentOrder(uint256)", listOf("paymentOrderId")) {
            listOf(decodeUint256(it, 0))
        },
        customError("InvalidState(string)", listOf("reason")) {
            listOf(decodeString(it, 0))
        },
        customError("InvalidActor(address)", listOf("actor")) {
            listOf(decodeAddress(it, 0))
        },
        customError("InvalidInput(string)", listOf("reason")) {
            listOf(decodeString(it, 0))
        },
        customError("InvalidApprover(bytes32)", listOf("userHash")) {
            listOf(decodeBytes32(it, 0))
        },
        customError("InvalidApproverTurn(address,address)", listOf("expected", "actual")) {
            listOf(decodeAddress(it, 0), decodeAddress(it, 1))
        },
        customError("DuplicatePayment(string)", listOf("paymentOrderId")) {
            listOf(decodeString(it, 0))
        },
        customError("DuplicatePaymentReceipt(uint256)", listOf("paymentId")) {
            listOf(decodeUint256(it, 0))
        },
        customError("UnknownPayment(uint256)", listOf("paymentId")) {
            listOf(decodeUint256(it, 0))
        },
        customError("UnknownPaymentReceipt(uint256)", listOf("paymentReceiptId")) {
            listOf(decodeUint256(it, 0))
        },
        customError("UnknownPartyAccount(string,string)", listOf("party", "accountName")) {
            listOf(decodeString(it, 0), decodeString(it, 1))
        },
        customError("DuplicateAccountName(string)", listOf("accountName")) {
            listOf(decodeString(it, 0))
        },
        customError("UnknownContact(uint256)", listOf("contactId")) {
            listOf(decodeUint256(it, 0))
        }
    ).associateBy { selector(it.signature) }

    fun decode(rawData: String?): DecodedContractRevert? {
        val data = extractHex(rawData) ?: return null
        val clean = Numeric.cleanHexPrefix(data).toLowerCase(Locale.US)
        if (clean.length < 8) {
            return null
        }

        val selector = clean.substring(0, 8)
        val payload = clean.substring(8)

        if (selector == ERROR_STRING_SELECTOR) {
            val reason = decodeString(payload, 0)
            return DecodedContractRevert(
                name = "Error",
                signature = "Error(string)",
                args = listOf("reason" to reason),
                data = Numeric.prependHexPrefix(clean)
            )
        }

        if (selector == PANIC_SELECTOR) {
            return DecodedContractRevert(
                name = "Panic",
                signature = "Panic(uint256)",
                args = listOf("code" to decodeUint256(payload, 0)),
                data = Numeric.prependHexPrefix(clean)
            )
        }

        val customError = customErrors[selector]
        if (customError != null) {
            val values = customError.decode(payload)
            return DecodedContractRevert(
                name = customError.name,
                signature = customError.signature,
                args = customError.argNames.zip(values),
                data = Numeric.prependHexPrefix(clean)
            )
        }

        return DecodedContractRevert(
            name = "UnknownCustomError",
            signature = "0x$selector",
            args = listOf("rawData" to Numeric.prependHexPrefix(clean)),
            data = Numeric.prependHexPrefix(clean)
        )
    }

    fun extractHex(vararg candidates: String?): String? {
        candidates.forEach { candidate ->
            if (candidate != null) {
                val match = Regex("0x[0-9a-fA-F]+").find(candidate)
                if (match != null) {
                    return match.value
                }
            }
        }
        return null
    }

    private fun customError(
        signature: String,
        argNames: List<String>,
        decode: (String) -> List<String>
    ): CustomError {
        return CustomError(
            signature = signature,
            name = signature.substringBefore("("),
            argNames = argNames,
            decode = decode
        )
    }

    private fun selector(signature: String): String {
        return Numeric.cleanHexPrefix(Hash.sha3String(signature)).substring(0, 8)
    }

    private fun decodeAddress(payload: String, index: Int): String {
        val raw = word(payload, index).takeLast(40)
        return Keys.toChecksumAddress("0x$raw")
    }

    private fun decodeBytes32(payload: String, index: Int): String {
        return "0x${word(payload, index)}"
    }

    private fun decodeUint256(payload: String, index: Int): String {
        return BigInteger(word(payload, index), 16).toString()
    }

    private fun decodeString(payload: String, index: Int): String {
        val offsetBytes = BigInteger(word(payload, index), 16).toInt()
        val lengthWordStart = offsetBytes * 2
        if (payload.length < lengthWordStart + 64) {
            return "<malformed string>"
        }

        val length = BigInteger(payload.substring(lengthWordStart, lengthWordStart + 64), 16).toInt()
        val valueStart = lengthWordStart + 64
        val valueEnd = valueStart + length * 2
        if (payload.length < valueEnd) {
            return "<malformed string>"
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

    private data class CustomError(
        val signature: String,
        val name: String,
        val argNames: List<String>,
        val decode: (String) -> List<String>
    )
}
