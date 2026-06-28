package io.cryptoblk.migration.listenernew

import com.demo.server.epmigration.chain.generated.TopazLifecycle
import org.web3j.abi.datatypes.Event
import org.web3j.protocol.core.methods.response.BaseEventResponse
import org.web3j.protocol.core.methods.response.Log

/** 一个事件订阅: ABI 事件定义 + 把原始 log 解码并分发给 workflow 的回调. */
data class TopazEventSubscription(
    val name: String,
    val event: Event,
    val handle: (Log) -> Unit
)

/**
 * TopazLifecycle 合约「事件 -> workflow 处理方法」的集中登记处.
 * 新增一个事件: 在 [subscriptions] 加一行, 在 [ABI_EVENTS] 加事件名, 并在 workflow 里加对应方法.
 */
object TopazEventRegistry {

    val ABI_EVENTS: Set<String> = setOf(
        "BankPaymentReferenceRecorded",
        "BankPaymentRequested",
        "ClaimCreated",
        "ClaimDocumentsUpdated",
        "ClaimStatusChanged",
        "InvoiceCreated",
        "InvoiceDocumentsUpdated",
        "InvoiceStatusChanged",
        "PaymentCreatedForOrder",
        "PaymentOrderCreated",
        "PaymentOrderStatusChanged",
        "ProjectApproverRemoved",
        "ProjectCreated",
        "ProjectStatusChanged",
        "ProjectUpdated",
        "RoleAdminChanged",
        "RoleGranted",
        "RoleRevoked"
    )

    fun subscriptions(workflow: TopazWorkflowService): List<TopazEventSubscription> = listOf(
        subscription("BankPaymentReferenceRecorded", TopazLifecycle.BANKPAYMENTREFERENCERECORDED_EVENT, TopazLifecycle::getBankPaymentReferenceRecordedEventFromLog, workflow::onBankPaymentReferenceRecorded),
        subscription("BankPaymentRequested", TopazLifecycle.BANKPAYMENTREQUESTED_EVENT, TopazLifecycle::getBankPaymentRequestedEventFromLog, workflow::onBankPaymentRequested),
        subscription("ClaimCreated", TopazLifecycle.CLAIMCREATED_EVENT, TopazLifecycle::getClaimCreatedEventFromLog, workflow::onClaimCreated),
        subscription("ClaimDocumentsUpdated", TopazLifecycle.CLAIMDOCUMENTSUPDATED_EVENT, TopazLifecycle::getClaimDocumentsUpdatedEventFromLog, workflow::onClaimDocumentsUpdated),
        subscription("ClaimStatusChanged", TopazLifecycle.CLAIMSTATUSCHANGED_EVENT, TopazLifecycle::getClaimStatusChangedEventFromLog, workflow::onClaimStatusChanged),
        subscription("InvoiceCreated", TopazLifecycle.INVOICECREATED_EVENT, TopazLifecycle::getInvoiceCreatedEventFromLog, workflow::onInvoiceCreated),
        subscription("InvoiceDocumentsUpdated", TopazLifecycle.INVOICEDOCUMENTSUPDATED_EVENT, TopazLifecycle::getInvoiceDocumentsUpdatedEventFromLog, workflow::onInvoiceDocumentsUpdated),
        subscription("InvoiceStatusChanged", TopazLifecycle.INVOICESTATUSCHANGED_EVENT, TopazLifecycle::getInvoiceStatusChangedEventFromLog, workflow::onInvoiceStatusChanged),
        subscription("PaymentCreatedForOrder", TopazLifecycle.PAYMENTCREATEDFORORDER_EVENT, TopazLifecycle::getPaymentCreatedForOrderEventFromLog, workflow::onPaymentCreatedForOrder),
        subscription("PaymentOrderCreated", TopazLifecycle.PAYMENTORDERCREATED_EVENT, TopazLifecycle::getPaymentOrderCreatedEventFromLog, workflow::onPaymentOrderCreated),
        subscription("PaymentOrderStatusChanged", TopazLifecycle.PAYMENTORDERSTATUSCHANGED_EVENT, TopazLifecycle::getPaymentOrderStatusChangedEventFromLog, workflow::onPaymentOrderStatusChanged),
        subscription("ProjectApproverRemoved", TopazLifecycle.PROJECTAPPROVERREMOVED_EVENT, TopazLifecycle::getProjectApproverRemovedEventFromLog, workflow::onProjectApproverRemoved),
        subscription("ProjectCreated", TopazLifecycle.PROJECTCREATED_EVENT, TopazLifecycle::getProjectCreatedEventFromLog, workflow::onProjectCreated),
        subscription("ProjectStatusChanged", TopazLifecycle.PROJECTSTATUSCHANGED_EVENT, TopazLifecycle::getProjectStatusChangedEventFromLog, workflow::onProjectStatusChanged),
        subscription("ProjectUpdated", TopazLifecycle.PROJECTUPDATED_EVENT, TopazLifecycle::getProjectUpdatedEventFromLog, workflow::onProjectUpdated),
        subscription("RoleAdminChanged", TopazLifecycle.ROLEADMINCHANGED_EVENT, TopazLifecycle::getRoleAdminChangedEventFromLog, workflow::onRoleAdminChanged),
        subscription("RoleGranted", TopazLifecycle.ROLEGRANTED_EVENT, TopazLifecycle::getRoleGrantedEventFromLog, workflow::onRoleGranted),
        subscription("RoleRevoked", TopazLifecycle.ROLEREVOKED_EVENT, TopazLifecycle::getRoleRevokedEventFromLog, workflow::onRoleRevoked)
    )

    private fun <T : BaseEventResponse> subscription(
        name: String,
        event: Event,
        decode: (Log) -> T,
        handle: (T) -> Unit
    ): TopazEventSubscription {
        return TopazEventSubscription(name, event) { chainLog ->
            handle(decode(chainLog))
        }
    }
}
