package com.demo.server.epmigration.project.dto

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import com.demo.server.epmigration.chain.generated.TopazLifecycle

class ParticipantRequest @JsonCreator constructor(
    @JsonProperty("wallet") wallet: String = "",
    @JsonProperty("legalName") legalName: String = "",
    @JsonProperty("addressLine1") addressLine1: String = "",
    @JsonProperty("addressLine2") addressLine2: String = "",
    @JsonProperty("bic") bic: String = "",
    @JsonProperty("lei") lei: String = "",
    @JsonProperty("externalRef") externalRef: String = ""
) : TopazLifecycle.Participant(
    abiAddress(wallet),
    legalName,
    addressLine1,
    addressLine2,
    bic,
    lei,
    externalRef
) {
    val rawWallet: String = wallet
}

private const val ZERO_ADDRESS = "0x0000000000000000000000000000000000000000"
private val addressPattern = Regex("^0x[0-9a-fA-F]{40}$")

private fun abiAddress(value: String): String {
    return if (addressPattern.matches(value)) value else ZERO_ADDRESS
}
