package io.cryptoblk.migration.listenernew

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

/**
 * Workflow entry point: one handler per on-chain event. The current implementation
 * simply prints the decoded event; real business logic can be filled in per contract later.
 */
@Service
class TopazWorkflowService {
    private val log = LoggerFactory.getLogger(TopazWorkflowService::class.java)

    // ---- Lifecycle contract ----

    fun onLifecycleProjectCreated(event: TopazDecodedEvent) {
        printWorkflow("onLifecycleProjectCreated", event)
    }

    fun onLifecycleProjectStatusChanged(event: TopazDecodedEvent) {
        printWorkflow("onLifecycleProjectStatusChanged", event)
    }

    fun onLifecycleProjectUpdated(event: TopazDecodedEvent) {
        printWorkflow("onLifecycleProjectUpdated", event)
    }

    fun onLifecycleProjectApproverRemoved(event: TopazDecodedEvent) {
        printWorkflow("onLifecycleProjectApproverRemoved", event)
    }

    fun onLifecycleClaimCreated(event: TopazDecodedEvent) {
        printWorkflow("onLifecycleClaimCreated", event)
    }

    fun onLifecycleClaimDocumentsUpdated(event: TopazDecodedEvent) {
        printWorkflow("onLifecycleClaimDocumentsUpdated", event)
    }

    fun onLifecycleClaimStatusChanged(event: TopazDecodedEvent) {
        printWorkflow("onLifecycleClaimStatusChanged", event)
    }

    fun onLifecycleInvoiceCreated(event: TopazDecodedEvent) {
        printWorkflow("onLifecycleInvoiceCreated", event)
    }

    fun onLifecycleInvoiceDocumentsUpdated(event: TopazDecodedEvent) {
        printWorkflow("onLifecycleInvoiceDocumentsUpdated", event)
    }

    fun onLifecycleInvoiceStatusChanged(event: TopazDecodedEvent) {
        printWorkflow("onLifecycleInvoiceStatusChanged", event)
    }

    fun onLifecyclePaymentOrderCreated(event: TopazDecodedEvent) {
        printWorkflow("onLifecyclePaymentOrderCreated", event)
    }

    fun onLifecyclePaymentOrderStatusChanged(event: TopazDecodedEvent) {
        printWorkflow("onLifecyclePaymentOrderStatusChanged", event)
    }

    fun onLifecyclePaymentCreatedForOrder(event: TopazDecodedEvent) {
        printWorkflow("onLifecyclePaymentCreatedForOrder", event)
    }

    fun onLifecycleBankPaymentRequested(event: TopazDecodedEvent) {
        printWorkflow("onLifecycleBankPaymentRequested", event)
    }

    fun onLifecycleBankPaymentReferenceRecorded(event: TopazDecodedEvent) {
        printWorkflow("onLifecycleBankPaymentReferenceRecorded", event)
    }

    fun onLifecycleRoleAdminChanged(event: TopazDecodedEvent) {
        printWorkflow("onLifecycleRoleAdminChanged", event)
    }

    fun onLifecycleRoleGranted(event: TopazDecodedEvent) {
        printWorkflow("onLifecycleRoleGranted", event)
    }

    fun onLifecycleRoleRevoked(event: TopazDecodedEvent) {
        printWorkflow("onLifecycleRoleRevoked", event)
    }

    // ---- Payment contract ----

    fun onPaymentPaymentCreated(event: TopazDecodedEvent) {
        printWorkflow("onPaymentPaymentCreated", event)
    }

    fun onPaymentPaymentAccepted(event: TopazDecodedEvent) {
        printWorkflow("onPaymentPaymentAccepted", event)
    }

    fun onPaymentPaymentRejected(event: TopazDecodedEvent) {
        printWorkflow("onPaymentPaymentRejected", event)
    }

    fun onPaymentPaymentReceiptCreated(event: TopazDecodedEvent) {
        printWorkflow("onPaymentPaymentReceiptCreated", event)
    }

    fun onPaymentRoleAdminChanged(event: TopazDecodedEvent) {
        printWorkflow("onPaymentRoleAdminChanged", event)
    }

    fun onPaymentRoleGranted(event: TopazDecodedEvent) {
        printWorkflow("onPaymentRoleGranted", event)
    }

    fun onPaymentRoleRevoked(event: TopazDecodedEvent) {
        printWorkflow("onPaymentRoleRevoked", event)
    }

    // ---- Contacts contract ----

    fun onContactsContactUpserted(event: TopazDecodedEvent) {
        printWorkflow("onContactsContactUpserted", event)
    }

    fun onContactsContactDeactivated(event: TopazDecodedEvent) {
        printWorkflow("onContactsContactDeactivated", event)
    }

    fun onContactsRoleAdminChanged(event: TopazDecodedEvent) {
        printWorkflow("onContactsRoleAdminChanged", event)
    }

    fun onContactsRoleGranted(event: TopazDecodedEvent) {
        printWorkflow("onContactsRoleGranted", event)
    }

    fun onContactsRoleRevoked(event: TopazDecodedEvent) {
        printWorkflow("onContactsRoleRevoked", event)
    }

    // ---- Shared output ----

    private fun printWorkflow(handler: String, event: TopazDecodedEvent) {
        val message = "Workflow event " +
            "handler=$handler " +
            "contract=${event.contractName} " +
            "address=${event.contractAddress} " +
            "event=${event.eventName} " +
            "txHash=${event.log.transactionHash} " +
            "blockNumber=${event.log.blockNumber} " +
            "logIndex=${event.log.logIndex} " +
            "values=${event.values}"
        log.info(message)
        println(message)
    }
}
