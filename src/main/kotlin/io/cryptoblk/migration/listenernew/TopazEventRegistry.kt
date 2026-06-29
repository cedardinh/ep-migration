package io.cryptoblk.migration.listenernew

import org.web3j.abi.EventEncoder
import org.web3j.abi.FunctionReturnDecoder
import org.web3j.abi.TypeReference
import org.web3j.abi.datatypes.Event
import org.web3j.abi.datatypes.Type
import org.web3j.protocol.core.methods.response.Log
import org.web3j.utils.Numeric
import java.util.Locale

// ---------------------------------------------------------------------------
// Domain model
// ---------------------------------------------------------------------------

/** Describes a single ABI event parameter: name, type, and whether it is indexed. */
data class TopazEventInput(
    val name: String,
    val type: String,
    val indexed: Boolean
)

/** A decoded field: a [TopazEventInput] enriched with its actual value. */
data class TopazEventField(
    val name: String,
    val type: String,
    val indexed: Boolean,
    val value: Any?
)

/** A fully decoded event from a single on-chain log. */
data class TopazDecodedEvent(
    val contractName: String,
    val contractAddress: String,
    val eventName: String,
    val topic0: String,
    val fields: List<TopazEventField>,
    val log: Log
) {
    val values: Map<String, Any?> = fields.associate { it.name to it.value }
}

/** A listenable subscription: contract + event + handler, plus the topic0 used for routing. */
data class TopazEventSubscription(
    val contractName: String,
    val contractAddress: String,
    val eventName: String,
    val handlerName: String,
    val topic0: String,
    val event: Event,
    val inputs: List<TopazEventInput>,
    val handle: (TopazDecodedEvent) -> Unit
)

/**
 * Event registry: declares which events each contract emits, and uses that to
 * build subscriptions and decode on-chain logs.
 */
object TopazEventRegistry {

    // ---- Supported contracts ----
    private const val LIFECYCLE = "lifecycle"
    private const val PAYMENT = "payment"
    private const val CONTACTS = "contacts"

    private val ADDRESS_PATTERN = Regex("^0x[0-9a-fA-F]{40}$")

    // ---- Event registry table: which events each contract emits ----
    private val contractSpecs: Map<String, ContractSpec> = linkedMapOf(
        LIFECYCLE to ContractSpec(name = LIFECYCLE, events = lifecycleEvents()),
        PAYMENT to ContractSpec(name = PAYMENT, events = paymentEvents()),
        CONTACTS to ContractSpec(name = CONTACTS, events = contactEvents())
    )

    // ---- Public API ----

    /** Builds one subscription per registered event for the three Topaz contracts. */
    fun subscriptions(
        lifecycleAddress: String,
        paymentAddress: String,
        contactsAddress: String,
        workflow: TopazWorkflowService
    ): List<TopazEventSubscription> {
        val addresses = linkedMapOf(
            LIFECYCLE to lifecycleAddress,
            PAYMENT to paymentAddress,
            CONTACTS to contactsAddress
        )

        return addresses.flatMap { (contractName, rawAddress) ->
            val address = rawAddress.trim()
            require(ADDRESS_PATTERN.matches(address)) {
                "ep.chain contract address for '$contractName' must be a 20-byte hex address"
            }
            contractSpecs.getValue(contractName).events.map { eventSpec ->
                val event = eventSpec.toWeb3jEvent()
                TopazEventSubscription(
                    contractName = contractName,
                    contractAddress = normalizeAddress(address),
                    eventName = eventSpec.name,
                    handlerName = handlerNameFor(contractName, eventSpec.name),
                    topic0 = EventEncoder.encode(event),
                    event = event,
                    inputs = eventSpec.inputs,
                    handle = handlerFor(contractName, eventSpec.name, workflow).handle
                )
            }
        }
    }

    /** Decodes an on-chain log that matched a subscription into a [TopazDecodedEvent]. */
    fun decode(subscription: TopazEventSubscription, chainLog: Log): TopazDecodedEvent {
        val topics = chainLog.topics ?: emptyList()
        val indexedInputs = subscription.inputs.filter { it.indexed }
        require(topics.size >= indexedInputs.size + 1) {
            "Log for ${subscription.contractName}.${subscription.eventName} has ${topics.size} topics, expected ${indexedInputs.size + 1}"
        }

        val indexedValues = subscription.event.indexedParameters.mapIndexed { index, typeReference ->
            FunctionReturnDecoder.decodeIndexedValue(topics[index + 1], typeReference)
        }
        val nonIndexedValues = FunctionReturnDecoder.decode(
            chainLog.data ?: "0x",
            subscription.event.nonIndexedParameters
        )

        var indexedIndex = 0
        var nonIndexedIndex = 0
        val fields = subscription.inputs.map { input ->
            val decodedValue = if (input.indexed) {
                indexedValues[indexedIndex++]
            } else {
                nonIndexedValues[nonIndexedIndex++]
            }
            TopazEventField(
                name = input.name,
                type = input.type,
                indexed = input.indexed,
                value = toPlainValue(decodedValue)
            )
        }

        return TopazDecodedEvent(
            contractName = subscription.contractName,
            contractAddress = subscription.contractAddress,
            eventName = subscription.eventName,
            topic0 = subscription.topic0,
            fields = fields,
            log = chainLog
        )
    }

    internal fun normalizeAddress(address: String): String {
        return address.trim().toLowerCase(Locale.US)
    }

    // ---- Event -> workflow handler routing ----

    private fun handlerFor(
        contractName: String,
        eventName: String,
        workflow: TopazWorkflowService
    ): HandlerBinding {
        return when (contractName) {
            LIFECYCLE -> when (eventName) {
                "ProjectCreated" -> bind(workflow::onLifecycleProjectCreated)
                "ProjectStatusChanged" -> bind(workflow::onLifecycleProjectStatusChanged)
                "ProjectUpdated" -> bind(workflow::onLifecycleProjectUpdated)
                "ProjectApproverRemoved" -> bind(workflow::onLifecycleProjectApproverRemoved)
                "ClaimCreated" -> bind(workflow::onLifecycleClaimCreated)
                "ClaimDocumentsUpdated" -> bind(workflow::onLifecycleClaimDocumentsUpdated)
                "ClaimStatusChanged" -> bind(workflow::onLifecycleClaimStatusChanged)
                "InvoiceCreated" -> bind(workflow::onLifecycleInvoiceCreated)
                "InvoiceDocumentsUpdated" -> bind(workflow::onLifecycleInvoiceDocumentsUpdated)
                "InvoiceStatusChanged" -> bind(workflow::onLifecycleInvoiceStatusChanged)
                "PaymentOrderCreated" -> bind(workflow::onLifecyclePaymentOrderCreated)
                "PaymentOrderStatusChanged" -> bind(workflow::onLifecyclePaymentOrderStatusChanged)
                "PaymentCreatedForOrder" -> bind(workflow::onLifecyclePaymentCreatedForOrder)
                "BankPaymentRequested" -> bind(workflow::onLifecycleBankPaymentRequested)
                "BankPaymentReferenceRecorded" -> bind(workflow::onLifecycleBankPaymentReferenceRecorded)
                "RoleAdminChanged" -> bind(workflow::onLifecycleRoleAdminChanged)
                "RoleGranted" -> bind(workflow::onLifecycleRoleGranted)
                "RoleRevoked" -> bind(workflow::onLifecycleRoleRevoked)
                else -> error("No workflow handler for $contractName.$eventName")
            }
            PAYMENT -> when (eventName) {
                "PaymentCreated" -> bind(workflow::onPaymentPaymentCreated)
                "PaymentAccepted" -> bind(workflow::onPaymentPaymentAccepted)
                "PaymentRejected" -> bind(workflow::onPaymentPaymentRejected)
                "PaymentReceiptCreated" -> bind(workflow::onPaymentPaymentReceiptCreated)
                "RoleAdminChanged" -> bind(workflow::onPaymentRoleAdminChanged)
                "RoleGranted" -> bind(workflow::onPaymentRoleGranted)
                "RoleRevoked" -> bind(workflow::onPaymentRoleRevoked)
                else -> error("No workflow handler for $contractName.$eventName")
            }
            CONTACTS -> when (eventName) {
                "ContactUpserted" -> bind(workflow::onContactsContactUpserted)
                "ContactDeactivated" -> bind(workflow::onContactsContactDeactivated)
                "RoleAdminChanged" -> bind(workflow::onContactsRoleAdminChanged)
                "RoleGranted" -> bind(workflow::onContactsRoleGranted)
                "RoleRevoked" -> bind(workflow::onContactsRoleRevoked)
                else -> error("No workflow handler for $contractName.$eventName")
            }
            else -> error("Unsupported contract '$contractName'")
        }
    }

    private fun handlerNameFor(contractName: String, eventName: String): String {
        val contractPrefix = when (contractName) {
            LIFECYCLE -> "Lifecycle"
            PAYMENT -> "Payment"
            CONTACTS -> "Contacts"
            else -> error("Unsupported contract '$contractName'")
        }
        return "on$contractPrefix$eventName"
    }

    // ---- Decoding helpers ----

    private fun toPlainValue(value: Type<*>): Any? {
        return when (val raw = value.value) {
            is ByteArray -> Numeric.toHexString(raw)
            is Array<*> -> raw.map { it }
            else -> raw
        }
    }

    // ---- Internal model ----

    private data class ContractSpec(
        val name: String,
        val events: List<EventSpec>
    )

    private data class EventSpec(
        val name: String,
        val inputs: List<TopazEventInput>
    ) {
        fun toWeb3jEvent(): Event {
            return Event(
                name,
                inputs.map { TypeReference.makeTypeReference(it.type, it.indexed, false) }
            )
        }
    }

    private data class HandlerBinding(
        val handle: (TopazDecodedEvent) -> Unit
    )

    // ---- Small DSL for declarations ----

    private fun lifecycleEvents(): List<EventSpec> {
        return listOf(
            event(
                "ProjectCreated",
                indexed("projectId", "uint256"),
                field("externalProjectId", "string"),
                indexed("developerWallet", "address")
            ),
            event(
                "ProjectStatusChanged",
                indexed("projectId", "uint256"),
                field("status", "uint8")
            ),
            event(
                "ProjectUpdated",
                indexed("projectId", "uint256"),
                field("externalProjectId", "string")
            ),
            event(
                "ProjectApproverRemoved",
                indexed("projectId", "uint256"),
                indexed("userHash", "bytes32")
            ),
            event(
                "ClaimCreated",
                indexed("claimId", "uint256"),
                indexed("projectId", "uint256"),
                indexed("contractorWallet", "address"),
                field("status", "uint8")
            ),
            event(
                "ClaimDocumentsUpdated",
                indexed("claimId", "uint256"),
                field("documentCount", "uint256")
            ),
            event(
                "ClaimStatusChanged",
                indexed("claimId", "uint256"),
                field("status", "uint8")
            ),
            event(
                "InvoiceCreated",
                indexed("invoiceId", "uint256"),
                indexed("claimId", "uint256"),
                field("status", "uint8")
            ),
            event(
                "InvoiceDocumentsUpdated",
                indexed("invoiceId", "uint256"),
                field("documentCount", "uint256")
            ),
            event(
                "InvoiceStatusChanged",
                indexed("invoiceId", "uint256"),
                field("status", "uint8")
            ),
            event(
                "PaymentOrderCreated",
                indexed("paymentOrderId", "uint256"),
                indexed("invoiceId", "uint256"),
                field("status", "uint8")
            ),
            event(
                "PaymentOrderStatusChanged",
                indexed("paymentOrderId", "uint256"),
                field("status", "uint8")
            ),
            event(
                "PaymentCreatedForOrder",
                indexed("paymentOrderId", "uint256"),
                indexed("paymentId", "uint256"),
                indexed("invoiceId", "uint256")
            ),
            event(
                "BankPaymentRequested",
                indexed("paymentOrderId", "uint256"),
                indexed("invoiceId", "uint256"),
                field("customerRefNumber", "string")
            ),
            event(
                "BankPaymentReferenceRecorded",
                indexed("paymentOrderId", "uint256"),
                field("bankPaymentRef", "string")
            )
        ) + accessControlEvents()
    }

    private fun paymentEvents(): List<EventSpec> {
        return listOf(
            event(
                "PaymentCreated",
                indexed("paymentId", "uint256"),
                indexed("paymentOrderId", "uint256"),
                indexed("invoiceId", "uint256"),
                field("customerRefNumber", "string"),
                field("instructedAmountMinor", "uint256"),
                field("instructedCurrency", "string")
            ),
            event(
                "PaymentAccepted",
                indexed("paymentId", "uint256"),
                indexed("paymentOrderId", "uint256"),
                field("settlementBankRef", "string")
            ),
            event(
                "PaymentRejected",
                indexed("paymentId", "uint256"),
                indexed("paymentOrderId", "uint256"),
                field("rejectCode", "string"),
                field("rejectReason", "string")
            ),
            event(
                "PaymentReceiptCreated",
                indexed("paymentReceiptId", "uint256"),
                indexed("paymentId", "uint256"),
                indexed("paymentOrderId", "uint256"),
                field("transactionRefNum", "string")
            )
        ) + accessControlEvents()
    }

    private fun contactEvents(): List<EventSpec> {
        return listOf(
            event(
                "ContactUpserted",
                indexed("contactId", "uint256"),
                field("party", "string"),
                field("accountName", "string"),
                field("contactType", "string"),
                field("created", "bool"),
                field("active", "bool")
            ),
            event(
                "ContactDeactivated",
                indexed("contactId", "uint256"),
                field("party", "string"),
                field("accountName", "string")
            )
        ) + accessControlEvents()
    }

    private fun accessControlEvents(): List<EventSpec> {
        return listOf(
            accessControlEvent("RoleAdminChanged"),
            accessControlEvent("RoleGranted"),
            accessControlEvent("RoleRevoked")
        )
    }

    private fun event(name: String, vararg inputs: TopazEventInput): EventSpec {
        return EventSpec(name, inputs.toList())
    }

    private fun accessControlEvent(name: String): EventSpec {
        return when (name) {
            "RoleAdminChanged" -> event(
                name,
                indexed("role", "bytes32"),
                indexed("previousAdminRole", "bytes32"),
                indexed("newAdminRole", "bytes32")
            )
            "RoleGranted", "RoleRevoked" -> event(
                name,
                indexed("role", "bytes32"),
                indexed("account", "address"),
                indexed("sender", "address")
            )
            else -> error("Unsupported access-control event '$name'")
        }
    }

    private fun indexed(name: String, type: String): TopazEventInput {
        return TopazEventInput(name = name, type = type, indexed = true)
    }

    private fun field(name: String, type: String): TopazEventInput {
        return TopazEventInput(name = name, type = type, indexed = false)
    }

    private fun bind(handle: (TopazDecodedEvent) -> Unit): HandlerBinding {
        return HandlerBinding(handle)
    }
}
