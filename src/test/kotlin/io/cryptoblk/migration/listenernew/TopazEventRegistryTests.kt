package io.cryptoblk.migration.listenernew

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class TopazEventRegistryTests {
    private val workflow = TopazWorkflowService()

    @Test
    fun `builds subscriptions for every supported event-emitting contract`() {
        val subscriptions = allSubscriptions()

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
        val subscriptions = allSubscriptions()
        val routeKeys = subscriptions.map { it.contractAddress to it.topic0 }

        assertEquals(subscriptions.size, routeKeys.toSet().size)
        assertTrue(subscriptions.filter { it.eventName == "RoleGranted" }.map { it.topic0 }.toSet().size == 1)
    }

    @Test
    fun `each subscription maps to its own workflow handler`() {
        val subscriptions = allSubscriptions()
        val handlersByRoute = subscriptions.associate { "${it.contractName}.${it.eventName}" to it.handlerName }

        assertEquals(subscriptions.size, subscriptions.map { it.handlerName }.toSet().size)
        subscriptions.forEach { subscription ->
            assertEquals(expectedHandlerName(subscription.contractName, subscription.eventName), subscription.handlerName)
        }
        assertEquals("onLifecycleRoleGranted", handlersByRoute.getValue("lifecycle.RoleGranted"))
        assertEquals("onPaymentRoleGranted", handlersByRoute.getValue("payment.RoleGranted"))
        assertEquals("onContactsRoleGranted", handlersByRoute.getValue("contacts.RoleGranted"))
        assertEquals("onLifecycleProjectCreated", handlersByRoute.getValue("lifecycle.ProjectCreated"))
        assertEquals("onPaymentPaymentCreated", handlersByRoute.getValue("payment.PaymentCreated"))
        assertEquals("onContactsContactUpserted", handlersByRoute.getValue("contacts.ContactUpserted"))
    }

    private fun allSubscriptions(): List<TopazEventSubscription> {
        return TopazEventRegistry.subscriptions(
            lifecycleAddress = LIFECYCLE_ADDRESS,
            paymentAddress = PAYMENT_ADDRESS,
            contactsAddress = CONTACTS_ADDRESS,
            workflow = workflow
        )
    }

    private fun expectedHandlerName(contractName: String, eventName: String): String {
        val contractPrefix = when (contractName) {
            "lifecycle" -> "Lifecycle"
            "payment" -> "Payment"
            "contacts" -> "Contacts"
            else -> error("Unsupported contract '$contractName'")
        }
        return "on$contractPrefix$eventName"
    }

    private companion object {
        private const val LIFECYCLE_ADDRESS = "0x0000000000000000000000000000000000000001"
        private const val PAYMENT_ADDRESS = "0x0000000000000000000000000000000000000002"
        private const val CONTACTS_ADDRESS = "0x0000000000000000000000000000000000000003"
    }
}
