package io.cryptoblk.migration.listenernew

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class TopazContractAddressesTests {

    @Test
    fun `returns addresses in listener order`() {
        val addresses = TopazContractAddresses(
            lifecycle = LIFECYCLE_ADDRESS,
            payment = PAYMENT_ADDRESS,
            contacts = CONTACTS_ADDRESS
        )

        assertEquals(
            listOf(LIFECYCLE_ADDRESS, PAYMENT_ADDRESS, CONTACTS_ADDRESS),
            addresses.all()
        )
    }

    @Test
    fun `normalizes configured addresses`() {
        assertEquals(
            "0x000000000000000000000000000000000000000a",
            TopazContractAddresses.normalize(" 0x000000000000000000000000000000000000000A ")
        )
    }

    private companion object {
        private const val LIFECYCLE_ADDRESS = "0x0000000000000000000000000000000000000001"
        private const val PAYMENT_ADDRESS = "0x0000000000000000000000000000000000000002"
        private const val CONTACTS_ADDRESS = "0x0000000000000000000000000000000000000003"
    }
}
