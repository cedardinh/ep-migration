package com.demo.server.epmigration.nonce_management.web3j

import com.demo.server.epmigration.nonce_management.allocation.NonceAllocator
import org.web3j.crypto.Credentials
import org.web3j.protocol.Web3j
import org.web3j.tx.RawTransactionManager
import java.math.BigInteger

internal class ManagedNonceTransactionManager(
    web3j: Web3j,
    credentials: Credentials,
    chainId: Long,
    private val nonceAllocator: NonceAllocator
) : RawTransactionManager(web3j, credentials, chainId) {
    protected override fun getNonce(): BigInteger {
        return nonceAllocator.nextNonce()
    }
}
