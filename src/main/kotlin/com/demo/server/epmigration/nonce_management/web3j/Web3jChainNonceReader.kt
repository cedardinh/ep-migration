package com.demo.server.epmigration.nonce_management.web3j

import com.demo.server.epmigration.config.EpChainProperties
import com.demo.server.epmigration.nonce_management.allocation.ChainNonceIdentity
import com.demo.server.epmigration.nonce_management.allocation.ChainNonceReader
import com.demo.server.epmigration.nonce_management.allocation.ChainNonceSnapshot
import org.web3j.crypto.Credentials
import org.web3j.protocol.Web3j
import org.web3j.protocol.core.DefaultBlockParameterName
import java.math.BigInteger
import java.util.Locale

internal class Web3jChainNonceReader(
    private val web3j: Web3j,
    private val credentials: Credentials,
    chainProperties: EpChainProperties
) : ChainNonceReader {
    private val configuredChainId =
        BigInteger.valueOf(chainProperties.chainId)
    private val signerAddress =
        credentials.address.toLowerCase(Locale.ROOT)

    override fun read(): ChainNonceSnapshot {
        val actualChainId = web3j.ethChainId().send().let {
            check(!it.hasError()) {
                "eth_chainId failed: ${it.error?.message}"
            }
            it.chainId
        }
        check(actualChainId == configuredChainId) {
            "configured chainId $configuredChainId does not match " +
                "RPC chainId $actualChainId"
        }

        val genesisHash = web3j.ethGetBlockByNumber(
            DefaultBlockParameterName.EARLIEST,
            false
        ).send().let {
            check(!it.hasError()) {
                "genesis lookup failed: ${it.error?.message}"
            }
            it.block?.hash
                ?.toLowerCase(Locale.ROOT)
                ?: error("genesis block does not contain a hash")
        }

        val pendingNonce = web3j.ethGetTransactionCount(
            signerAddress,
            DefaultBlockParameterName.PENDING
        ).send().let {
            check(!it.hasError()) {
                "pending nonce lookup failed: ${it.error?.message}"
            }
            it.transactionCount
                ?: error("pending nonce lookup returned no value")
        }

        return ChainNonceSnapshot(
            ChainNonceIdentity(
                actualChainId,
                genesisHash,
                signerAddress
            ),
            pendingNonce
        )
    }
}
