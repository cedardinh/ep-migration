package io.cryptoblk.migration.listenernew

import com.demo.server.epmigration.chain.generated.TopazLifecycle
import com.demo.server.epmigration.config.EpChainProperties
import io.reactivex.disposables.Disposable
import org.slf4j.LoggerFactory
import org.springframework.context.SmartLifecycle
import org.springframework.stereotype.Component
import org.web3j.abi.EventEncoder
import org.web3j.abi.datatypes.Event
import org.web3j.protocol.Web3j
import org.web3j.protocol.core.DefaultBlockParameterName
import org.web3j.protocol.core.methods.request.EthFilter
import org.web3j.protocol.core.methods.response.BaseEventResponse
import org.web3j.protocol.core.methods.response.Log

@Component
class TopazLifecycleListener(
    private val web3j: Web3j,
    properties: EpChainProperties,
    workflow: TopazWorkflowService
) : SmartLifecycle {
    private val log = LoggerFactory.getLogger(TopazLifecycleListener::class.java)
    private val contractAddress = properties.lifecycleContractAddress.trim()
    private val subscriptions = listOf(
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
    private val byTopic0 = subscriptions.associateBy { EventEncoder.encode(it.event) }

    @Volatile
    private var running = false
    private var stream: Disposable? = null

    init {
        require(contractAddress.matches(Regex("^0x[0-9a-fA-F]{40}$"))) {
            "ep.chain.lifecycle-contract-address must be a 20-byte hex address"
        }
        require(subscriptions.map { it.name }.toSet() == TOPAZ_LIFECYCLE_ABI_EVENTS) {
            "TopazLifecycle listener subscriptions do not match ABI events"
        }
    }

    override fun start() {
        if (running) return
        val filter = EthFilter(DefaultBlockParameterName.LATEST, DefaultBlockParameterName.LATEST, contractAddress)
        stream = web3j.ethLogFlowable(filter)
            .retryWhen { errors -> errors.delay(5, java.util.concurrent.TimeUnit.SECONDS) }
            .subscribe(::route, ::subscriptionFailed)
        running = true
        println("TopazLifecycleListener started")
    }

    override fun stop() {
        stream?.dispose()
        running = false
        println("TopazLifecycleListener stopped")
    }

    override fun isRunning(): Boolean = running

    override fun getPhase(): Int = Int.MAX_VALUE

    private fun route(chainLog: Log) {
        val subscription = byTopic0[chainLog.topics.firstOrNull()] ?: return
        runCatching {
            subscription.handle(chainLog)
        }.onFailure { ex ->
            log.error(
                "TopazLifecycle listener handler failed event={} tx={} logIndex={}: {}",
                subscription.name,
                chainLog.transactionHash,
                chainLog.logIndex,
                ex.message,
                ex
            )
        }
    }

    private fun subscriptionFailed(error: Throwable) {
        log.error("TopazLifecycle listener subscription failed: {}", error.message, error)
    }

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

    private data class TopazEventSubscription(
        val name: String,
        val event: Event,
        val handle: (Log) -> Unit
    )

    private companion object {
        private val TOPAZ_LIFECYCLE_ABI_EVENTS = setOf(
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
    }
}
