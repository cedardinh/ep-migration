package com.demo.server.epmigration.config

import org.springframework.boot.context.properties.ConfigurationProperties
import java.math.BigInteger

@ConfigurationProperties(prefix = "ep.chain")
class EpChainProperties {
    var rpcUrl: String = "http://127.0.0.1:8546"
    var chainId: Long = 1337L
    var lifecycleContractAddress: String = ""
    var signerPrivateKey: String = ""
    var gasLimit: BigInteger = BigInteger.valueOf(5_000_000L)
}
