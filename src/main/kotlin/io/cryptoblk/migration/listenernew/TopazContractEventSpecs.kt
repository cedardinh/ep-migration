package io.cryptoblk.migration.listenernew

import com.demo.server.epmigration.chain.generated.TopazContacts
import com.demo.server.epmigration.chain.generated.TopazLifecycle
import com.demo.server.epmigration.chain.generated.TopazPayment
import org.web3j.abi.datatypes.Event
import org.web3j.protocol.core.methods.response.BaseEventResponse
import org.web3j.protocol.core.methods.response.Log

/** 列出每个 Topaz 合约当前支持监听的 wrapper 事件。 */
internal fun topazContractSpecs(): Map<TopazContract, List<EventSpec>> {
    return linkedMapOf(
        TopazContract.LIFECYCLE to listOf(
            wrapperEvent(TopazLifecycle.PROJECTCREATED_EVENT, TopazLifecycle::getProjectCreatedEventFromLog),
            wrapperEvent(TopazLifecycle.PROJECTSTATUSCHANGED_EVENT, TopazLifecycle::getProjectStatusChangedEventFromLog),
            wrapperEvent(TopazLifecycle.PROJECTUPDATED_EVENT, TopazLifecycle::getProjectUpdatedEventFromLog),
            wrapperEvent(TopazLifecycle.PROJECTAPPROVERREMOVED_EVENT, TopazLifecycle::getProjectApproverRemovedEventFromLog),
            wrapperEvent(TopazLifecycle.CLAIMCREATED_EVENT, TopazLifecycle::getClaimCreatedEventFromLog),
            wrapperEvent(TopazLifecycle.CLAIMDOCUMENTSUPDATED_EVENT, TopazLifecycle::getClaimDocumentsUpdatedEventFromLog),
            wrapperEvent(TopazLifecycle.CLAIMSTATUSCHANGED_EVENT, TopazLifecycle::getClaimStatusChangedEventFromLog),
            wrapperEvent(TopazLifecycle.INVOICECREATED_EVENT, TopazLifecycle::getInvoiceCreatedEventFromLog),
            wrapperEvent(TopazLifecycle.INVOICEDOCUMENTSUPDATED_EVENT, TopazLifecycle::getInvoiceDocumentsUpdatedEventFromLog),
            wrapperEvent(TopazLifecycle.INVOICESTATUSCHANGED_EVENT, TopazLifecycle::getInvoiceStatusChangedEventFromLog),
            wrapperEvent(TopazLifecycle.PAYMENTORDERCREATED_EVENT, TopazLifecycle::getPaymentOrderCreatedEventFromLog),
            wrapperEvent(TopazLifecycle.PAYMENTORDERSTATUSCHANGED_EVENT, TopazLifecycle::getPaymentOrderStatusChangedEventFromLog),
            wrapperEvent(TopazLifecycle.PAYMENTCREATEDFORORDER_EVENT, TopazLifecycle::getPaymentCreatedForOrderEventFromLog),
            wrapperEvent(TopazLifecycle.BANKPAYMENTREQUESTED_EVENT, TopazLifecycle::getBankPaymentRequestedEventFromLog),
            wrapperEvent(
                TopazLifecycle.BANKPAYMENTREFERENCERECORDED_EVENT,
                TopazLifecycle::getBankPaymentReferenceRecordedEventFromLog
            ),
            wrapperEvent(TopazLifecycle.ROLEADMINCHANGED_EVENT, TopazLifecycle::getRoleAdminChangedEventFromLog),
            wrapperEvent(TopazLifecycle.ROLEGRANTED_EVENT, TopazLifecycle::getRoleGrantedEventFromLog),
            wrapperEvent(TopazLifecycle.ROLEREVOKED_EVENT, TopazLifecycle::getRoleRevokedEventFromLog)
        ),
        TopazContract.PAYMENT to listOf(
            wrapperEvent(TopazPayment.PAYMENTCREATED_EVENT, TopazPayment::getPaymentCreatedEventFromLog),
            wrapperEvent(TopazPayment.PAYMENTACCEPTED_EVENT, TopazPayment::getPaymentAcceptedEventFromLog),
            wrapperEvent(TopazPayment.PAYMENTREJECTED_EVENT, TopazPayment::getPaymentRejectedEventFromLog),
            wrapperEvent(TopazPayment.PAYMENTRECEIPTCREATED_EVENT, TopazPayment::getPaymentReceiptCreatedEventFromLog),
            wrapperEvent(TopazPayment.ROLEADMINCHANGED_EVENT, TopazPayment::getRoleAdminChangedEventFromLog),
            wrapperEvent(TopazPayment.ROLEGRANTED_EVENT, TopazPayment::getRoleGrantedEventFromLog),
            wrapperEvent(TopazPayment.ROLEREVOKED_EVENT, TopazPayment::getRoleRevokedEventFromLog)
        ),
        TopazContract.CONTACTS to listOf(
            wrapperEvent(TopazContacts.CONTACTUPSERTED_EVENT, TopazContacts::getContactUpsertedEventFromLog),
            wrapperEvent(TopazContacts.CONTACTDEACTIVATED_EVENT, TopazContacts::getContactDeactivatedEventFromLog),
            wrapperEvent(TopazContacts.ROLEADMINCHANGED_EVENT, TopazContacts::getRoleAdminChangedEventFromLog),
            wrapperEvent(TopazContacts.ROLEGRANTED_EVENT, TopazContacts::getRoleGrantedEventFromLog),
            wrapperEvent(TopazContacts.ROLEREVOKED_EVENT, TopazContacts::getRoleRevokedEventFromLog)
        )
    )
}

/** 将 generated wrapper 的事件常量和日志解码函数适配为内部事件声明。 */
private fun <R : BaseEventResponse> wrapperEvent(
    event: Event,
    decodeResponse: (Log) -> R
): EventSpec {
    return EventSpec(
        name = event.name,
        event = event,
        decode = decodeResponse
    )
}
