package io.cryptoblk.migration.listener

import org.springframework.stereotype.Service

@Service
class TopazLifecycleHandlers(
    private val workflow: TopazWorkflowService,
    private val dedup: EventDedupService
) {

    fun onProjectCreated(r: RoutedEvent, ev: TopazLifecycleEvent.ProjectCreated) {
        if (!dedup.firstTime(r.eventId)) return
        workflow.handleProjectCreated(r.meta, ev)
    }

    fun onProjectUpdated(r: RoutedEvent, ev: TopazLifecycleEvent.ProjectUpdated) = handleOnce(r, ev)

    fun onProjectStatusChanged(r: RoutedEvent, ev: TopazLifecycleEvent.ProjectStatusChanged) = handleOnce(r, ev)

    fun onProjectApproverRemoved(r: RoutedEvent, ev: TopazLifecycleEvent.ProjectApproverRemoved) = handleOnce(r, ev)

    fun onClaimCreated(r: RoutedEvent, ev: TopazLifecycleEvent.ClaimCreated) {
        if (!dedup.firstTime(r.eventId)) return
        workflow.handleClaimCreated(r.meta, ev)
    }

    fun onClaimStatusChanged(r: RoutedEvent, ev: TopazLifecycleEvent.ClaimStatusChanged) = handleOnce(r, ev)

    fun onClaimDocumentsUpdated(r: RoutedEvent, ev: TopazLifecycleEvent.ClaimDocumentsUpdated) = handleOnce(r, ev)

    fun onInvoiceCreated(r: RoutedEvent, ev: TopazLifecycleEvent.InvoiceCreated) {
        if (!dedup.firstTime(r.eventId)) return
        workflow.handleInvoiceCreated(r.meta, ev)
    }

    fun onInvoiceStatusChanged(r: RoutedEvent, ev: TopazLifecycleEvent.InvoiceStatusChanged) = handleOnce(r, ev)

    fun onInvoiceDocumentsUpdated(r: RoutedEvent, ev: TopazLifecycleEvent.InvoiceDocumentsUpdated) = handleOnce(r, ev)

    fun onPaymentOrderCreated(r: RoutedEvent, ev: TopazLifecycleEvent.PaymentOrderCreated) = handleOnce(r, ev)

    fun onPaymentOrderStatusChanged(r: RoutedEvent, ev: TopazLifecycleEvent.PaymentOrderStatusChanged) = handleOnce(r, ev)

    fun onPaymentCreatedForOrder(r: RoutedEvent, ev: TopazLifecycleEvent.PaymentCreatedForOrder) = handleOnce(r, ev)

    fun onBankPaymentRequested(r: RoutedEvent, ev: TopazLifecycleEvent.BankPaymentRequested) {
        if (!dedup.firstTime(r.eventId)) return
        workflow.requestBankPayment(r.meta, ev)
    }

    fun onBankPaymentReferenceRecorded(r: RoutedEvent, ev: TopazLifecycleEvent.BankPaymentReferenceRecorded) {
        if (!dedup.firstTime(r.eventId)) return
        workflow.handleBankPaymentReference(r.meta, ev)
    }

    fun onRoleAdminChanged(r: RoutedEvent, ev: TopazLifecycleEvent.RoleAdminChanged) = handleOnce(r, ev)

    fun onRoleGranted(r: RoutedEvent, ev: TopazLifecycleEvent.RoleGranted) = handleOnce(r, ev)

    fun onRoleRevoked(r: RoutedEvent, ev: TopazLifecycleEvent.RoleRevoked) = handleOnce(r, ev)

    private fun handleOnce(r: RoutedEvent, ev: TopazLifecycleEvent) {
        if (!dedup.firstTime(r.eventId)) return
        workflow.handleLifecycleEvent(r.meta, ev)
    }
}
