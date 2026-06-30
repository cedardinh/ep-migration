package io.cryptoblk.migration.listener

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
    props: EpChainProperties,
    web3j: Web3j,
    credentials: Credentials,
    private val objectMapper: ObjectMapper
) {
    private val log = LoggerFactory.getLogger(TopazLifecycleEventRouter::class.java)
    private val lifecycle: TopazLifecycle = TopazLifecycle.load(
        props.lifecycleContractAddress.trim(),
        web3j,
        credentials,
        props.gasPrice,
        props.gasLimit
    )

    fun handleProjectCreated(meta: ChainMeta, ev: TopazLifecycleEvent.ProjectCreated) {
        log.info("Workflow: project created tx=${meta.txHash} projectId=${ev.projectId}")
        println("Workflow: project created tx=${meta.txHash} projectId=${ev.projectId}")

        val summary = lifecycle.getProjectSummary(ev.projectId).send()
        val json = objectMapper.writeValueAsString(
            mapOf(
                "projectId" to ev.projectId,
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

    fun handleClaimCreated(meta: ChainMeta, ev: TopazLifecycleEvent.ClaimCreated) {
        println("Workflow: claim created tx=${meta.txHash} claimId=${ev.claimId}")
    }

    fun handleLifecycleEvent(meta: ChainMeta, ev: TopazLifecycleEvent) {
        val json = objectMapper.writeValueAsString(ev)
        log.info("Workflow: lifecycle event tx=${meta.txHash} block=${meta.blockNumber} logIndex=${meta.logIndex} type=${ev::class.simpleName} payload=$json")
        println("Workflow: lifecycle event tx=${meta.txHash} type=${ev::class.simpleName} payload=$json")
    }

    fun handleInvoiceCreated(meta: ChainMeta, ev: TopazLifecycleEvent.InvoiceCreated) {
        println("Workflow: invoice created tx=${meta.txHash} invoiceId=${ev.invoiceId}")
    }

    fun requestBankPayment(meta: ChainMeta, ev: TopazLifecycleEvent.BankPaymentRequested) {
        println("Workflow: bank payment requested tx=${meta.txHash} paymentOrderId=${ev.paymentOrderId}")
        // TODO: 调用银行/支付系统, 或发 Kafka/AMQP
    }

    fun handleBankPaymentReference(meta: ChainMeta, ev: TopazLifecycleEvent.BankPaymentReferenceRecorded) {
        println("Workflow: bank payment ref recorded tx=${meta.txHash} ref=${ev.bankPaymentRef}")
    }

    private fun TopazLifecycle.Participant.toJson(): Map<String, String> {
        return mapOf(
            "wallet" to wallet,
            "legalName" to legalName,
            "addressLine1" to addressLine1,
            "addressLine2" to addressLine2,
            "bic" to bic,
            "lei" to lei,
            "externalRef" to externalRef
        )
    }

    private fun TopazLifecycle.ApproverConfig.toJson(): Map<String, String> {
        return mapOf(
            "wallet" to wallet,
            "userHash" to Numeric.toHexString(userHash),
            "email" to email,
            "firstName" to firstName,
            "lastName" to lastName,
            "userProfileName" to userProfileName,
            "roleName" to roleName,
            "externalRef" to externalRef
        )
    }
}
