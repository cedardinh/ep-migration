package com.demo.server.epmigration.web

import com.demo.server.epmigration.chain.error.ChainException
import com.demo.server.epmigration.observability.ChainCallReporter
import com.demo.server.epmigration.project.BadProjectRequestException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler

@ControllerAdvice
class RestExceptionHandler(
    private val reporter: ChainCallReporter
) {
    @ExceptionHandler(BadProjectRequestException::class)
    fun handleBadProjectRequest(
        ex: BadProjectRequestException
    ): ResponseEntity<ApiErrorResponse> {
        return error(
            HttpStatus.BAD_REQUEST,
            "BAD_PROJECT_REQUEST",
            ex.message ?: "Bad project request"
        )
    }

    @ExceptionHandler(HttpMessageNotReadableException::class)
    fun handleUnreadableRequest(
        ex: HttpMessageNotReadableException
    ): ResponseEntity<ApiErrorResponse> {
        return error(
            HttpStatus.BAD_REQUEST,
            "BAD_PROJECT_REQUEST",
            "Request body is not valid JSON for createProject"
        )
    }

    @ExceptionHandler(ChainException::class)
    fun handleChainException(ex: ChainException): ResponseEntity<ApiErrorResponse> {
        reporter.failed(ex.context)
        val status = HttpStatus.resolve(ex.context.httpStatus) ?: HttpStatus.INTERNAL_SERVER_ERROR
        return error(status, ex.errorCode, ex.publicMessage)
    }

    private fun error(
        status: HttpStatus,
        code: String,
        message: String
    ): ResponseEntity<ApiErrorResponse> {
        return ResponseEntity.status(status).body(ApiErrorResponse(code, message))
    }
}
