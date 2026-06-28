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
            .on(TopazLifecycleEvent.ClaimCreated::class, handlers::onClaimCreated)
            .on(TopazLifecycleEvent.InvoiceCreated::class, handlers::onInvoiceCreated)
            .on(TopazLifecycleEvent.BankPaymentRequested::class, handlers::onBankPaymentRequested)
            .on(TopazLifecycleEvent.BankPaymentReferenceRecorded::class, handlers::onBankPaymentReferenceRecorded)
    }
}
