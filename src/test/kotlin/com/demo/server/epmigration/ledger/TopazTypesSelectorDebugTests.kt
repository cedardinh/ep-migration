package com.demo.server.epmigration.ledger

import com.demo.server.epmigration.chain.generated.TopazLifecycle
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.web3j.crypto.Credentials
import org.web3j.protocol.Web3j
import java.math.BigInteger

class TopazTypesSelectorDebugTests {
    @Test
    fun `generated topaz wrapper produces create project selector`() {
        val input = TopazLifecycle.CreateProjectInput(
            "selector-debug",
            "Selector Debug",
            TopazLifecycle.Participant(WALLET, "Developer Ltd", "", "", "", "", "developer"),
            listOf(TopazLifecycle.Participant(WALLET, "Contractor Ltd", "", "", "", "", "contractor")),
            listOf(
                TopazLifecycle.ApproverConfig(
                    WALLET,
                    "0xf09b66dfb6bd1bb5e7d2be0b15a80542e02b79b94ea63cd7e918ac65b1164a9a".hexBytes(),
                    "claim@example.com",
                    "Claim",
                    "Approver",
                    "claim-approver",
                    "Claim Approver",
                    "claim-approver"
                )
            ),
            listOf(
                TopazLifecycle.ApproverConfig(
                    WALLET,
                    "0x06a649d9b77f6b7a90a57443026f693d362b91ab6d64aac3557edef254d5efeb".hexBytes(),
                    "payment@example.com",
                    "Payment",
                    "Approver",
                    "payment-approver",
                    "Payment Approver",
                    "payment-approver"
                )
            ),
            listOf("bank-1")
        )
        val lifecycle = TopazLifecycle.load(
            CONTRACT_ADDRESS,
            Mockito.mock(Web3j::class.java),
            Credentials.create(PRIVATE_KEY),
            BigInteger.ZERO,
            BigInteger.ONE
        )

        val calldata = lifecycle.createProject(input).encodeFunctionCall()

        assertEquals("0xcd9c2f36", calldata.take(10))
    }

    companion object {
        private const val CONTRACT_ADDRESS = "0x0000000000000000000000000000000000000001"
        private const val PRIVATE_KEY = "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef"
        private const val WALLET = "0x628d684197485c054cda7d3def46e8be6b3d174c"

        private fun String.hexBytes(): ByteArray {
            val clean = removePrefix("0x")
            return ByteArray(clean.length / 2) { index ->
                clean.substring(index * 2, index * 2 + 2).toInt(16).toByte()
            }
        }
    }
}
