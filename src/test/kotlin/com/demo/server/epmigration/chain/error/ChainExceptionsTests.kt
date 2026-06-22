package com.demo.server.epmigration.chain.error

import com.demo.server.epmigration.chain.tx.ChainCallContext
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ChainExceptionsTests {
    @Test
    fun `base chain exception exposes error code and public message without a cause`() {
        val exception = ChainException(ChainErrorType.RPC_UNAVAILABLE, context())

        assertEquals(ChainErrorType.RPC_UNAVAILABLE, exception.type)
        assertEquals("CHAIN_RPC_UNAVAILABLE", exception.errorCode)
        assertEquals("Blockchain RPC is unavailable", exception.publicMessage)
        assertEquals("Blockchain RPC is unavailable", exception.message)
        assertNull(exception.cause)
    }

    @Test
    fun `concrete chain exceptions default to a null cause`() {
        assertNull(ChainRpcUnavailableException(context()).cause)
        assertNull(NonceUnavailableException(context()).cause)
        assertNull(TransactionSubmissionFailedException(context()).cause)
    }

    @Test
    fun `concrete chain exceptions retain the supplied cause`() {
        val cause = IllegalStateException("boom")

        assertSame(cause, ChainRpcUnavailableException(context(), cause).cause)
        assertSame(cause, NonceUnavailableException(context(), cause).cause)
        assertSame(cause, TransactionSubmissionFailedException(context(), cause).cause)
    }

    @Test
    fun `error type factory builds the matching exception for every type`() {
        val cause = RuntimeException("root")

        val rpc = ChainErrorType.RPC_UNAVAILABLE.exception(context(), cause)
        val nonce = ChainErrorType.NONCE_UNAVAILABLE.exception(context())
        val submission = ChainErrorType.TRANSACTION_SUBMISSION_FAILED.exception(context())

        assertTrue(rpc is ChainRpcUnavailableException)
        assertSame(cause, rpc.cause)
        assertTrue(nonce is NonceUnavailableException)
        assertEquals(ChainErrorType.NONCE_UNAVAILABLE, nonce.type)
        assertTrue(submission is TransactionSubmissionFailedException)
        assertEquals(ChainErrorType.TRANSACTION_SUBMISSION_FAILED, submission.type)
    }

    private fun context(): ChainCallContext =
        ChainCallContext(op = "createProject", externalProjectId = "project-1", from = "0xfrom", to = "0xto")
}
