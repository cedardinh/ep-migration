package io.cryptoblk.migration.listenernew

import com.demo.server.epmigration.chain.generated.TopazContacts
import com.demo.server.epmigration.chain.generated.TopazLifecycle
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.web3j.abi.FunctionEncoder
import org.web3j.abi.TypeEncoder
import org.web3j.abi.datatypes.Address
import org.web3j.abi.datatypes.Bool
import org.web3j.abi.datatypes.Type
import org.web3j.abi.datatypes.Utf8String
import org.web3j.abi.datatypes.generated.Bytes32
import org.web3j.abi.datatypes.generated.Uint256
import org.web3j.abi.datatypes.generated.Uint8
import org.web3j.protocol.core.methods.response.Log
import java.math.BigInteger

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
    fun `decoded event exposes generated wrapper response`() {
        val subscription = allSubscriptions()
            .single { it.contractName == "lifecycle" && it.eventName == "ProjectCreated" }
        val projectId = BigInteger.valueOf(42L)

        val response = subscription.decode(
            chainLog(
                address = LIFECYCLE_ADDRESS,
                topics = listOf(
                    subscription.topic0,
                    topic(Uint256(projectId)),
                    topic(Address(160, DEVELOPER_ADDRESS))
                ),
                data = data(Utf8String("external-project-42"))
            )
        ) as TopazLifecycle.ProjectCreatedEventResponse
        assertEquals(projectId, response.projectId)
        assertEquals("external-project-42", response.externalProjectId)
        assertEquals(DEVELOPER_ADDRESS, response.developerWallet)
    }

    @Test
    fun `decoded generated wrapper responses expose small integer and boolean values`() {
        val statusSubscription = allSubscriptions()
            .single { it.contractName == "lifecycle" && it.eventName == "ProjectStatusChanged" }
        val contactSubscription = allSubscriptions()
            .single { it.contractName == "contacts" && it.eventName == "ContactUpserted" }

        val statusResponse = statusSubscription.decode(
            chainLog(
                address = LIFECYCLE_ADDRESS,
                topics = listOf(
                    statusSubscription.topic0,
                    topic(Uint256(BigInteger.valueOf(7L)))
                ),
                data = data(Uint8(BigInteger.valueOf(3L)))
            )
        ) as TopazLifecycle.ProjectStatusChangedEventResponse
        val contactResponse = contactSubscription.decode(
            chainLog(
                address = CONTACTS_ADDRESS,
                topics = listOf(
                    contactSubscription.topic0,
                    topic(Uint256(BigInteger.valueOf(9L))),
                    topic(Address(160, DEVELOPER_ADDRESS))
                ),
                data = data(
                    Utf8String("party-a"),
                    Utf8String("account-a"),
                    Utf8String("beneficiary"),
                    Bool(true),
                    Bool(false)
                )
            )
        ) as TopazContacts.ContactUpsertedEventResponse
        assertEquals(3, statusResponse.status.toInt())
        assertEquals(DEVELOPER_ADDRESS, contactResponse.wallet)
        assertTrue(contactResponse.created)
        assertFalse(contactResponse.active)
    }

    @Test
    fun `workflow event parameters expose comparable json values`() {
        val subscription = allSubscriptions()
            .single { it.contractName == "lifecycle" && it.eventName == "ProjectApproverRemoved" }
        val projectId = BigInteger.valueOf(42L)
        val userHash = ByteArray(32) { index -> index.toByte() }

        val response = subscription.decode(
            chainLog(
                address = LIFECYCLE_ADDRESS,
                topics = listOf(
                    subscription.topic0,
                    topic(Uint256(projectId)),
                    topic(Bytes32(userHash))
                ),
                data = "0x"
            )
        ) as TopazLifecycle.ProjectApproverRemovedEventResponse
        val params = eventParameters(response)

        assertEquals(projectId.toString(), params["projectId"])
        assertEquals("0x000102030405060708090a0b0c0d0e0f101112131415161718191a1b1c1d1e1f", params["userHash"])
    }

    private fun allSubscriptions(): List<TopazEventSubscription> {
        return TopazEventRegistry.subscriptions(
            lifecycleAddress = LIFECYCLE_ADDRESS,
            paymentAddress = PAYMENT_ADDRESS,
            contactsAddress = CONTACTS_ADDRESS,
            workflow = workflow
        )
    }

    private fun chainLog(address: String, topics: List<String>, data: String): Log {
        return Log().apply {
            setAddress(address)
            setTopics(topics)
            setData(data)
            setTransactionHash("0x0000000000000000000000000000000000000000000000000000000000001234")
            setBlockNumber("0x10")
            setLogIndex("0x1")
        }
    }

    private fun topic(value: Type<*>): String {
        return "0x" + TypeEncoder.encode(value)
    }

    private fun data(vararg values: Type<*>): String {
        return FunctionEncoder.encodeConstructor(values.toList())
    }

    private companion object {
        private const val LIFECYCLE_ADDRESS = "0x0000000000000000000000000000000000000001"
        private const val PAYMENT_ADDRESS = "0x0000000000000000000000000000000000000002"
        private const val CONTACTS_ADDRESS = "0x0000000000000000000000000000000000000003"
        private const val DEVELOPER_ADDRESS = "0x1111111111111111111111111111111111111111"
    }
}
