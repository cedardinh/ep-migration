package com.demo.server.epmigration.ledger

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.web3j.abi.FunctionEncoder
import org.web3j.abi.TypeReference
import org.web3j.abi.datatypes.Address
import org.web3j.abi.datatypes.DynamicArray
import org.web3j.abi.datatypes.DynamicStruct
import org.web3j.abi.datatypes.Function
import org.web3j.abi.datatypes.Utf8String
import org.web3j.abi.datatypes.generated.Bytes32

class TopazTypesSelectorDebugTests {
    @Test
    fun `screenshot topaz types produce create project selector`() {
        val input = ScreenshotTopazTypes.CreateProjectInput(
            externalProjectId = "selector-debug",
            name = "Selector Debug",
            developer = ScreenshotTopazTypes.Participant(
                wallet = WALLET,
                legalName = "Developer Ltd",
                addressLine1 = "",
                addressLine2 = "",
                bic = "",
                lei = "",
                externalRef = "developer"
            ),
            mainContractors = listOf(
                ScreenshotTopazTypes.Participant(
                    wallet = WALLET,
                    legalName = "Contractor Ltd",
                    addressLine1 = "",
                    addressLine2 = "",
                    bic = "",
                    lei = "",
                    externalRef = "contractor"
                )
            ),
            claimApprovers = listOf(
                ScreenshotTopazTypes.ApproverConfig(
                    wallet = WALLET,
                    userHash = "0xf09b66dfb6bd1bb5e7d2be0b15a80542e02b79b94ea63cd7e918ac65b1164a9a".hexBytes(),
                    email = "claim@example.com",
                    firstName = "Claim",
                    lastName = "Approver",
                    userProfileName = "claim-approver",
                    roleName = "Claim Approver",
                    externalRef = "claim-approver"
                )
            ),
            paymentApprovers = listOf(
                ScreenshotTopazTypes.ApproverConfig(
                    wallet = WALLET,
                    userHash = "0x06a649d9b77f6b7a90a57443026f693d362b91ab6d64aac3557edef254d5efeb".hexBytes(),
                    email = "payment@example.com",
                    firstName = "Payment",
                    lastName = "Approver",
                    userProfileName = "payment-approver",
                    roleName = "Payment Approver",
                    externalRef = "payment-approver"
                )
            ),
            bankAccountRefs = listOf("bank-1")
        )

        val function = Function("createProject", listOf(input), emptyList<TypeReference<*>>())
        val calldata = FunctionEncoder.encode(function)

        assertEquals("0xcd9c2f36", calldata.take(10))
    }

    private object ScreenshotTopazTypes {
        class Participant(
            val wallet: String,
            val legalName: String,
            val addressLine1: String,
            val addressLine2: String,
            val bic: String,
            val lei: String,
            val externalRef: String
        ) : DynamicStruct(
            Address(wallet),
            Utf8String(legalName),
            Utf8String(addressLine1),
            Utf8String(addressLine2),
            Utf8String(bic),
            Utf8String(lei),
            Utf8String(externalRef)
        ) {
            constructor(
                wallet: Address,
                legalName: Utf8String,
                addressLine1: Utf8String,
                addressLine2: Utf8String,
                bic: Utf8String,
                lei: Utf8String,
                externalRef: Utf8String
            ) : this(
                wallet.value,
                legalName.value,
                addressLine1.value,
                addressLine2.value,
                bic.value,
                lei.value,
                externalRef.value
            )
        }

        class ApproverConfig(
            val wallet: String,
            val userHash: ByteArray,
            val email: String,
            val firstName: String,
            val lastName: String,
            val userProfileName: String,
            val roleName: String,
            val externalRef: String
        ) : DynamicStruct(
            Address(wallet),
            Bytes32(bytes32(userHash)),
            Utf8String(email),
            Utf8String(firstName),
            Utf8String(lastName),
            Utf8String(userProfileName),
            Utf8String(roleName),
            Utf8String(externalRef)
        ) {
            constructor(
                wallet: Address,
                userHash: Bytes32,
                email: Utf8String,
                firstName: Utf8String,
                lastName: Utf8String,
                userProfileName: Utf8String,
                roleName: Utf8String,
                externalRef: Utf8String
            ) : this(
                wallet.value,
                userHash.value,
                email.value,
                firstName.value,
                lastName.value,
                userProfileName.value,
                roleName.value,
                externalRef.value
            )
        }

        class CreateProjectInput(
            val externalProjectId: String,
            val name: String,
            val developer: Participant,
            val mainContractors: List<Participant>,
            val claimApprovers: List<ApproverConfig>,
            val paymentApprovers: List<ApproverConfig>,
            val bankAccountRefs: List<String>
        ) : DynamicStruct(
            Utf8String(externalProjectId),
            Utf8String(name),
            developer,
            DynamicArray(Participant::class.java, mainContractors),
            DynamicArray(ApproverConfig::class.java, claimApprovers),
            DynamicArray(ApproverConfig::class.java, paymentApprovers),
            strings(bankAccountRefs)
        )

        private fun strings(values: List<String>): DynamicArray<Utf8String> {
            return DynamicArray(Utf8String::class.java, values.map { Utf8String(it) })
        }

        private fun bytes32(value: ByteArray): ByteArray {
            require(value.size == 32) { "bytes32 value must be exactly 32 bytes" }
            return value
        }
    }

    companion object {
        private const val WALLET = "0x628d684197485c054cda7d3def46e8be6b3d174c"

        private fun String.hexBytes(): ByteArray {
            val clean = removePrefix("0x")
            return ByteArray(clean.length / 2) { index ->
                clean.substring(index * 2, index * 2 + 2).toInt(16).toByte()
            }
        }
    }
}
