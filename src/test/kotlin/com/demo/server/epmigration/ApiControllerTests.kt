package com.demo.server.epmigration

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ApiControllerTests {
    @Test
    fun `ping reports the service is running`() {
        val body = ApiController().ping()

        assertEquals("success", body["status"])
        assertEquals("ep-migration api is running", body["message"])
    }
}
