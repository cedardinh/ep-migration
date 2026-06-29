package io.cryptoblk.migration.listenernew

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class TopazEventRegistryTests {
    private val workflow = TopazWorkflowService()

    @Test
    fun `builds subscriptions for every supported event-emitting contract`() {
        val subscriptions = TopazEventRegistry.subscriptions(allContractAddresses(), workflow)

        val eventNamesByContract = subscriptions.groupBy { it.contractName }
            .mapValues { entry -> entry.value.map { it.eventName } }

        assertEquals(
            listOf(
                "ProjectCreated",
                "ProjectStatusChanged",
                "ProjectUpdated",
                "ProjectApproverRemoved",
                "ClaimCreated",
                "ClaimDocumentsUpdated",
                "ClaimStatusChanged",
                "InvoiceCreated",
                "InvoiceDocumentsUpdated",
                "InvoiceStatusChanged",
                "PaymentOrderCreated",
                "PaymentOrderStatusChanged",
                "PaymentCreatedForOrder",
                "BankPaymentRequested",
                "BankPaymentReferenceRecorded",
                "RoleAdminChanged",
                "RoleGranted",
                "RoleRevoked"
            ),
            eventNamesByContract.getValue("lifecycle")
        )
        assertEquals(
            listOf(
                "PaymentCreated",
                "PaymentAccepted",
                "PaymentRejected",
                "PaymentReceiptCreated",
                "RoleAdminChanged",
                "RoleGranted",
                "RoleRevoked"
            ),
            eventNamesByContract.getValue("payment")
        )
        assertEquals(
            listOf(
                "ContactUpserted",
                "ContactDeactivated",
                "RoleAdminChanged",
                "RoleGranted",
                "RoleRevoked"
            ),
            eventNamesByContract.getValue("contacts")
        )
        assertEquals(30, subscriptions.size)
    }

    @Test
    fun `routes are unique by address and topic0 even when contracts share role topics`() {
        val subscriptions = TopazEventRegistry.subscriptions(allContractAddresses(), workflow)
        val routeKeys = subscriptions.map { it.contractAddress to it.topic0 }

        assertEquals(subscriptions.size, routeKeys.toSet().size)
        assertTrue(subscriptions.filter { it.eventName == "RoleGranted" }.map { it.topic0 }.toSet().size == 1)
    }

    @Test
    fun `skips contracts without a configured address`() {
        val subscriptions = TopazEventRegistry.subscriptions(
            TopazContractAddresses(
                lifecycle = LIFECYCLE_ADDRESS,
                payment = "",
                contacts = ""
            ),
            workflow
        )

        assertEquals(setOf("lifecycle"), subscriptions.map { it.contractName }.toSet())
        assertEquals(18, subscriptions.size)
    }

    private fun allContractAddresses(): TopazContractAddresses {
        return TopazContractAddresses(
            lifecycle = LIFECYCLE_ADDRESS,
            payment = PAYMENT_ADDRESS,
            contacts = CONTACTS_ADDRESS
        )
    }

    private companion object {
        private const val LIFECYCLE_ADDRESS = "0x0000000000000000000000000000000000000001"
        private const val PAYMENT_ADDRESS = "0x0000000000000000000000000000000000000002"
        private const val CONTACTS_ADDRESS = "0x0000000000000000000000000000000000000003"
    }
}
