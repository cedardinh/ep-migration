package com.demo.server.epmigration.project.dto

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import com.demo.server.epmigration.chain.generated.TopazLifecycle
import org.web3j.utils.Numeric

class ApproverRequest @JsonCreator constructor(
    @JsonProperty("wallet") wallet: String = "",
    @JsonProperty("userHash") userHash: String = "",
    @JsonProperty("roleName") roleName: String = "",
    @JsonProperty("externalRef") externalRef: String = ""
) : TopazLifecycle.ApproverConfig(
    abiApproverAddress(wallet),
    abiBytes32(userHash),
    roleName,
    externalRef
)

private const val ZERO_APPROVER_ADDRESS = "0x0000000000000000000000000000000000000000"
private val approverAddressPattern = Regex("^0x[0-9a-fA-F]{40}$")
private val bytes32Pattern = Regex("^0x[0-9a-fA-F]{64}$")

private fun abiApproverAddress(value: String): String {
    return if (approverAddressPattern.matches(value)) value else ZERO_APPROVER_ADDRESS
}

private fun abiBytes32(value: String): ByteArray {
    return if (bytes32Pattern.matches(value)) Numeric.hexStringToByteArray(value) else ByteArray(32)
}
