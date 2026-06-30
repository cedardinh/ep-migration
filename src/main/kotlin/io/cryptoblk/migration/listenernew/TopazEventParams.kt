package io.cryptoblk.migration.listenernew

import java.math.BigInteger

sealed class TopazEventParams {
    data class LifecycleProjectCreated(
        val projectId: BigInteger,
        val externalProjectId: String,
        val developerWallet: String
    ) : TopazEventParams()

    data class LifecycleProjectStatusChanged(
        val projectId: BigInteger,
        val status: Int
    ) : TopazEventParams()

    data class LifecycleProjectUpdated(
        val projectId: BigInteger,
        val externalProjectId: String
    ) : TopazEventParams()

    data class LifecycleProjectApproverRemoved(
        val projectId: BigInteger,
        val userHash: String
    ) : TopazEventParams()

    data class LifecycleClaimCreated(
        val claimId: BigInteger,
        val projectId: BigInteger,
        val contractorWallet: String,
        val status: Int
    ) : TopazEventParams()

    data class LifecycleClaimDocumentsUpdated(
        val claimId: BigInteger,
        val documentCount: BigInteger
    ) : TopazEventParams()

    data class LifecycleClaimStatusChanged(
        val claimId: BigInteger,
        val status: Int
    ) : TopazEventParams()

    data class LifecycleInvoiceCreated(
        val invoiceId: BigInteger,
        val claimId: BigInteger,
        val status: Int
    ) : TopazEventParams()

    data class LifecycleInvoiceDocumentsUpdated(
        val invoiceId: BigInteger,
        val documentCount: BigInteger
    ) : TopazEventParams()

    data class LifecycleInvoiceStatusChanged(
        val invoiceId: BigInteger,
        val status: Int
    ) : TopazEventParams()

    data class LifecyclePaymentOrderCreated(
        val paymentOrderId: BigInteger,
        val invoiceId: BigInteger,
        val status: Int
    ) : TopazEventParams()

    data class LifecyclePaymentOrderStatusChanged(
        val paymentOrderId: BigInteger,
        val status: Int
    ) : TopazEventParams()

    data class LifecyclePaymentCreatedForOrder(
        val paymentOrderId: BigInteger,
        val paymentId: BigInteger,
        val invoiceId: BigInteger
    ) : TopazEventParams()

    data class LifecycleBankPaymentRequested(
        val paymentOrderId: BigInteger,
        val invoiceId: BigInteger,
        val customerRefNumber: String
    ) : TopazEventParams()

    data class LifecycleBankPaymentReferenceRecorded(
        val paymentOrderId: BigInteger,
        val bankPaymentRef: String
    ) : TopazEventParams()

    data class LifecycleRoleAdminChanged(
        val role: String,
        val previousAdminRole: String,
        val newAdminRole: String
    ) : TopazEventParams()

    data class LifecycleRoleGranted(
        val role: String,
        val account: String,
        val sender: String
    ) : TopazEventParams()

    data class LifecycleRoleRevoked(
        val role: String,
        val account: String,
        val sender: String
    ) : TopazEventParams()

    data class PaymentPaymentCreated(
        val paymentId: BigInteger,
        val paymentOrderId: BigInteger,
        val invoiceId: BigInteger,
        val customerRefNumber: String,
        val instructedAmountMinor: BigInteger,
        val instructedCurrency: String
    ) : TopazEventParams()

    data class PaymentPaymentAccepted(
        val paymentId: BigInteger,
        val paymentOrderId: BigInteger,
        val settlementBankRef: String
    ) : TopazEventParams()

    data class PaymentPaymentRejected(
        val paymentId: BigInteger,
        val paymentOrderId: BigInteger,
        val rejectCode: String,
        val rejectReason: String
    ) : TopazEventParams()

    data class PaymentPaymentReceiptCreated(
        val paymentReceiptId: BigInteger,
        val paymentId: BigInteger,
        val paymentOrderId: BigInteger,
        val transactionRefNum: String
    ) : TopazEventParams()

    data class PaymentRoleAdminChanged(
        val role: String,
        val previousAdminRole: String,
        val newAdminRole: String
    ) : TopazEventParams()

    data class PaymentRoleGranted(
        val role: String,
        val account: String,
        val sender: String
    ) : TopazEventParams()

    data class PaymentRoleRevoked(
        val role: String,
        val account: String,
        val sender: String
    ) : TopazEventParams()

    data class ContactsContactUpserted(
        val contactId: BigInteger,
        val wallet: String,
        val party: String,
        val accountName: String,
        val contactType: String,
        val created: Boolean,
        val active: Boolean
    ) : TopazEventParams()

    data class ContactsContactDeactivated(
        val contactId: BigInteger,
        val wallet: String,
        val party: String,
        val accountName: String
    ) : TopazEventParams()

    data class ContactsRoleAdminChanged(
        val role: String,
        val previousAdminRole: String,
        val newAdminRole: String
    ) : TopazEventParams()

    data class ContactsRoleGranted(
        val role: String,
        val account: String,
        val sender: String
    ) : TopazEventParams()

    data class ContactsRoleRevoked(
        val role: String,
        val account: String,
        val sender: String
    ) : TopazEventParams()
}
