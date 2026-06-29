package io.cryptoblk.migration.listenernew

import java.util.Locale

data class TopazContractAddresses(
    val lifecycle: String,
    val payment: String,
    val contacts: String
) {
    fun all(): List<String> {
        return listOf(lifecycle, payment, contacts)
    }

    companion object {
        const val LIFECYCLE = "lifecycle"
        const val PAYMENT = "payment"
        const val CONTACTS = "contacts"

        internal fun normalize(address: String): String {
            return address.trim().toLowerCase(Locale.US)
        }
    }
}
