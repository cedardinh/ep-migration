package io.cryptoblk.migration.listenernew

import org.web3j.abi.EventEncoder
import org.web3j.abi.FunctionReturnDecoder
import org.web3j.abi.TypeReference
import org.web3j.abi.datatypes.Event
import org.web3j.abi.datatypes.Type
import org.web3j.protocol.core.methods.response.Log
import org.web3j.utils.Numeric
import java.math.BigInteger
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
    val params: TopazEventParams,
    val fields: List<TopazEventField>,
    val log: Log
)

/** A listenable subscription: contract + event + handler, plus the topic0 used for routing. */
data class TopazEventSubscription(
    val contractName: String,
    val contractAddress: String,
    val eventName: String,
    val handlerName: String,
    val topic0: String,
    val event: Event,
    val inputs: List<TopazEventInput>,
    val buildParams: (TopazEventValues) -> TopazEventParams,
    val handle: (TopazDecodedEvent) -> Unit
)

class TopazEventValues internal constructor(fields: List<TopazEventField>) {
    private val values = fields.associate { it.name to it.value }

    fun string(name: String): String {
        val value = requiredValue(name)
        if (value is String) return value
        typeError(name, value, "String")
    }

    fun bigInteger(name: String): BigInteger {
        val value = requiredValue(name)
        return when (value) {
            is BigInteger -> value
            is Number -> BigInteger.valueOf(value.toLong())
            is String -> runCatching { BigInteger(value) }.getOrElse { typeError(name, value, "BigInteger") }
            else -> typeError(name, value, "BigInteger")
        }
    }

    fun int(name: String): Int {
        return bigInteger(name).toInt()
    }

    fun boolean(name: String): Boolean {
        val value = requiredValue(name)
        if (value is Boolean) return value
        typeError(name, value, "Boolean")
    }

    private fun requiredValue(name: String): Any {
        return values[name] ?: error("Decoded event does not contain parameter '$name'")
    }

    private fun typeError(name: String, value: Any, expected: String): Nothing {
        error("Decoded event parameter '$name' is ${value::class.java.simpleName}, expected $expected")
    }
}

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
                val binding = handlerFor(contractName, eventSpec.name, workflow)
                TopazEventSubscription(
                    contractName = contractName,
                    contractAddress = normalizeAddress(address),
                    eventName = eventSpec.name,
                    handlerName = handlerNameFor(contractName, eventSpec.name),
                    topic0 = EventEncoder.encode(event),
                    event = event,
                    inputs = eventSpec.inputs,
                    buildParams = binding.buildParams,
                    handle = binding.handle
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
        val params = subscription.buildParams(TopazEventValues(fields))

        return TopazDecodedEvent(
            contractName = subscription.contractName,
            contractAddress = subscription.contractAddress,
            eventName = subscription.eventName,
            topic0 = subscription.topic0,
            params = params,
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
                "ProjectCreated" -> bind(::lifecycleProjectCreatedParams, workflow::onLifecycleProjectCreated)
                "ProjectStatusChanged" -> bind(::lifecycleProjectStatusChangedParams, workflow::onLifecycleProjectStatusChanged)
                "ProjectUpdated" -> bind(::lifecycleProjectUpdatedParams, workflow::onLifecycleProjectUpdated)
                "ProjectApproverRemoved" -> bind(::lifecycleProjectApproverRemovedParams, workflow::onLifecycleProjectApproverRemoved)
                "ClaimCreated" -> bind(::lifecycleClaimCreatedParams, workflow::onLifecycleClaimCreated)
                "ClaimDocumentsUpdated" -> bind(::lifecycleClaimDocumentsUpdatedParams, workflow::onLifecycleClaimDocumentsUpdated)
                "ClaimStatusChanged" -> bind(::lifecycleClaimStatusChangedParams, workflow::onLifecycleClaimStatusChanged)
                "InvoiceCreated" -> bind(::lifecycleInvoiceCreatedParams, workflow::onLifecycleInvoiceCreated)
                "InvoiceDocumentsUpdated" -> bind(::lifecycleInvoiceDocumentsUpdatedParams, workflow::onLifecycleInvoiceDocumentsUpdated)
                "InvoiceStatusChanged" -> bind(::lifecycleInvoiceStatusChangedParams, workflow::onLifecycleInvoiceStatusChanged)
                "PaymentOrderCreated" -> bind(::lifecyclePaymentOrderCreatedParams, workflow::onLifecyclePaymentOrderCreated)
                "PaymentOrderStatusChanged" -> bind(::lifecyclePaymentOrderStatusChangedParams, workflow::onLifecyclePaymentOrderStatusChanged)
                "PaymentCreatedForOrder" -> bind(::lifecyclePaymentCreatedForOrderParams, workflow::onLifecyclePaymentCreatedForOrder)
                "BankPaymentRequested" -> bind(::lifecycleBankPaymentRequestedParams, workflow::onLifecycleBankPaymentRequested)
                "BankPaymentReferenceRecorded" -> bind(::lifecycleBankPaymentReferenceRecordedParams, workflow::onLifecycleBankPaymentReferenceRecorded)
                "RoleAdminChanged" -> bind(::lifecycleRoleAdminChangedParams, workflow::onLifecycleRoleAdminChanged)
                "RoleGranted" -> bind(::lifecycleRoleGrantedParams, workflow::onLifecycleRoleGranted)
                "RoleRevoked" -> bind(::lifecycleRoleRevokedParams, workflow::onLifecycleRoleRevoked)
                else -> error("No workflow handler for $contractName.$eventName")
            }
            PAYMENT -> when (eventName) {
                "PaymentCreated" -> bind(::paymentPaymentCreatedParams, workflow::onPaymentPaymentCreated)
                "PaymentAccepted" -> bind(::paymentPaymentAcceptedParams, workflow::onPaymentPaymentAccepted)
                "PaymentRejected" -> bind(::paymentPaymentRejectedParams, workflow::onPaymentPaymentRejected)
                "PaymentReceiptCreated" -> bind(::paymentPaymentReceiptCreatedParams, workflow::onPaymentPaymentReceiptCreated)
                "RoleAdminChanged" -> bind(::paymentRoleAdminChangedParams, workflow::onPaymentRoleAdminChanged)
                "RoleGranted" -> bind(::paymentRoleGrantedParams, workflow::onPaymentRoleGranted)
                "RoleRevoked" -> bind(::paymentRoleRevokedParams, workflow::onPaymentRoleRevoked)
                else -> error("No workflow handler for $contractName.$eventName")
            }
            CONTACTS -> when (eventName) {
                "ContactUpserted" -> bind(::contactsContactUpsertedParams, workflow::onContactsContactUpserted)
                "ContactDeactivated" -> bind(::contactsContactDeactivatedParams, workflow::onContactsContactDeactivated)
                "RoleAdminChanged" -> bind(::contactsRoleAdminChangedParams, workflow::onContactsRoleAdminChanged)
                "RoleGranted" -> bind(::contactsRoleGrantedParams, workflow::onContactsRoleGranted)
                "RoleRevoked" -> bind(::contactsRoleRevokedParams, workflow::onContactsRoleRevoked)
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

    // ---- Event parameter mapping ----

    private fun lifecycleProjectCreatedParams(values: TopazEventValues): TopazEventParams.LifecycleProjectCreated {
        return TopazEventParams.LifecycleProjectCreated(
            projectId = values.bigInteger("projectId"),
            externalProjectId = values.string("externalProjectId"),
            developerWallet = values.string("developerWallet")
        )
    }

    private fun lifecycleProjectStatusChangedParams(values: TopazEventValues): TopazEventParams.LifecycleProjectStatusChanged {
        return TopazEventParams.LifecycleProjectStatusChanged(
            projectId = values.bigInteger("projectId"),
            status = values.int("status")
        )
    }

    private fun lifecycleProjectUpdatedParams(values: TopazEventValues): TopazEventParams.LifecycleProjectUpdated {
        return TopazEventParams.LifecycleProjectUpdated(
            projectId = values.bigInteger("projectId"),
            externalProjectId = values.string("externalProjectId")
        )
    }

    private fun lifecycleProjectApproverRemovedParams(values: TopazEventValues): TopazEventParams.LifecycleProjectApproverRemoved {
        return TopazEventParams.LifecycleProjectApproverRemoved(
            projectId = values.bigInteger("projectId"),
            userHash = values.string("userHash")
        )
    }

    private fun lifecycleClaimCreatedParams(values: TopazEventValues): TopazEventParams.LifecycleClaimCreated {
        return TopazEventParams.LifecycleClaimCreated(
            claimId = values.bigInteger("claimId"),
            projectId = values.bigInteger("projectId"),
            contractorWallet = values.string("contractorWallet"),
            status = values.int("status")
        )
    }

    private fun lifecycleClaimDocumentsUpdatedParams(values: TopazEventValues): TopazEventParams.LifecycleClaimDocumentsUpdated {
        return TopazEventParams.LifecycleClaimDocumentsUpdated(
            claimId = values.bigInteger("claimId"),
            documentCount = values.bigInteger("documentCount")
        )
    }

    private fun lifecycleClaimStatusChangedParams(values: TopazEventValues): TopazEventParams.LifecycleClaimStatusChanged {
        return TopazEventParams.LifecycleClaimStatusChanged(
            claimId = values.bigInteger("claimId"),
            status = values.int("status")
        )
    }

    private fun lifecycleInvoiceCreatedParams(values: TopazEventValues): TopazEventParams.LifecycleInvoiceCreated {
        return TopazEventParams.LifecycleInvoiceCreated(
            invoiceId = values.bigInteger("invoiceId"),
            claimId = values.bigInteger("claimId"),
            status = values.int("status")
        )
    }

    private fun lifecycleInvoiceDocumentsUpdatedParams(values: TopazEventValues): TopazEventParams.LifecycleInvoiceDocumentsUpdated {
        return TopazEventParams.LifecycleInvoiceDocumentsUpdated(
            invoiceId = values.bigInteger("invoiceId"),
            documentCount = values.bigInteger("documentCount")
        )
    }

    private fun lifecycleInvoiceStatusChangedParams(values: TopazEventValues): TopazEventParams.LifecycleInvoiceStatusChanged {
        return TopazEventParams.LifecycleInvoiceStatusChanged(
            invoiceId = values.bigInteger("invoiceId"),
            status = values.int("status")
        )
    }

    private fun lifecyclePaymentOrderCreatedParams(values: TopazEventValues): TopazEventParams.LifecyclePaymentOrderCreated {
        return TopazEventParams.LifecyclePaymentOrderCreated(
            paymentOrderId = values.bigInteger("paymentOrderId"),
            invoiceId = values.bigInteger("invoiceId"),
            status = values.int("status")
        )
    }

    private fun lifecyclePaymentOrderStatusChangedParams(values: TopazEventValues): TopazEventParams.LifecyclePaymentOrderStatusChanged {
        return TopazEventParams.LifecyclePaymentOrderStatusChanged(
            paymentOrderId = values.bigInteger("paymentOrderId"),
            status = values.int("status")
        )
    }

    private fun lifecyclePaymentCreatedForOrderParams(values: TopazEventValues): TopazEventParams.LifecyclePaymentCreatedForOrder {
        return TopazEventParams.LifecyclePaymentCreatedForOrder(
            paymentOrderId = values.bigInteger("paymentOrderId"),
            paymentId = values.bigInteger("paymentId"),
            invoiceId = values.bigInteger("invoiceId")
        )
    }

    private fun lifecycleBankPaymentRequestedParams(values: TopazEventValues): TopazEventParams.LifecycleBankPaymentRequested {
        return TopazEventParams.LifecycleBankPaymentRequested(
            paymentOrderId = values.bigInteger("paymentOrderId"),
            invoiceId = values.bigInteger("invoiceId"),
            customerRefNumber = values.string("customerRefNumber")
        )
    }

    private fun lifecycleBankPaymentReferenceRecordedParams(values: TopazEventValues): TopazEventParams.LifecycleBankPaymentReferenceRecorded {
        return TopazEventParams.LifecycleBankPaymentReferenceRecorded(
            paymentOrderId = values.bigInteger("paymentOrderId"),
            bankPaymentRef = values.string("bankPaymentRef")
        )
    }

    private fun lifecycleRoleAdminChangedParams(values: TopazEventValues): TopazEventParams.LifecycleRoleAdminChanged {
        return TopazEventParams.LifecycleRoleAdminChanged(
            role = values.string("role"),
            previousAdminRole = values.string("previousAdminRole"),
            newAdminRole = values.string("newAdminRole")
        )
    }

    private fun lifecycleRoleGrantedParams(values: TopazEventValues): TopazEventParams.LifecycleRoleGranted {
        return TopazEventParams.LifecycleRoleGranted(
            role = values.string("role"),
            account = values.string("account"),
            sender = values.string("sender")
        )
    }

    private fun lifecycleRoleRevokedParams(values: TopazEventValues): TopazEventParams.LifecycleRoleRevoked {
        return TopazEventParams.LifecycleRoleRevoked(
            role = values.string("role"),
            account = values.string("account"),
            sender = values.string("sender")
        )
    }

    private fun paymentPaymentCreatedParams(values: TopazEventValues): TopazEventParams.PaymentPaymentCreated {
        return TopazEventParams.PaymentPaymentCreated(
            paymentId = values.bigInteger("paymentId"),
            paymentOrderId = values.bigInteger("paymentOrderId"),
            invoiceId = values.bigInteger("invoiceId"),
            customerRefNumber = values.string("customerRefNumber"),
            instructedAmountMinor = values.bigInteger("instructedAmountMinor"),
            instructedCurrency = values.string("instructedCurrency")
        )
    }

    private fun paymentPaymentAcceptedParams(values: TopazEventValues): TopazEventParams.PaymentPaymentAccepted {
        return TopazEventParams.PaymentPaymentAccepted(
            paymentId = values.bigInteger("paymentId"),
            paymentOrderId = values.bigInteger("paymentOrderId"),
            settlementBankRef = values.string("settlementBankRef")
        )
    }

    private fun paymentPaymentRejectedParams(values: TopazEventValues): TopazEventParams.PaymentPaymentRejected {
        return TopazEventParams.PaymentPaymentRejected(
            paymentId = values.bigInteger("paymentId"),
            paymentOrderId = values.bigInteger("paymentOrderId"),
            rejectCode = values.string("rejectCode"),
            rejectReason = values.string("rejectReason")
        )
    }

    private fun paymentPaymentReceiptCreatedParams(values: TopazEventValues): TopazEventParams.PaymentPaymentReceiptCreated {
        return TopazEventParams.PaymentPaymentReceiptCreated(
            paymentReceiptId = values.bigInteger("paymentReceiptId"),
            paymentId = values.bigInteger("paymentId"),
            paymentOrderId = values.bigInteger("paymentOrderId"),
            transactionRefNum = values.string("transactionRefNum")
        )
    }

    private fun paymentRoleAdminChangedParams(values: TopazEventValues): TopazEventParams.PaymentRoleAdminChanged {
        return TopazEventParams.PaymentRoleAdminChanged(
            role = values.string("role"),
            previousAdminRole = values.string("previousAdminRole"),
            newAdminRole = values.string("newAdminRole")
        )
    }

    private fun paymentRoleGrantedParams(values: TopazEventValues): TopazEventParams.PaymentRoleGranted {
        return TopazEventParams.PaymentRoleGranted(
            role = values.string("role"),
            account = values.string("account"),
            sender = values.string("sender")
        )
    }

    private fun paymentRoleRevokedParams(values: TopazEventValues): TopazEventParams.PaymentRoleRevoked {
        return TopazEventParams.PaymentRoleRevoked(
            role = values.string("role"),
            account = values.string("account"),
            sender = values.string("sender")
        )
    }

    private fun contactsContactUpsertedParams(values: TopazEventValues): TopazEventParams.ContactsContactUpserted {
        return TopazEventParams.ContactsContactUpserted(
            contactId = values.bigInteger("contactId"),
            wallet = values.string("wallet"),
            party = values.string("party"),
            accountName = values.string("accountName"),
            contactType = values.string("contactType"),
            created = values.boolean("created"),
            active = values.boolean("active")
        )
    }

    private fun contactsContactDeactivatedParams(values: TopazEventValues): TopazEventParams.ContactsContactDeactivated {
        return TopazEventParams.ContactsContactDeactivated(
            contactId = values.bigInteger("contactId"),
            wallet = values.string("wallet"),
            party = values.string("party"),
            accountName = values.string("accountName")
        )
    }

    private fun contactsRoleAdminChangedParams(values: TopazEventValues): TopazEventParams.ContactsRoleAdminChanged {
        return TopazEventParams.ContactsRoleAdminChanged(
            role = values.string("role"),
            previousAdminRole = values.string("previousAdminRole"),
            newAdminRole = values.string("newAdminRole")
        )
    }

    private fun contactsRoleGrantedParams(values: TopazEventValues): TopazEventParams.ContactsRoleGranted {
        return TopazEventParams.ContactsRoleGranted(
            role = values.string("role"),
            account = values.string("account"),
            sender = values.string("sender")
        )
    }

    private fun contactsRoleRevokedParams(values: TopazEventValues): TopazEventParams.ContactsRoleRevoked {
        return TopazEventParams.ContactsRoleRevoked(
            role = values.string("role"),
            account = values.string("account"),
            sender = values.string("sender")
        )
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
        val buildParams: (TopazEventValues) -> TopazEventParams,
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
                indexed("wallet", "address"),
                field("party", "string"),
                field("accountName", "string"),
                field("contactType", "string"),
                field("created", "bool"),
                field("active", "bool")
            ),
            event(
                "ContactDeactivated",
                indexed("contactId", "uint256"),
                indexed("wallet", "address"),
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

    @Suppress("UNCHECKED_CAST")
    private fun <P : TopazEventParams> bind(
        buildParams: (TopazEventValues) -> P,
        handle: (TopazDecodedEvent, P) -> Unit
    ): HandlerBinding {
        return HandlerBinding(
            buildParams = buildParams,
            handle = { event -> handle(event, event.params as P) }
        )
    }
}
