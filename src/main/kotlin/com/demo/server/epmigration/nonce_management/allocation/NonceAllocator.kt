package com.demo.server.epmigration.nonce_management.allocation

import java.math.BigInteger

internal data class ChainNonceIdentity(
    val chainId: BigInteger,
    val genesisHash: String,
    val signerAddress: String
)

internal data class ChainNonceSnapshot(
    val identity: ChainNonceIdentity,
    val pendingNonce: BigInteger
)

internal interface ChainNonceReader {
    fun read(): ChainNonceSnapshot
}

internal interface NonceCursorStore {
    fun allocate(
        identity: ChainNonceIdentity,
        pendingNonce: BigInteger
    ): BigInteger
}

internal class NonceAllocator(
    private val chain: ChainNonceReader,
    private val cursorStore: NonceCursorStore
) {
    fun nextNonce(): BigInteger {
        val snapshot = chain.read()
        return cursorStore.allocate(
            snapshot.identity,
            snapshot.pendingNonce
        )
    }
}
