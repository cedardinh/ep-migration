package com.demo.server.epmigration.config

import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.web3j.crypto.Credentials
import org.web3j.protocol.Web3j
import org.web3j.protocol.http.HttpService
import org.web3j.utils.Numeric

@Configuration
@EnableConfigurationProperties(EpChainProperties::class)
class Web3jConfig {
    private val privateKeyPattern = Regex("^(0x)?[0-9a-fA-F]{64}$")

    @Bean
    fun web3j(properties: EpChainProperties): Web3j {
        return Web3j.build(HttpService(properties.rpcUrl))
    }

    @Bean
    fun chainCredentials(properties: EpChainProperties): Credentials {
        val privateKey = properties.signerPrivateKey.trim()
        if (!privateKeyPattern.matches(privateKey)) {
            throw IllegalStateException("ep.chain.signer-private-key must be a 32-byte hex private key")
        }
        return Credentials.create(Numeric.cleanHexPrefix(privateKey))
    }
}
