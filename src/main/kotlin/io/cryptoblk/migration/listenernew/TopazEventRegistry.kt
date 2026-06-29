package io.cryptoblk.migration.listenernew

import com.demo.server.epmigration.config.EpChainProperties
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
        LIFECYCLE to ContractSpec(
            name = LIFECYCLE,
            events = listOf(
                event("ProjectCreated", indexed("projectId", "uint256"), field("externalProjectId", "string"), indexed("developerWallet", "address")),
                event("ProjectStatusChanged", indexed("projectId", "uint256"), field("status", "uint8")),
                event("ProjectUpdated", indexed("projectId", "uint256"), field("externalProjectId", "string")),
                event("ProjectApproverRemoved", indexed("projectId", "uint256"), indexed("userHash", "bytes32")),
                event("ClaimCreated", indexed("claimId", "uint256"), indexed("projectId", "uint256"), indexed("contractorWallet", "address"), field("status", "uint8")),
                event("ClaimDocumentsUpdated", indexed("claimId", "uint256"), field("documentCount", "uint256")),
                event("ClaimStatusChanged", indexed("claimId", "uint256"), field("status", "uint8")),
                event("InvoiceCreated", indexed("invoiceId", "uint256"), indexed("claimId", "uint256"), field("status", "uint8")),
                event("InvoiceDocumentsUpdated", indexed("invoiceId", "uint256"), field("documentCount", "uint256")),
                event("InvoiceStatusChanged", indexed("invoiceId", "uint256"), field("status", "uint8")),
                event("PaymentOrderCreated", indexed("paymentOrderId", "uint256"), indexed("invoiceId", "uint256"), field("status", "uint8")),
                event("PaymentOrderStatusChanged", indexed("paymentOrderId", "uint256"), field("status", "uint8")),
                event("PaymentCreatedForOrder", indexed("paymentOrderId", "uint256"), indexed("paymentId", "uint256"), indexed("invoiceId", "uint256")),
                event("BankPaymentRequested", indexed("paymentOrderId", "uint256"), indexed("invoiceId", "uint256"), field("customerRefNumber", "string")),
                event("BankPaymentReferenceRecorded", indexed("paymentOrderId", "uint256"), field("bankPaymentRef", "string")),
                accessControlEvent("RoleAdminChanged"),
                accessControlEvent("RoleGranted"),
                accessControlEvent("RoleRevoked")
            )
        ),
        PAYMENT to ContractSpec(
            name = PAYMENT,
            events = listOf(
                event("PaymentCreated", indexed("paymentId", "uint256"), indexed("paymentOrderId", "uint256"), indexed("invoiceId", "uint256"), field("customerRefNumber", "string"), field("instructedAmountMinor", "uint256"), field("instructedCurrency", "string")),
                event("PaymentAccepted", indexed("paymentId", "uint256"), indexed("paymentOrderId", "uint256"), field("settlementBankRef", "string")),
                event("PaymentRejected", indexed("paymentId", "uint256"), indexed("paymentOrderId", "uint256"), field("rejectCode", "string"), field("rejectReason", "string")),
                event("PaymentReceiptCreated", indexed("paymentReceiptId", "uint256"), indexed("paymentId", "uint256"), indexed("paymentOrderId", "uint256"), field("transactionRefNum", "string")),
                accessControlEvent("RoleAdminChanged"),
                accessControlEvent("RoleGranted"),
                accessControlEvent("RoleRevoked")
            )
        ),
        CONTACTS to ContractSpec(
            name = CONTACTS,
            events = listOf(
                event("ContactUpserted", indexed("contactId", "uint256"), field("party", "string"), field("accountName", "string"), field("contactType", "string"), field("created", "bool"), field("active", "bool")),
                event("ContactDeactivated", indexed("contactId", "uint256"), field("party", "string"), field("accountName", "string")),
                accessControlEvent("RoleAdminChanged"),
                accessControlEvent("RoleGranted"),
                accessControlEvent("RoleRevoked")
            )
        )
    )

    // ---- Public API ----

    /** Builds one subscription per registered event, based on the configured contract addresses. */
    fun subscriptions(
        properties: EpChainProperties,
        workflow: TopazWorkflowService
    ): List<TopazEventSubscription> {
        val addresses = configuredContractAddresses(properties)
        require(addresses.isNotEmpty()) {
            "Configure at least one ep.chain.contract-addresses entry"
        }

        return addresses.flatMap { (contractName, address) ->
            val spec = contractSpecs.getValue(contractName)
            spec.events.map { eventSpec ->
                val event = eventSpec.toWeb3jEvent()
                val handler = handlerFor(contractName, eventSpec.name, workflow)
                TopazEventSubscription(
                    contractName = contractName,
                    contractAddress = normalizeAddress(address),
                    eventName = eventSpec.name,
                    handlerName = handler.name,
                    topic0 = EventEncoder.encode(event),
                    event = event,
                    inputs = eventSpec.inputs,
                    handle = handler.handle
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

    // ---- Contract address resolution ----

    internal fun configuredContractAddresses(properties: EpChainProperties): Map<String, String> {
        val addresses = linkedMapOf<String, String>()

        properties.contractAddresses.forEach { rawName, rawAddress ->
            val address = rawAddress.trim()
            if (address.isBlank()) return@forEach
            val contractName = canonicalContractName(rawName)
            require(contractSpecs.containsKey(contractName)) {
                "Unsupported contract name '$rawName'. Supported names: ${contractSpecs.keys.joinToString()}"
            }
            addresses[contractName] = address
        }

        addresses.forEach { contractName, address ->
            require(ADDRESS_PATTERN.matches(address)) {
                "ep.chain contract address for '$contractName' must be a 20-byte hex address"
            }
        }
        return addresses
    }

    internal fun normalizeAddress(address: String): String {
        return address.trim().toLowerCase(Locale.US)
    }

    private fun canonicalContractName(name: String): String {
        val normalized = name.trim()
            .replace("_", "")
            .replace("-", "")
            .toLowerCase(Locale.US)
        return when (normalized) {
            "lifecycle", "topazlifecycle" -> LIFECYCLE
            "payment", "topazpayment" -> PAYMENT
            "contacts", "topazcontacts" -> CONTACTS
            else -> normalized
        }
    }

    // ---- Event -> workflow handler routing ----

    private fun handlerFor(
        contractName: String,
        eventName: String,
        workflow: TopazWorkflowService
    ): HandlerBinding {
        return when (contractName) {
            LIFECYCLE -> when (eventName) {
                "ProjectCreated" -> bind("onLifecycleProjectCreated", workflow::onLifecycleProjectCreated)
                "ProjectStatusChanged" -> bind("onLifecycleProjectStatusChanged", workflow::onLifecycleProjectStatusChanged)
                "ProjectUpdated" -> bind("onLifecycleProjectUpdated", workflow::onLifecycleProjectUpdated)
                "ProjectApproverRemoved" -> bind("onLifecycleProjectApproverRemoved", workflow::onLifecycleProjectApproverRemoved)
                "ClaimCreated" -> bind("onLifecycleClaimCreated", workflow::onLifecycleClaimCreated)
                "ClaimDocumentsUpdated" -> bind("onLifecycleClaimDocumentsUpdated", workflow::onLifecycleClaimDocumentsUpdated)
                "ClaimStatusChanged" -> bind("onLifecycleClaimStatusChanged", workflow::onLifecycleClaimStatusChanged)
                "InvoiceCreated" -> bind("onLifecycleInvoiceCreated", workflow::onLifecycleInvoiceCreated)
                "InvoiceDocumentsUpdated" -> bind("onLifecycleInvoiceDocumentsUpdated", workflow::onLifecycleInvoiceDocumentsUpdated)
                "InvoiceStatusChanged" -> bind("onLifecycleInvoiceStatusChanged", workflow::onLifecycleInvoiceStatusChanged)
                "PaymentOrderCreated" -> bind("onLifecyclePaymentOrderCreated", workflow::onLifecyclePaymentOrderCreated)
                "PaymentOrderStatusChanged" -> bind("onLifecyclePaymentOrderStatusChanged", workflow::onLifecyclePaymentOrderStatusChanged)
                "PaymentCreatedForOrder" -> bind("onLifecyclePaymentCreatedForOrder", workflow::onLifecyclePaymentCreatedForOrder)
                "BankPaymentRequested" -> bind("onLifecycleBankPaymentRequested", workflow::onLifecycleBankPaymentRequested)
                "BankPaymentReferenceRecorded" -> bind("onLifecycleBankPaymentReferenceRecorded", workflow::onLifecycleBankPaymentReferenceRecorded)
                "RoleAdminChanged" -> bind("onLifecycleRoleAdminChanged", workflow::onLifecycleRoleAdminChanged)
                "RoleGranted" -> bind("onLifecycleRoleGranted", workflow::onLifecycleRoleGranted)
                "RoleRevoked" -> bind("onLifecycleRoleRevoked", workflow::onLifecycleRoleRevoked)
                else -> error("No workflow handler for $contractName.$eventName")
            }
            PAYMENT -> when (eventName) {
                "PaymentCreated" -> bind("onPaymentPaymentCreated", workflow::onPaymentPaymentCreated)
                "PaymentAccepted" -> bind("onPaymentPaymentAccepted", workflow::onPaymentPaymentAccepted)
                "PaymentRejected" -> bind("onPaymentPaymentRejected", workflow::onPaymentPaymentRejected)
                "PaymentReceiptCreated" -> bind("onPaymentPaymentReceiptCreated", workflow::onPaymentPaymentReceiptCreated)
                "RoleAdminChanged" -> bind("onPaymentRoleAdminChanged", workflow::onPaymentRoleAdminChanged)
                "RoleGranted" -> bind("onPaymentRoleGranted", workflow::onPaymentRoleGranted)
                "RoleRevoked" -> bind("onPaymentRoleRevoked", workflow::onPaymentRoleRevoked)
                else -> error("No workflow handler for $contractName.$eventName")
            }
            CONTACTS -> when (eventName) {
                "ContactUpserted" -> bind("onContactsContactUpserted", workflow::onContactsContactUpserted)
                "ContactDeactivated" -> bind("onContactsContactDeactivated", workflow::onContactsContactDeactivated)
                "RoleAdminChanged" -> bind("onContactsRoleAdminChanged", workflow::onContactsRoleAdminChanged)
                "RoleGranted" -> bind("onContactsRoleGranted", workflow::onContactsRoleGranted)
                "RoleRevoked" -> bind("onContactsRoleRevoked", workflow::onContactsRoleRevoked)
                else -> error("No workflow handler for $contractName.$eventName")
            }
            else -> error("Unsupported contract '$contractName'")
        }
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
        val name: String,
        val handle: (TopazDecodedEvent) -> Unit
    )

    // ---- Small DSL for declarations ----

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

    private fun bind(name: String, handle: (TopazDecodedEvent) -> Unit): HandlerBinding {
        return HandlerBinding(name, handle)
    }
}
