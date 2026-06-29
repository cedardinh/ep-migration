package io.cryptoblk.migration.listenernew

import org.web3j.abi.EventEncoder
import org.web3j.abi.TypeReference
import org.web3j.abi.datatypes.Event

/**
 * Declares the Topaz events this service listens to and builds web3j subscriptions.
 */
object TopazEventRegistry {

    // ---- Public API ----

    /** Builds one subscription per registered event, based on the configured contract addresses. */
    fun subscriptions(
        addresses: TopazContractAddresses,
        workflow: TopazWorkflowService
    ): List<TopazEventSubscription> {
        return contractSpecs(addresses, workflow).flatMap { contract ->
            contract.events.map { registration ->
                val event = registration.event.toWeb3jEvent()
                TopazEventSubscription(
                    contractName = contract.name,
                    contractAddress = TopazContractAddresses.normalize(contract.address),
                    eventName = registration.event.name,
                    topic0 = EventEncoder.encode(event),
                    event = event,
                    inputs = registration.event.inputs,
                    handle = registration.handler
                )
            }
        }
    }

    // ---- Event declarations ----

    private fun contractSpecs(addresses: TopazContractAddresses, workflow: TopazWorkflowService): List<ContractSpec> {
        val contracts = mutableListOf<ContractSpec>()

        if (addresses.lifecycle.isNotBlank()) {
            contracts += ContractSpec(
                name = TopazContractAddresses.LIFECYCLE,
                address = addresses.lifecycle,
                events = lifecycleEvents(workflow)
            )
        }

        if (addresses.payment.isNotBlank()) {
            contracts += ContractSpec(
                name = TopazContractAddresses.PAYMENT,
                address = addresses.payment,
                events = paymentEvents(workflow)
            )
        }

        if (addresses.contacts.isNotBlank()) {
            contracts += ContractSpec(
                name = TopazContractAddresses.CONTACTS,
                address = addresses.contacts,
                events = contactEvents(workflow)
            )
        }

        return contracts
    }

    private fun lifecycleEvents(workflow: TopazWorkflowService): List<EventRegistration> {
        return listOf(
            handledEvent(
                "ProjectCreated",
                workflow::onLifecycleProjectCreated,
                indexed("projectId", "uint256"),
                field("externalProjectId", "string"),
                indexed("developerWallet", "address")
            ),
            handledEvent(
                "ProjectStatusChanged",
                workflow::onLifecycleProjectStatusChanged,
                indexed("projectId", "uint256"),
                field("status", "uint8")
            ),
            handledEvent(
                "ProjectUpdated",
                workflow::onLifecycleProjectUpdated,
                indexed("projectId", "uint256"),
                field("externalProjectId", "string")
            ),
            handledEvent(
                "ProjectApproverRemoved",
                workflow::onLifecycleProjectApproverRemoved,
                indexed("projectId", "uint256"),
                indexed("userHash", "bytes32")
            ),
            handledEvent(
                "ClaimCreated",
                workflow::onLifecycleClaimCreated,
                indexed("claimId", "uint256"),
                indexed("projectId", "uint256"),
                indexed("contractorWallet", "address"),
                field("status", "uint8")
            ),
            handledEvent(
                "ClaimDocumentsUpdated",
                workflow::onLifecycleClaimDocumentsUpdated,
                indexed("claimId", "uint256"),
                field("documentCount", "uint256")
            ),
            handledEvent(
                "ClaimStatusChanged",
                workflow::onLifecycleClaimStatusChanged,
                indexed("claimId", "uint256"),
                field("status", "uint8")
            ),
            handledEvent(
                "InvoiceCreated",
                workflow::onLifecycleInvoiceCreated,
                indexed("invoiceId", "uint256"),
                indexed("claimId", "uint256"),
                field("status", "uint8")
            ),
            handledEvent(
                "InvoiceDocumentsUpdated",
                workflow::onLifecycleInvoiceDocumentsUpdated,
                indexed("invoiceId", "uint256"),
                field("documentCount", "uint256")
            ),
            handledEvent(
                "InvoiceStatusChanged",
                workflow::onLifecycleInvoiceStatusChanged,
                indexed("invoiceId", "uint256"),
                field("status", "uint8")
            ),
            handledEvent(
                "PaymentOrderCreated",
                workflow::onLifecyclePaymentOrderCreated,
                indexed("paymentOrderId", "uint256"),
                indexed("invoiceId", "uint256"),
                field("status", "uint8")
            ),
            handledEvent(
                "PaymentOrderStatusChanged",
                workflow::onLifecyclePaymentOrderStatusChanged,
                indexed("paymentOrderId", "uint256"),
                field("status", "uint8")
            ),
            handledEvent(
                "PaymentCreatedForOrder",
                workflow::onLifecyclePaymentCreatedForOrder,
                indexed("paymentOrderId", "uint256"),
                indexed("paymentId", "uint256"),
                indexed("invoiceId", "uint256")
            ),
            handledEvent(
                "BankPaymentRequested",
                workflow::onLifecycleBankPaymentRequested,
                indexed("paymentOrderId", "uint256"),
                indexed("invoiceId", "uint256"),
                field("customerRefNumber", "string")
            ),
            handledEvent(
                "BankPaymentReferenceRecorded",
                workflow::onLifecycleBankPaymentReferenceRecorded,
                indexed("paymentOrderId", "uint256"),
                field("bankPaymentRef", "string")
            )
        ) + accessControlEvents(
            workflow::onLifecycleRoleAdminChanged,
            workflow::onLifecycleRoleGranted,
            workflow::onLifecycleRoleRevoked
        )
    }

    private fun paymentEvents(workflow: TopazWorkflowService): List<EventRegistration> {
        return listOf(
            handledEvent(
                "PaymentCreated",
                workflow::onPaymentPaymentCreated,
                indexed("paymentId", "uint256"),
                indexed("paymentOrderId", "uint256"),
                indexed("invoiceId", "uint256"),
                field("customerRefNumber", "string"),
                field("instructedAmountMinor", "uint256"),
                field("instructedCurrency", "string")
            ),
            handledEvent(
                "PaymentAccepted",
                workflow::onPaymentPaymentAccepted,
                indexed("paymentId", "uint256"),
                indexed("paymentOrderId", "uint256"),
                field("settlementBankRef", "string")
            ),
            handledEvent(
                "PaymentRejected",
                workflow::onPaymentPaymentRejected,
                indexed("paymentId", "uint256"),
                indexed("paymentOrderId", "uint256"),
                field("rejectCode", "string"),
                field("rejectReason", "string")
            ),
            handledEvent(
                "PaymentReceiptCreated",
                workflow::onPaymentPaymentReceiptCreated,
                indexed("paymentReceiptId", "uint256"),
                indexed("paymentId", "uint256"),
                indexed("paymentOrderId", "uint256"),
                field("transactionRefNum", "string")
            )
        ) + accessControlEvents(
            workflow::onPaymentRoleAdminChanged,
            workflow::onPaymentRoleGranted,
            workflow::onPaymentRoleRevoked
        )
    }

    private fun contactEvents(workflow: TopazWorkflowService): List<EventRegistration> {
        return listOf(
            handledEvent(
                "ContactUpserted",
                workflow::onContactsContactUpserted,
                indexed("contactId", "uint256"),
                field("party", "string"),
                field("accountName", "string"),
                field("contactType", "string"),
                field("created", "bool"),
                field("active", "bool")
            ),
            handledEvent(
                "ContactDeactivated",
                workflow::onContactsContactDeactivated,
                indexed("contactId", "uint256"),
                field("party", "string"),
                field("accountName", "string")
            )
        ) + accessControlEvents(
            workflow::onContactsRoleAdminChanged,
            workflow::onContactsRoleGranted,
            workflow::onContactsRoleRevoked
        )
    }

    private fun accessControlEvents(
        roleAdminChanged: (TopazDecodedEvent) -> Unit,
        roleGranted: (TopazDecodedEvent) -> Unit,
        roleRevoked: (TopazDecodedEvent) -> Unit
    ): List<EventRegistration> {
        return listOf(
            roleAdminChangedEvent(roleAdminChanged),
            roleChangeEvent("RoleGranted", roleGranted),
            roleChangeEvent("RoleRevoked", roleRevoked)
        )
    }

    private fun roleAdminChangedEvent(handler: (TopazDecodedEvent) -> Unit): EventRegistration {
        return handledEvent(
            "RoleAdminChanged",
            handler,
            indexed("role", "bytes32"),
            indexed("previousAdminRole", "bytes32"),
            indexed("newAdminRole", "bytes32")
        )
    }

    private fun roleChangeEvent(name: String, handler: (TopazDecodedEvent) -> Unit): EventRegistration {
        return handledEvent(
            name,
            handler,
            indexed("role", "bytes32"),
            indexed("account", "address"),
            indexed("sender", "address")
        )
    }

    private fun indexed(name: String, type: String): TopazEventInput {
        return TopazEventInput(name = name, type = type, indexed = true)
    }

    private fun field(name: String, type: String): TopazEventInput {
        return TopazEventInput(name = name, type = type, indexed = false)
    }

    private fun handledEvent(
        name: String,
        handler: (TopazDecodedEvent) -> Unit,
        vararg inputs: TopazEventInput
    ): EventRegistration {
        return EventRegistration(
            event = EventSpec(name, inputs.toList()),
            handler = handler
        )
    }

    // ---- Internal model ----

    private data class ContractSpec(
        val name: String,
        val address: String,
        val events: List<EventRegistration>
    )

    private data class EventRegistration(
        val event: EventSpec,
        val handler: (TopazDecodedEvent) -> Unit
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
}
