package com.demo.server.epmigration.config

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class Web3jConfigTests {
    private val config = Web3jConfig()

    @Test
    fun `chain credentials accept private keys with or without hex prefix`() {
        val withoutPrefix = EpChainProperties().apply { signerPrivateKey = PRIVATE_KEY }
        val withPrefix = EpChainProperties().apply { signerPrivateKey = "0x$PRIVATE_KEY" }

        assertEquals(config.chainCredentials(withoutPrefix).address, config.chainCredentials(withPrefix).address)
    }

    @Test
    fun `chain credentials reject malformed private keys`() {
        listOf("", "0x1234", "not-a-key", "0x${"0".repeat(63)}").forEach { key ->
            val properties = EpChainProperties().apply { signerPrivateKey = key }

            val ex = assertThrows(IllegalStateException::class.java, {
                config.chainCredentials(properties)
            }, key)

            assertEquals("ep.chain.signer-private-key must be a 32-byte hex private key", ex.message)
        }
    }

    companion object {
        private const val PRIVATE_KEY = "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef"
    }
}
