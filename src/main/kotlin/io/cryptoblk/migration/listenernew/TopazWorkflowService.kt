package io.cryptoblk.migration.listenernew

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

/**
 * Workflow entry point: one handler per on-chain event. The current implementation
 * simply prints the decoded event; real business logic can be filled in per contract later.
 */
@Service
class TopazWorkflowService {
    private val log = LoggerFactory.getLogger(TopazWorkflowService::class.java)
    private val objectMapper = jacksonObjectMapper()

    // ---- Lifecycle contract ----

    fun onLifecycleProjectCreated(event: TopazDecodedEvent, params: TopazEventParams.LifecycleProjectCreated) {
        printWorkflow("onLifecycleProjectCreated", event, params)
    }

    fun onLifecycleProjectStatusChanged(event: TopazDecodedEvent, params: TopazEventParams.LifecycleProjectStatusChanged) {
        printWorkflow("onLifecycleProjectStatusChanged", event, params)
    }

    fun onLifecycleProjectUpdated(event: TopazDecodedEvent, params: TopazEventParams.LifecycleProjectUpdated) {
        printWorkflow("onLifecycleProjectUpdated", event, params)
    }

    fun onLifecycleProjectApproverRemoved(event: TopazDecodedEvent, params: TopazEventParams.LifecycleProjectApproverRemoved) {
        printWorkflow("onLifecycleProjectApproverRemoved", event, params)
    }

    fun onLifecycleClaimCreated(event: TopazDecodedEvent, params: TopazEventParams.LifecycleClaimCreated) {
        printWorkflow("onLifecycleClaimCreated", event, params)
    }

    fun onLifecycleClaimDocumentsUpdated(event: TopazDecodedEvent, params: TopazEventParams.LifecycleClaimDocumentsUpdated) {
        printWorkflow("onLifecycleClaimDocumentsUpdated", event, params)
    }

    fun onLifecycleClaimStatusChanged(event: TopazDecodedEvent, params: TopazEventParams.LifecycleClaimStatusChanged) {
        printWorkflow("onLifecycleClaimStatusChanged", event, params)
    }

    fun onLifecycleInvoiceCreated(event: TopazDecodedEvent, params: TopazEventParams.LifecycleInvoiceCreated) {
        printWorkflow("onLifecycleInvoiceCreated", event, params)
    }

    fun onLifecycleInvoiceDocumentsUpdated(event: TopazDecodedEvent, params: TopazEventParams.LifecycleInvoiceDocumentsUpdated) {
        printWorkflow("onLifecycleInvoiceDocumentsUpdated", event, params)
    }

    fun onLifecycleInvoiceStatusChanged(event: TopazDecodedEvent, params: TopazEventParams.LifecycleInvoiceStatusChanged) {
        printWorkflow("onLifecycleInvoiceStatusChanged", event, params)
    }

    fun onLifecyclePaymentOrderCreated(event: TopazDecodedEvent, params: TopazEventParams.LifecyclePaymentOrderCreated) {
        printWorkflow("onLifecyclePaymentOrderCreated", event, params)
    }

    fun onLifecyclePaymentOrderStatusChanged(event: TopazDecodedEvent, params: TopazEventParams.LifecyclePaymentOrderStatusChanged) {
        printWorkflow("onLifecyclePaymentOrderStatusChanged", event, params)
    }

    fun onLifecyclePaymentCreatedForOrder(event: TopazDecodedEvent, params: TopazEventParams.LifecyclePaymentCreatedForOrder) {
        printWorkflow("onLifecyclePaymentCreatedForOrder", event, params)
    }

    fun onLifecycleBankPaymentRequested(event: TopazDecodedEvent, params: TopazEventParams.LifecycleBankPaymentRequested) {
        printWorkflow("onLifecycleBankPaymentRequested", event, params)
    }

    fun onLifecycleBankPaymentReferenceRecorded(event: TopazDecodedEvent, params: TopazEventParams.LifecycleBankPaymentReferenceRecorded) {
        printWorkflow("onLifecycleBankPaymentReferenceRecorded", event, params)
    }

    fun onLifecycleRoleAdminChanged(event: TopazDecodedEvent, params: TopazEventParams.LifecycleRoleAdminChanged) {
        printWorkflow("onLifecycleRoleAdminChanged", event, params)
    }

    fun onLifecycleRoleGranted(event: TopazDecodedEvent, params: TopazEventParams.LifecycleRoleGranted) {
        printWorkflow("onLifecycleRoleGranted", event, params)
    }

    fun onLifecycleRoleRevoked(event: TopazDecodedEvent, params: TopazEventParams.LifecycleRoleRevoked) {
        printWorkflow("onLifecycleRoleRevoked", event, params)
    }

    // ---- Payment contract ----

    fun onPaymentPaymentCreated(event: TopazDecodedEvent, params: TopazEventParams.PaymentPaymentCreated) {
        printWorkflow("onPaymentPaymentCreated", event, params)
    }

    fun onPaymentPaymentAccepted(event: TopazDecodedEvent, params: TopazEventParams.PaymentPaymentAccepted) {
        printWorkflow("onPaymentPaymentAccepted", event, params)
    }

    fun onPaymentPaymentRejected(event: TopazDecodedEvent, params: TopazEventParams.PaymentPaymentRejected) {
        printWorkflow("onPaymentPaymentRejected", event, params)
    }

    fun onPaymentPaymentReceiptCreated(event: TopazDecodedEvent, params: TopazEventParams.PaymentPaymentReceiptCreated) {
        printWorkflow("onPaymentPaymentReceiptCreated", event, params)
    }

    fun onPaymentRoleAdminChanged(event: TopazDecodedEvent, params: TopazEventParams.PaymentRoleAdminChanged) {
        printWorkflow("onPaymentRoleAdminChanged", event, params)
    }

    fun onPaymentRoleGranted(event: TopazDecodedEvent, params: TopazEventParams.PaymentRoleGranted) {
        printWorkflow("onPaymentRoleGranted", event, params)
    }

    fun onPaymentRoleRevoked(event: TopazDecodedEvent, params: TopazEventParams.PaymentRoleRevoked) {
        printWorkflow("onPaymentRoleRevoked", event, params)
    }

    // ---- Contacts contract ----

    fun onContactsContactUpserted(event: TopazDecodedEvent, params: TopazEventParams.ContactsContactUpserted) {
        printWorkflow("onContactsContactUpserted", event, params)
    }

    fun onContactsContactDeactivated(event: TopazDecodedEvent, params: TopazEventParams.ContactsContactDeactivated) {
        printWorkflow("onContactsContactDeactivated", event, params)
    }

    fun onContactsRoleAdminChanged(event: TopazDecodedEvent, params: TopazEventParams.ContactsRoleAdminChanged) {
        printWorkflow("onContactsRoleAdminChanged", event, params)
    }

    fun onContactsRoleGranted(event: TopazDecodedEvent, params: TopazEventParams.ContactsRoleGranted) {
        printWorkflow("onContactsRoleGranted", event, params)
    }

    fun onContactsRoleRevoked(event: TopazDecodedEvent, params: TopazEventParams.ContactsRoleRevoked) {
        printWorkflow("onContactsRoleRevoked", event, params)
    }

    // ---- Shared output ----

    private fun printWorkflow(handler: String, event: TopazDecodedEvent, params: TopazEventParams) {
        val payload = linkedMapOf<String, Any?>(
            "handler" to handler,
            "contract" to event.contractName,
            "address" to event.contractAddress,
            "event" to event.eventName,
            "txHash" to event.log.transactionHash,
            "blockNumber" to event.log.blockNumber,
            "logIndex" to event.log.logIndex,
            "params" to params,
            "fields" to event.fields
        )
        val json = objectMapper.writeValueAsString(payload)
        log.info("Workflow event {}", json)
        println(json)
    }
}
