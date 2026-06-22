package com.demo.server.epmigration.web

import com.demo.server.epmigration.chain.error.TransactionSubmissionFailedException
import com.demo.server.epmigration.chain.tx.ChainCallContext
import com.demo.server.epmigration.observability.ChainCallReporter
import com.demo.server.epmigration.project.BadProjectRequestException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.http.HttpStatus
import org.springframework.http.converter.HttpMessageNotReadableException

class RestExceptionHandlerTests {
    private val reporter = Mockito.mock(ChainCallReporter::class.java)
    private val handler = RestExceptionHandler(reporter)

    @Test
    fun `bad project request returns public validation error`() {
        val response = handler.handleBadProjectRequest(BadProjectRequestException("bad field"))

        assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)
        assertEquals("BAD_PROJECT_REQUEST", response.body!!.errorCode)
        assertEquals("bad field", response.body!!.message)
    }

    @Test
    fun `bad project request with missing message returns default validation error`() {
        val response = handler.handleBadProjectRequest(BadProjectRequestException(null))

        assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)
        assertEquals("BAD_PROJECT_REQUEST", response.body!!.errorCode)
        assertEquals("Bad project request", response.body!!.message)
    }

    @Test
    fun `unreadable json returns create project parse error`() {
        val response = handler.handleUnreadableRequest(Mockito.mock(HttpMessageNotReadableException::class.java))

        assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)
        assertEquals("BAD_PROJECT_REQUEST", response.body!!.errorCode)
        assertEquals("Request body is not valid JSON for createProject", response.body!!.message)
    }

    @Test
    fun `chain exception returns chain status and reports failure`() {
        val context = context(httpStatus = 502)
        val response = handler.handleChainException(TransactionSubmissionFailedException(context))

        assertEquals(HttpStatus.BAD_GATEWAY, response.statusCode)
        assertEquals("TRANSACTION_SUBMISSION_FAILED", response.body!!.errorCode)
        assertEquals("Blockchain transaction submission failed", response.body!!.message)
        Mockito.verify(reporter).failed(context)
    }

    @Test
    fun `chain exception with unknown http status falls back to internal server error`() {
        val context = context(httpStatus = 599)
        val response = handler.handleChainException(TransactionSubmissionFailedException(context))

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.statusCode)
        Mockito.verify(reporter).failed(context)
    }

    private fun context(httpStatus: Int): ChainCallContext =
        ChainCallContext(
            op = "createProject",
            externalProjectId = "project-1",
            from = "0xfrom",
            to = "0xto",
            httpStatus = httpStatus
        )
}
