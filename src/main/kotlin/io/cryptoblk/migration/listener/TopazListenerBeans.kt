package io.cryptoblk.migration.listener

import com.demo.server.epmigration.config.EpChainProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.web3j.crypto.Credentials
import org.web3j.protocol.Web3j
import org.web3j.tx.RawTransactionManager
import org.web3j.tx.gas.StaticGasProvider

@Configuration
class TopazListenerBeans {

    @Bean
    fun topazLifecycleEventRouter(
        web3j: Web3j,
        props: EpChainProperties,
        credentials: Credentials,
        handlers: TopazLifecycleHandlers
    ): TopazLifecycleEventRouter {
        val contractAddress = props.lifecycleContractAddress.trim()
        TopazLifecycleEvents.init(
            web3j = web3j,
            txManager = RawTransactionManager(web3j, credentials, props.chainId),
            gasProvider = StaticGasProvider(props.gasPrice, props.gasLimit),
            contractAddress = contractAddress
        )

        return TopazLifecycleEventRouter(web3j, contractAddress)
            .on(TopazLifecycleEvent.ProjectCreated::class, handlers::onProjectCreated)
            .on(TopazLifecycleEvent.ProjectUpdated::class, handlers::onProjectUpdated)
            .on(TopazLifecycleEvent.ProjectStatusChanged::class, handlers::onProjectStatusChanged)
            .on(TopazLifecycleEvent.ProjectApproverRemoved::class, handlers::onProjectApproverRemoved)
            .on(TopazLifecycleEvent.ClaimCreated::class, handlers::onClaimCreated)
            .on(TopazLifecycleEvent.ClaimStatusChanged::class, handlers::onClaimStatusChanged)
            .on(TopazLifecycleEvent.ClaimDocumentsUpdated::class, handlers::onClaimDocumentsUpdated)
            .on(TopazLifecycleEvent.InvoiceCreated::class, handlers::onInvoiceCreated)
            .on(TopazLifecycleEvent.InvoiceStatusChanged::class, handlers::onInvoiceStatusChanged)
            .on(TopazLifecycleEvent.InvoiceDocumentsUpdated::class, handlers::onInvoiceDocumentsUpdated)
            .on(TopazLifecycleEvent.PaymentOrderCreated::class, handlers::onPaymentOrderCreated)
            .on(TopazLifecycleEvent.PaymentOrderStatusChanged::class, handlers::onPaymentOrderStatusChanged)
            .on(TopazLifecycleEvent.PaymentCreatedForOrder::class, handlers::onPaymentCreatedForOrder)
            .on(TopazLifecycleEvent.BankPaymentRequested::class, handlers::onBankPaymentRequested)
            .on(TopazLifecycleEvent.BankPaymentReferenceRecorded::class, handlers::onBankPaymentReferenceRecorded)
            .on(TopazLifecycleEvent.RoleAdminChanged::class, handlers::onRoleAdminChanged)
            .on(TopazLifecycleEvent.RoleGranted::class, handlers::onRoleGranted)
            .on(TopazLifecycleEvent.RoleRevoked::class, handlers::onRoleRevoked)
    }
}
