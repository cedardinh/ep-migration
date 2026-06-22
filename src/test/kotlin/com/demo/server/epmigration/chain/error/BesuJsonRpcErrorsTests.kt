package com.demo.server.epmigration.chain.error

import com.demo.server.epmigration.chain.tx.ChainCallContext
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.web3j.protocol.core.Response
import java.math.BigInteger

class BesuJsonRpcErrorsTests {
    @Test
    fun `documented Besu send error messages are classified`() {
        val cases = listOf(
            ErrorCase("Known transaction", BesuJsonRpcError.KNOWN_TRANSACTION),
            ErrorCase("Nonce too high", BesuJsonRpcError.FUTURE_NONCE),
            ErrorCase("Transaction nonce is too distant from current sender nonce", BesuJsonRpcError.FUTURE_NONCE),
            ErrorCase("Nonce too low", BesuJsonRpcError.NONCE_CONFLICT),
            ErrorCase("Replacement transaction underpriced", BesuJsonRpcError.NONCE_CONFLICT),
            ErrorCase("An invalid transaction with a lower nonce exists", BesuJsonRpcError.NONCE_CONFLICT),
            ErrorCase("Internal error", BesuJsonRpcError.RPC_UNAVAILABLE),
            ErrorCase("Timeout expired", BesuJsonRpcError.RPC_UNAVAILABLE),
            ErrorCase("Initial sync is still in progress", BesuJsonRpcError.RPC_UNAVAILABLE),
            ErrorCase(
                "Transaction pool not enabled. (Either txpool explicitly disabled, or node not yet in sync).",
                BesuJsonRpcError.RPC_UNAVAILABLE
            ),
            ErrorCase("World state unavailable", BesuJsonRpcError.RPC_UNAVAILABLE),
            ErrorCase("Block not found", BesuJsonRpcError.RPC_UNAVAILABLE),
            ErrorCase(
                "Transaction processing could not be completed due to an exception",
                BesuJsonRpcError.RPC_UNAVAILABLE
            ),
            ErrorCase("Invalid params", BesuJsonRpcError.SUBMISSION_REJECTED),
            ErrorCase("Invalid block, unable to parse RLP", BesuJsonRpcError.SUBMISSION_REJECTED),
            ErrorCase("Invalid input", BesuJsonRpcError.SUBMISSION_REJECTED),
            ErrorCase("Invalid transaction params (missing or incorrect)", BesuJsonRpcError.SUBMISSION_REJECTED),
            ErrorCase("Invalid signature", BesuJsonRpcError.SUBMISSION_REJECTED),
            ErrorCase("Invalid transaction type", BesuJsonRpcError.SUBMISSION_REJECTED),
            ErrorCase("Intrinsic gas exceeds gas limit", BesuJsonRpcError.SUBMISSION_REJECTED),
            ErrorCase("Upfront cost exceeds account balance", BesuJsonRpcError.SUBMISSION_REJECTED),
            ErrorCase("Transaction gas limit exceeds block gas limit", BesuJsonRpcError.SUBMISSION_REJECTED),
            ErrorCase("Sender account not authorized to send transactions", BesuJsonRpcError.SUBMISSION_REJECTED),
            ErrorCase("Gas price below configured minimum gas price", BesuJsonRpcError.SUBMISSION_REJECTED),
            ErrorCase("Gas price below current base fee", BesuJsonRpcError.SUBMISSION_REJECTED),
            ErrorCase("blob gas price below current blob base fee", BesuJsonRpcError.SUBMISSION_REJECTED),
            ErrorCase("Wrong chainId", BesuJsonRpcError.SUBMISSION_REJECTED),
            ErrorCase("ChainId not supported", BesuJsonRpcError.SUBMISSION_REJECTED),
            ErrorCase("ChainId is required", BesuJsonRpcError.SUBMISSION_REJECTED),
            ErrorCase("Transaction fee cap exceeded", BesuJsonRpcError.SUBMISSION_REJECTED),
            ErrorCase("Max priority fee per gas exceeds max fee per gas", BesuJsonRpcError.SUBMISSION_REJECTED),
            ErrorCase("Unsupported private transaction type", BesuJsonRpcError.SUBMISSION_REJECTED),
            ErrorCase("Offchain Privacy group does not exist.", BesuJsonRpcError.SUBMISSION_REJECTED),
            ErrorCase("Private transaction invalid", BesuJsonRpcError.SUBMISSION_REJECTED),
            ErrorCase("Private transaction failed", BesuJsonRpcError.SUBMISSION_REJECTED),
            ErrorCase("Total blob gas too high", BesuJsonRpcError.SUBMISSION_REJECTED),
            ErrorCase("Plugin has marked the transaction as invalid", BesuJsonRpcError.SUBMISSION_REJECTED),
            ErrorCase("blobs failed kzg validation", BesuJsonRpcError.SUBMISSION_REJECTED)
        )

        cases.forEach { case ->
            assertEquals(case.expected, BesuJsonRpcError.from(case.message), case.message)
        }
    }

    @Test
    fun `Besu error classification is case and whitespace insensitive`() {
        assertEquals(BesuJsonRpcError.KNOWN_TRANSACTION, BesuJsonRpcError.from("  known transaction  "))
        assertEquals(BesuJsonRpcError.RPC_UNAVAILABLE, BesuJsonRpcError.from("INITIAL SYNC IS STILL IN PROGRESS"))
    }

    @Test
    fun `unknown or missing Besu error message falls back to unknown`() {
        assertEquals(BesuJsonRpcError.UNKNOWN, BesuJsonRpcError.from(null))
        assertEquals(BesuJsonRpcError.UNKNOWN, BesuJsonRpcError.from("not in the Besu table"))
    }

    @Test
    fun `send error helpers detect known transaction and build exceptions`() {
        assertEquals(true, BesuJsonRpcErrors.isKnownTransaction(error("Known transaction")))
        assertEquals(false, BesuJsonRpcErrors.isKnownTransaction(error("Nonce too high")))

        val ex = BesuJsonRpcErrors.sendError(context(), BigInteger.TEN, error("Nonce too high"))
        assertEquals(ChainErrorType.NONCE_UNAVAILABLE, ex.type)
        assertEquals("10", ex.context.nonce)
    }

    @Test
    fun `each classification exposes its mapped chain error type`() {
        assertEquals(ChainErrorType.TRANSACTION_SUBMISSION_FAILED, BesuJsonRpcError.KNOWN_TRANSACTION.chainError)
        assertEquals(ChainErrorType.NONCE_UNAVAILABLE, BesuJsonRpcError.FUTURE_NONCE.chainError)
        assertEquals(ChainErrorType.NONCE_UNAVAILABLE, BesuJsonRpcError.NONCE_CONFLICT.chainError)
        assertEquals(ChainErrorType.RPC_UNAVAILABLE, BesuJsonRpcError.RPC_UNAVAILABLE.chainError)
        assertEquals(ChainErrorType.TRANSACTION_SUBMISSION_FAILED, BesuJsonRpcError.SUBMISSION_REJECTED.chainError)
        assertEquals(ChainErrorType.TRANSACTION_SUBMISSION_FAILED, BesuJsonRpcError.UNKNOWN.chainError)
    }

    @Test
    fun `nonce error preserves existing context nonce when no new nonce is supplied`() {
        val ex = BesuJsonRpcErrors.nonceError(context().copy(nonce = "cached"), error("nonce failed"))

        assertEquals("cached", ex.context.nonce)
        assertEquals("nonce failed", ex.context.rpcMessage)
    }

    private fun context(): ChainCallContext =
        ChainCallContext(
            op = "createProject",
            externalProjectId = "project-1",
            from = "0xfrom",
            to = "0xto"
        )

    private fun error(message: String): Response.Error = Response.Error(-32000, message)

    private data class ErrorCase(
        val message: String,
        val expected: BesuJsonRpcError
    )
}
