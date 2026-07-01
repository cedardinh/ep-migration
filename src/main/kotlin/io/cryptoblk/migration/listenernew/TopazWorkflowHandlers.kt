package io.cryptoblk.migration.listenernew

import com.demo.server.epmigration.chain.generated.TopazContacts
import com.demo.server.epmigration.chain.generated.TopazLifecycle
import com.demo.server.epmigration.chain.generated.TopazPayment
import org.web3j.protocol.core.methods.response.BaseEventResponse

/** 根据合约和事件名解析最终要调用的工作流处理函数。 */
internal fun handlerFor(
    contract: TopazContract,
    eventName: String,
    workflow: TopazWorkflowService
): (BaseEventResponse) -> Unit {
    return when (contract) {
        TopazContract.LIFECYCLE -> when (eventName) {
            TopazLifecycle.PROJECTCREATED_EVENT.name -> bind(workflow::onLifecycleProjectCreated)
            TopazLifecycle.PROJECTSTATUSCHANGED_EVENT.name -> bind(workflow::onLifecycleProjectStatusChanged)
            TopazLifecycle.PROJECTUPDATED_EVENT.name -> bind(workflow::onLifecycleProjectUpdated)
            TopazLifecycle.PROJECTAPPROVERREMOVED_EVENT.name -> bind(workflow::onLifecycleProjectApproverRemoved)
            TopazLifecycle.CLAIMCREATED_EVENT.name -> bind(workflow::onLifecycleClaimCreated)
            TopazLifecycle.CLAIMDOCUMENTSUPDATED_EVENT.name -> bind(workflow::onLifecycleClaimDocumentsUpdated)
            TopazLifecycle.CLAIMSTATUSCHANGED_EVENT.name -> bind(workflow::onLifecycleClaimStatusChanged)
            TopazLifecycle.INVOICECREATED_EVENT.name -> bind(workflow::onLifecycleInvoiceCreated)
            TopazLifecycle.INVOICEDOCUMENTSUPDATED_EVENT.name -> bind(workflow::onLifecycleInvoiceDocumentsUpdated)
            TopazLifecycle.INVOICESTATUSCHANGED_EVENT.name -> bind(workflow::onLifecycleInvoiceStatusChanged)
            TopazLifecycle.PAYMENTORDERCREATED_EVENT.name -> bind(workflow::onLifecyclePaymentOrderCreated)
            TopazLifecycle.PAYMENTORDERSTATUSCHANGED_EVENT.name -> bind(workflow::onLifecyclePaymentOrderStatusChanged)
            TopazLifecycle.PAYMENTCREATEDFORORDER_EVENT.name -> bind(workflow::onLifecyclePaymentCreatedForOrder)
            TopazLifecycle.BANKPAYMENTREQUESTED_EVENT.name -> bind(workflow::onLifecycleBankPaymentRequested)
            TopazLifecycle.BANKPAYMENTREFERENCERECORDED_EVENT.name -> bind(workflow::onLifecycleBankPaymentReferenceRecorded)
            TopazLifecycle.ROLEADMINCHANGED_EVENT.name -> bind(workflow::onLifecycleRoleAdminChanged)
            TopazLifecycle.ROLEGRANTED_EVENT.name -> bind(workflow::onLifecycleRoleGranted)
            TopazLifecycle.ROLEREVOKED_EVENT.name -> bind(workflow::onLifecycleRoleRevoked)
            else -> null
        }
        TopazContract.PAYMENT -> when (eventName) {
            TopazPayment.PAYMENTCREATED_EVENT.name -> bind(workflow::onPaymentPaymentCreated)
            TopazPayment.PAYMENTACCEPTED_EVENT.name -> bind(workflow::onPaymentPaymentAccepted)
            TopazPayment.PAYMENTREJECTED_EVENT.name -> bind(workflow::onPaymentPaymentRejected)
            TopazPayment.PAYMENTRECEIPTCREATED_EVENT.name -> bind(workflow::onPaymentPaymentReceiptCreated)
            TopazPayment.ROLEADMINCHANGED_EVENT.name -> bind(workflow::onPaymentRoleAdminChanged)
            TopazPayment.ROLEGRANTED_EVENT.name -> bind(workflow::onPaymentRoleGranted)
            TopazPayment.ROLEREVOKED_EVENT.name -> bind(workflow::onPaymentRoleRevoked)
            else -> null
        }
        TopazContract.CONTACTS -> when (eventName) {
            TopazContacts.CONTACTUPSERTED_EVENT.name -> bind(workflow::onContactsContactUpserted)
            TopazContacts.CONTACTDEACTIVATED_EVENT.name -> bind(workflow::onContactsContactDeactivated)
            TopazContacts.ROLEADMINCHANGED_EVENT.name -> bind(workflow::onContactsRoleAdminChanged)
            TopazContacts.ROLEGRANTED_EVENT.name -> bind(workflow::onContactsRoleGranted)
            TopazContacts.ROLEREVOKED_EVENT.name -> bind(workflow::onContactsRoleRevoked)
            else -> null
        }
    } ?: error("No workflow handler for ${contract.id}.$eventName")
}

/** 将通用 wrapper 响应转成具体事件响应后调用类型明确的工作流函数。 */
@Suppress("UNCHECKED_CAST")
private fun <R : BaseEventResponse> bind(
    handle: (R) -> Unit
): (BaseEventResponse) -> Unit {
    return { response -> handle(response as R) }
}
