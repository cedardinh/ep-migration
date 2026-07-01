package io.cryptoblk.migration.listenernew

import com.demo.server.epmigration.chain.generated.TopazContacts
import com.demo.server.epmigration.chain.generated.TopazLifecycle
import com.demo.server.epmigration.chain.generated.TopazPayment
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.web3j.protocol.core.methods.response.BaseEventResponse

/**
 * Topaz 链上事件的工作流入口, 当前先统一打印事件信息。
 */
@Service
class TopazWorkflowService {
    private val log = LoggerFactory.getLogger(TopazWorkflowService::class.java)
    private val objectMapper = jacksonObjectMapper()

    // ---- Lifecycle contract ----

    /** 处理 lifecycle 合约发出的项目创建事件。 */
    fun onLifecycleProjectCreated(response: TopazLifecycle.ProjectCreatedEventResponse) {
        printWorkflow(TopazContract.LIFECYCLE, TopazLifecycle.PROJECTCREATED_EVENT.name, response)
    }

    /** 处理 lifecycle 合约发出的项目状态变更事件。 */
    fun onLifecycleProjectStatusChanged(response: TopazLifecycle.ProjectStatusChangedEventResponse) {
        printWorkflow(TopazContract.LIFECYCLE, TopazLifecycle.PROJECTSTATUSCHANGED_EVENT.name, response)
    }

    /** 处理 lifecycle 合约发出的项目更新事件。 */
    fun onLifecycleProjectUpdated(response: TopazLifecycle.ProjectUpdatedEventResponse) {
        printWorkflow(TopazContract.LIFECYCLE, TopazLifecycle.PROJECTUPDATED_EVENT.name, response)
    }

    /** 处理 lifecycle 合约发出的项目审批人移除事件。 */
    fun onLifecycleProjectApproverRemoved(response: TopazLifecycle.ProjectApproverRemovedEventResponse) {
        printWorkflow(TopazContract.LIFECYCLE, TopazLifecycle.PROJECTAPPROVERREMOVED_EVENT.name, response)
    }

    /** 处理 lifecycle 合约发出的 claim 创建事件。 */
    fun onLifecycleClaimCreated(response: TopazLifecycle.ClaimCreatedEventResponse) {
        printWorkflow(TopazContract.LIFECYCLE, TopazLifecycle.CLAIMCREATED_EVENT.name, response)
    }

    /** 处理 lifecycle 合约发出的 claim 文档更新事件。 */
    fun onLifecycleClaimDocumentsUpdated(response: TopazLifecycle.ClaimDocumentsUpdatedEventResponse) {
        printWorkflow(TopazContract.LIFECYCLE, TopazLifecycle.CLAIMDOCUMENTSUPDATED_EVENT.name, response)
    }

    /** 处理 lifecycle 合约发出的 claim 状态变更事件。 */
    fun onLifecycleClaimStatusChanged(response: TopazLifecycle.ClaimStatusChangedEventResponse) {
        printWorkflow(TopazContract.LIFECYCLE, TopazLifecycle.CLAIMSTATUSCHANGED_EVENT.name, response)
    }

    /** 处理 lifecycle 合约发出的 invoice 创建事件。 */
    fun onLifecycleInvoiceCreated(response: TopazLifecycle.InvoiceCreatedEventResponse) {
        printWorkflow(TopazContract.LIFECYCLE, TopazLifecycle.INVOICECREATED_EVENT.name, response)
    }

    /** 处理 lifecycle 合约发出的 invoice 文档更新事件。 */
    fun onLifecycleInvoiceDocumentsUpdated(response: TopazLifecycle.InvoiceDocumentsUpdatedEventResponse) {
        printWorkflow(TopazContract.LIFECYCLE, TopazLifecycle.INVOICEDOCUMENTSUPDATED_EVENT.name, response)
    }

    /** 处理 lifecycle 合约发出的 invoice 状态变更事件。 */
    fun onLifecycleInvoiceStatusChanged(response: TopazLifecycle.InvoiceStatusChangedEventResponse) {
        printWorkflow(TopazContract.LIFECYCLE, TopazLifecycle.INVOICESTATUSCHANGED_EVENT.name, response)
    }

    /** 处理 lifecycle 合约发出的付款指令创建事件。 */
    fun onLifecyclePaymentOrderCreated(response: TopazLifecycle.PaymentOrderCreatedEventResponse) {
        printWorkflow(TopazContract.LIFECYCLE, TopazLifecycle.PAYMENTORDERCREATED_EVENT.name, response)
    }

    /** 处理 lifecycle 合约发出的付款指令状态变更事件。 */
    fun onLifecyclePaymentOrderStatusChanged(response: TopazLifecycle.PaymentOrderStatusChangedEventResponse) {
        printWorkflow(TopazContract.LIFECYCLE, TopazLifecycle.PAYMENTORDERSTATUSCHANGED_EVENT.name, response)
    }

    /** 处理 lifecycle 合约发出的付款指令关联付款创建事件。 */
    fun onLifecyclePaymentCreatedForOrder(response: TopazLifecycle.PaymentCreatedForOrderEventResponse) {
        printWorkflow(TopazContract.LIFECYCLE, TopazLifecycle.PAYMENTCREATEDFORORDER_EVENT.name, response)
    }

    /** 处理 lifecycle 合约发出的银行付款请求事件。 */
    fun onLifecycleBankPaymentRequested(response: TopazLifecycle.BankPaymentRequestedEventResponse) {
        printWorkflow(TopazContract.LIFECYCLE, TopazLifecycle.BANKPAYMENTREQUESTED_EVENT.name, response)
    }

    /** 处理 lifecycle 合约发出的银行付款引用记录事件。 */
    fun onLifecycleBankPaymentReferenceRecorded(response: TopazLifecycle.BankPaymentReferenceRecordedEventResponse) {
        printWorkflow(TopazContract.LIFECYCLE, TopazLifecycle.BANKPAYMENTREFERENCERECORDED_EVENT.name, response)
    }

    /** 处理 lifecycle 合约发出的角色管理员变更事件。 */
    fun onLifecycleRoleAdminChanged(response: TopazLifecycle.RoleAdminChangedEventResponse) {
        printWorkflow(TopazContract.LIFECYCLE, TopazLifecycle.ROLEADMINCHANGED_EVENT.name, response)
    }

    /** 处理 lifecycle 合约发出的角色授予事件。 */
    fun onLifecycleRoleGranted(response: TopazLifecycle.RoleGrantedEventResponse) {
        printWorkflow(TopazContract.LIFECYCLE, TopazLifecycle.ROLEGRANTED_EVENT.name, response)
    }

    /** 处理 lifecycle 合约发出的角色撤销事件。 */
    fun onLifecycleRoleRevoked(response: TopazLifecycle.RoleRevokedEventResponse) {
        printWorkflow(TopazContract.LIFECYCLE, TopazLifecycle.ROLEREVOKED_EVENT.name, response)
    }

    // ---- Payment contract ----

    /** 处理 payment 合约发出的付款创建事件。 */
    fun onPaymentPaymentCreated(response: TopazPayment.PaymentCreatedEventResponse) {
        printWorkflow(TopazContract.PAYMENT, TopazPayment.PAYMENTCREATED_EVENT.name, response)
    }

    /** 处理 payment 合约发出的付款接受事件。 */
    fun onPaymentPaymentAccepted(response: TopazPayment.PaymentAcceptedEventResponse) {
        printWorkflow(TopazContract.PAYMENT, TopazPayment.PAYMENTACCEPTED_EVENT.name, response)
    }

    /** 处理 payment 合约发出的付款拒绝事件。 */
    fun onPaymentPaymentRejected(response: TopazPayment.PaymentRejectedEventResponse) {
        printWorkflow(TopazContract.PAYMENT, TopazPayment.PAYMENTREJECTED_EVENT.name, response)
    }

    /** 处理 payment 合约发出的付款回执创建事件。 */
    fun onPaymentPaymentReceiptCreated(response: TopazPayment.PaymentReceiptCreatedEventResponse) {
        printWorkflow(TopazContract.PAYMENT, TopazPayment.PAYMENTRECEIPTCREATED_EVENT.name, response)
    }

    /** 处理 payment 合约发出的角色管理员变更事件。 */
    fun onPaymentRoleAdminChanged(response: TopazPayment.RoleAdminChangedEventResponse) {
        printWorkflow(TopazContract.PAYMENT, TopazPayment.ROLEADMINCHANGED_EVENT.name, response)
    }

    /** 处理 payment 合约发出的角色授予事件。 */
    fun onPaymentRoleGranted(response: TopazPayment.RoleGrantedEventResponse) {
        printWorkflow(TopazContract.PAYMENT, TopazPayment.ROLEGRANTED_EVENT.name, response)
    }

    /** 处理 payment 合约发出的角色撤销事件。 */
    fun onPaymentRoleRevoked(response: TopazPayment.RoleRevokedEventResponse) {
        printWorkflow(TopazContract.PAYMENT, TopazPayment.ROLEREVOKED_EVENT.name, response)
    }

    // ---- Contacts contract ----

    /** 处理 contacts 合约发出的联系人新增或更新事件。 */
    fun onContactsContactUpserted(response: TopazContacts.ContactUpsertedEventResponse) {
        printWorkflow(TopazContract.CONTACTS, TopazContacts.CONTACTUPSERTED_EVENT.name, response)
    }

    /** 处理 contacts 合约发出的联系人停用事件。 */
    fun onContactsContactDeactivated(response: TopazContacts.ContactDeactivatedEventResponse) {
        printWorkflow(TopazContract.CONTACTS, TopazContacts.CONTACTDEACTIVATED_EVENT.name, response)
    }

    /** 处理 contacts 合约发出的角色管理员变更事件。 */
    fun onContactsRoleAdminChanged(response: TopazContacts.RoleAdminChangedEventResponse) {
        printWorkflow(TopazContract.CONTACTS, TopazContacts.ROLEADMINCHANGED_EVENT.name, response)
    }

    /** 处理 contacts 合约发出的角色授予事件。 */
    fun onContactsRoleGranted(response: TopazContacts.RoleGrantedEventResponse) {
        printWorkflow(TopazContract.CONTACTS, TopazContacts.ROLEGRANTED_EVENT.name, response)
    }

    /** 处理 contacts 合约发出的角色撤销事件。 */
    fun onContactsRoleRevoked(response: TopazContacts.RoleRevokedEventResponse) {
        printWorkflow(TopazContract.CONTACTS, TopazContacts.ROLEREVOKED_EVENT.name, response)
    }

    // ---- Shared output ----

    /** 将已处理的链上事件统一输出为结构化日志。 */
    private fun printWorkflow(contract: TopazContract, eventName: String, response: BaseEventResponse) {
        val payload = linkedMapOf<String, Any?>(
            "handler" to "on${contract.handlerPrefix}$eventName",
            "contract" to contract.id,
            "address" to response.log.address,
            "event" to eventName,
            "txHash" to response.log.transactionHash,
            "blockNumber" to response.log.blockNumber,
            "logIndex" to response.log.logIndex,
            "responseType" to response::class.java.simpleName
        )
        val json = objectMapper.writeValueAsString(payload)
        log.info("Workflow event {}", json)
        println(json)
    }
}
