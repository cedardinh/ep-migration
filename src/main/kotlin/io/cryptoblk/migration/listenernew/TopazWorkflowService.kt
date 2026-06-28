package io.cryptoblk.migration.listenernew

import com.demo.server.epmigration.chain.generated.TopazLifecycle
import com.demo.server.epmigration.config.EpChainProperties
import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.web3j.crypto.Credentials
import org.web3j.protocol.Web3j
import org.web3j.utils.Numeric

@Service
class TopazWorkflowService(
    properties: EpChainProperties,
    web3j: Web3j,
    credentials: Credentials,
    private val objectMapper: ObjectMapper
) {
    private val log = LoggerFactory.getLogger(TopazWorkflowService::class.java)
    private val lifecycle = TopazLifecycle.load(
        properties.lifecycleContractAddress.trim(),
        web3j,
        credentials,
        properties.gasPrice,
        properties.gasLimit
    )

    fun onProjectCreated(event: TopazLifecycle.ProjectCreatedEventResponse) {
        printWorkflow("Workflow: project created tx=${event.log.transactionHash} projectId=${event.projectId}")

        val summary = lifecycle.getProjectSummary(event.projectId).send()
        val json = objectMapper.writeValueAsString(
            mapOf(
                "projectId" to event.projectId,
                "externalProjectId" to summary.value1,
                "name" to summary.value2,
                "status" to summary.value3,
                "developer" to summary.value4.toJson(),
                "mainContractors" to summary.value5.map { it.toJson() },
                "claimApprovers" to summary.value6.map { it.toJson() },
                "paymentApprovers" to summary.value7.map { it.toJson() },
                "bankAccountRefs" to summary.value8,
                "createdAt" to summary.value9,
                "updatedAt" to summary.value10,
                "claimCount" to summary.value11
            )
        )
        log.info("Workflow: project summary json=$json")
        println(json)
    }

    fun onProjectUpdated(event: TopazLifecycle.ProjectUpdatedEventResponse) {
        printWorkflow("Workflow: project updated tx=${event.log.transactionHash} projectId=${event.projectId} externalProjectId=${event.externalProjectId}")
    }

    fun onProjectStatusChanged(event: TopazLifecycle.ProjectStatusChangedEventResponse) {
        printWorkflow("Workflow: project status changed tx=${event.log.transactionHash} projectId=${event.projectId} status=${event.status}")
    }

    fun onProjectApproverRemoved(event: TopazLifecycle.ProjectApproverRemovedEventResponse) {
        printWorkflow("Workflow: project approver removed tx=${event.log.transactionHash} projectId=${event.projectId} userHash=${Numeric.toHexString(event.userHash)}")
    }

    fun onClaimCreated(event: TopazLifecycle.ClaimCreatedEventResponse) {
        printWorkflow("Workflow: claim created tx=${event.log.transactionHash} claimId=${event.claimId}")
    }

    fun onClaimStatusChanged(event: TopazLifecycle.ClaimStatusChangedEventResponse) {
        printWorkflow("Workflow: claim status changed tx=${event.log.transactionHash} claimId=${event.claimId} status=${event.status}")
    }

    fun onClaimDocumentsUpdated(event: TopazLifecycle.ClaimDocumentsUpdatedEventResponse) {
        printWorkflow("Workflow: claim documents updated tx=${event.log.transactionHash} claimId=${event.claimId} documentCount=${event.documentCount}")
    }

    fun onInvoiceCreated(event: TopazLifecycle.InvoiceCreatedEventResponse) {
        printWorkflow("Workflow: invoice created tx=${event.log.transactionHash} invoiceId=${event.invoiceId}")
    }

    fun onInvoiceStatusChanged(event: TopazLifecycle.InvoiceStatusChangedEventResponse) {
        printWorkflow("Workflow: invoice status changed tx=${event.log.transactionHash} invoiceId=${event.invoiceId} status=${event.status}")
    }

    fun onInvoiceDocumentsUpdated(event: TopazLifecycle.InvoiceDocumentsUpdatedEventResponse) {
        printWorkflow("Workflow: invoice documents updated tx=${event.log.transactionHash} invoiceId=${event.invoiceId} documentCount=${event.documentCount}")
    }

    fun onPaymentOrderCreated(event: TopazLifecycle.PaymentOrderCreatedEventResponse) {
        printWorkflow("Workflow: payment order created tx=${event.log.transactionHash} paymentOrderId=${event.paymentOrderId}")
    }

    fun onPaymentOrderStatusChanged(event: TopazLifecycle.PaymentOrderStatusChangedEventResponse) {
        printWorkflow("Workflow: payment order status changed tx=${event.log.transactionHash} paymentOrderId=${event.paymentOrderId} status=${event.status}")
    }

    fun onPaymentCreatedForOrder(event: TopazLifecycle.PaymentCreatedForOrderEventResponse) {
        printWorkflow("Workflow: payment created for order tx=${event.log.transactionHash} paymentOrderId=${event.paymentOrderId} paymentId=${event.paymentId}")
    }

    fun onBankPaymentRequested(event: TopazLifecycle.BankPaymentRequestedEventResponse) {
        printWorkflow("Workflow: bank payment requested tx=${event.log.transactionHash} paymentOrderId=${event.paymentOrderId}")
    }

    fun onBankPaymentReferenceRecorded(event: TopazLifecycle.BankPaymentReferenceRecordedEventResponse) {
        printWorkflow("Workflow: bank payment ref recorded tx=${event.log.transactionHash} ref=${event.bankPaymentRef}")
    }

    fun onRoleAdminChanged(event: TopazLifecycle.RoleAdminChangedEventResponse) {
        printWorkflow("Workflow: role admin changed tx=${event.log.transactionHash} role=${Numeric.toHexString(event.role)}")
    }

    fun onRoleGranted(event: TopazLifecycle.RoleGrantedEventResponse) {
        printWorkflow("Workflow: role granted tx=${event.log.transactionHash} role=${Numeric.toHexString(event.role)} account=${event.account}")
    }

    fun onRoleRevoked(event: TopazLifecycle.RoleRevokedEventResponse) {
        printWorkflow("Workflow: role revoked tx=${event.log.transactionHash} role=${Numeric.toHexString(event.role)} account=${event.account}")
    }

    private fun printWorkflow(message: String) {
        log.info(message)
        println(message)
    }
}
