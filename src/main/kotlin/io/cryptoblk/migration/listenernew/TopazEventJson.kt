package io.cryptoblk.migration.listenernew

import com.demo.server.epmigration.chain.generated.TopazLifecycle
import org.web3j.utils.Numeric

internal fun TopazLifecycle.Participant.toJson(): Map<String, String> {
    return mapOf(
        "wallet" to wallet,
        "legalName" to legalName,
        "addressLine1" to addressLine1,
        "addressLine2" to addressLine2,
        "bic" to bic,
        "lei" to lei,
        "externalRef" to externalRef
    )
}

internal fun TopazLifecycle.ApproverConfig.toJson(): Map<String, String> {
    return mapOf(
        "wallet" to wallet,
        "userHash" to Numeric.toHexString(userHash),
        "roleName" to roleName,
        "externalRef" to externalRef
    )
}
