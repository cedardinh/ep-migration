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
        printWorkflow(event)
    }

    fun onLifecycleProjectStatusChanged(event: TopazDecodedEvent) {
        printWorkflow(event)
    }

    fun onLifecycleProjectUpdated(event: TopazDecodedEvent) {
        printWorkflow(event)
    }

    fun onLifecycleProjectApproverRemoved(event: TopazDecodedEvent) {
        printWorkflow(event)
    }

    fun onLifecycleClaimCreated(event: TopazDecodedEvent) {
        printWorkflow(event)
    }

    fun onLifecycleClaimDocumentsUpdated(event: TopazDecodedEvent) {
        printWorkflow(event)
    }

    fun onLifecycleClaimStatusChanged(event: TopazDecodedEvent) {
        printWorkflow(event)
    }

    fun onLifecycleInvoiceCreated(event: TopazDecodedEvent) {
        printWorkflow(event)
    }

    fun onLifecycleInvoiceDocumentsUpdated(event: TopazDecodedEvent) {
        printWorkflow(event)
    }

    fun onLifecycleInvoiceStatusChanged(event: TopazDecodedEvent) {
        printWorkflow(event)
    }

    fun onLifecyclePaymentOrderCreated(event: TopazDecodedEvent) {
        printWorkflow(event)
    }

    fun onLifecyclePaymentOrderStatusChanged(event: TopazDecodedEvent) {
        printWorkflow(event)
    }

    fun onLifecyclePaymentCreatedForOrder(event: TopazDecodedEvent) {
        printWorkflow(event)
    }

    fun onLifecycleBankPaymentRequested(event: TopazDecodedEvent) {
        printWorkflow(event)
    }

    fun onLifecycleBankPaymentReferenceRecorded(event: TopazDecodedEvent) {
        printWorkflow(event)
    }

    fun onLifecycleRoleAdminChanged(event: TopazDecodedEvent) {
        printWorkflow(event)
    }

    fun onLifecycleRoleGranted(event: TopazDecodedEvent) {
        printWorkflow(event)
    }

    fun onLifecycleRoleRevoked(event: TopazDecodedEvent) {
        printWorkflow(event)
    }

    // ---- Payment contract ----

    fun onPaymentPaymentCreated(event: TopazDecodedEvent) {
        printWorkflow(event)
    }

    fun onPaymentPaymentAccepted(event: TopazDecodedEvent) {
        printWorkflow(event)
    }

    fun onPaymentPaymentRejected(event: TopazDecodedEvent) {
        printWorkflow(event)
    }

    fun onPaymentPaymentReceiptCreated(event: TopazDecodedEvent) {
        printWorkflow(event)
    }

    fun onPaymentRoleAdminChanged(event: TopazDecodedEvent) {
        printWorkflow(event)
    }

    fun onPaymentRoleGranted(event: TopazDecodedEvent) {
        printWorkflow(event)
    }

    fun onPaymentRoleRevoked(event: TopazDecodedEvent) {
        printWorkflow(event)
    }

    // ---- Contacts contract ----

    fun onContactsContactUpserted(event: TopazDecodedEvent) {
        printWorkflow(event)
    }

    fun onContactsContactDeactivated(event: TopazDecodedEvent) {
        printWorkflow(event)
    }

    fun onContactsRoleAdminChanged(event: TopazDecodedEvent) {
        printWorkflow(event)
    }

    fun onContactsRoleGranted(event: TopazDecodedEvent) {
        printWorkflow(event)
    }

    fun onContactsRoleRevoked(event: TopazDecodedEvent) {
        printWorkflow(event)
    }

    // ---- Shared output ----

    private fun printWorkflow(event: TopazDecodedEvent) {
        val message = "Workflow event " +
            "contract=${event.contractName} " +
            "address=${event.contractAddress} " +
            "event=${event.eventName} " +
            "txHash=${event.log.transactionHash} " +
            "blockNumber=${event.log.blockNumber} " +
            "logIndex=${event.log.logIndex} " +
            "values=${event.values}"
        log.info(message)
    }
}
