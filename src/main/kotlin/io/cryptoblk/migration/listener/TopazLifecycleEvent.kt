package io.cryptoblk.migration.listener

import java.math.BigInteger

data class ChainMeta(
    val txHash: String,
    val blockNumber: BigInteger,
    val logIndex: BigInteger
)

sealed class TopazLifecycleEvent {
    data class ProjectCreated(val projectId: BigInteger, val externalProjectId: String, val developerWallet: String) : TopazLifecycleEvent()
    data class ProjectUpdated(val projectId: BigInteger, val externalProjectId: String) : TopazLifecycleEvent()
    data class ProjectStatusChanged(val projectId: BigInteger, val statusCode: Int) : TopazLifecycleEvent()
    data class ProjectApproverRemoved(val projectId: BigInteger, val userHashHex: String) : TopazLifecycleEvent()

    data class ClaimCreated(val claimId: BigInteger, val projectId: BigInteger, val contractorWallet: String, val statusCode: Int) : TopazLifecycleEvent()
    data class ClaimStatusChanged(val claimId: BigInteger, val statusCode: Int) : TopazLifecycleEvent()
    data class ClaimDocumentsUpdated(val claimId: BigInteger, val documentCount: BigInteger) : TopazLifecycleEvent()

    data class InvoiceCreated(val invoiceId: BigInteger, val claimId: BigInteger, val statusCode: Int) : TopazLifecycleEvent()
    data class InvoiceStatusChanged(val invoiceId: BigInteger, val statusCode: Int) : TopazLifecycleEvent()
    data class InvoiceDocumentsUpdated(val invoiceId: BigInteger, val documentCount: BigInteger) : TopazLifecycleEvent()

    data class PaymentOrderCreated(val paymentOrderId: BigInteger, val invoiceId: BigInteger, val statusCode: Int) : TopazLifecycleEvent()
    data class PaymentOrderStatusChanged(val paymentOrderId: BigInteger, val statusCode: Int) : TopazLifecycleEvent()
    data class PaymentCreatedForOrder(val paymentOrderId: BigInteger, val paymentId: BigInteger, val invoiceId: BigInteger) : TopazLifecycleEvent()
    data class BankPaymentRequested(val paymentOrderId: BigInteger, val invoiceId: BigInteger, val customerRefNumber: String) : TopazLifecycleEvent()
    data class BankPaymentReferenceRecorded(val paymentOrderId: BigInteger, val bankPaymentRef: String) : TopazLifecycleEvent()
}

data class RoutedEvent(
    val meta: ChainMeta,
    val event: TopazLifecycleEvent
) {
    /** 幂等键: 同一 tx 内多条 log 用 logIndex 区分 */
    val eventId: String = "${meta.txHash}:${meta.logIndex}"
}
