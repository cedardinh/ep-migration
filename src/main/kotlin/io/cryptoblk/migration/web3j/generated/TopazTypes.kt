package io.cryptoblk.migration.web3j.generated

class TopazTypes {
    data class Participant(
        val wallet: String,
        val legalName: String,
        val addressLine1: String,
        val addressLine2: String,
        val bic: String,
        val lei: String,
        val externalRef: String
    )

    data class ApproverConfig(
        val wallet: String,
        val userHash: ByteArray,
        val roleName: String,
        val externalRef: String
    )
}
