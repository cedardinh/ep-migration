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

    fun onClaimCreated(r: RoutedEvent, ev: TopazLifecycleEvent.ClaimCreated) {
        if (!dedup.firstTime(r.eventId)) return
        workflow.handleClaimCreated(r.meta, ev)
    }

    fun onInvoiceCreated(r: RoutedEvent, ev: TopazLifecycleEvent.InvoiceCreated) {
        if (!dedup.firstTime(r.eventId)) return
        workflow.handleInvoiceCreated(r.meta, ev)
    }

    fun onBankPaymentRequested(r: RoutedEvent, ev: TopazLifecycleEvent.BankPaymentRequested) {
        if (!dedup.firstTime(r.eventId)) return
        workflow.requestBankPayment(r.meta, ev)
    }

    fun onBankPaymentReferenceRecorded(r: RoutedEvent, ev: TopazLifecycleEvent.BankPaymentReferenceRecorded) {
        if (!dedup.firstTime(r.eventId)) return
        workflow.handleBankPaymentReference(r.meta, ev)
    }
}
