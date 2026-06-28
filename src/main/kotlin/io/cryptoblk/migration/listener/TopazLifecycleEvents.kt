package io.cryptoblk.migration.listener

import org.web3j.abi.EventEncoder
import org.web3j.abi.TypeReference
import org.web3j.abi.datatypes.Address
import org.web3j.abi.datatypes.Event
import org.web3j.abi.datatypes.Utf8String
import org.web3j.abi.datatypes.generated.Bytes32
import org.web3j.abi.datatypes.generated.Uint256
import org.web3j.abi.datatypes.generated.Uint8
import org.web3j.protocol.Web3j
import org.web3j.protocol.core.methods.response.Log
import org.web3j.tx.TransactionManager
import org.web3j.tx.gas.ContractGasProvider
import java.math.BigInteger

object TopazLifecycleEvents {

    @Volatile
    private var bridge: ContractBridge? = null

    fun init(
        web3j: Web3j,
        txManager: TransactionManager,
        gasProvider: ContractGasProvider,
        contractAddress: String
    ) {
        bridge = ContractBridge(contractAddress, web3j, txManager, gasProvider)
    }

    // --- Project ---
    private val ProjectCreated = Event("ProjectCreated", listOf(
        TypeReference.create(Uint256::class.java, true),
        TypeReference.create(Utf8String::class.java),
        TypeReference.create(Address::class.java, true)
    ))
    private val ProjectUpdated = Event("ProjectUpdated", listOf(
        TypeReference.create(Uint256::class.java, true),
        TypeReference.create(Utf8String::class.java)
    ))
    private val ProjectStatusChanged = Event("ProjectStatusChanged", listOf(
        TypeReference.create(Uint256::class.java, true),
        TypeReference.create(Uint8::class.java)
    ))
    private val ProjectApproverRemoved = Event("ProjectApproverRemoved", listOf(
        TypeReference.create(Uint256::class.java, true),
        TypeReference.create(Bytes32::class.java, true)
    ))

    // --- Claim ---
    private val ClaimCreated = Event("ClaimCreated", listOf(
        TypeReference.create(Uint256::class.java, true),
        TypeReference.create(Uint256::class.java, true),
        TypeReference.create(Address::class.java, true),
        TypeReference.create(Uint8::class.java)
    ))
    private val ClaimStatusChanged = Event("ClaimStatusChanged", listOf(
        TypeReference.create(Uint256::class.java, true),
        TypeReference.create(Uint8::class.java)
    ))
    private val ClaimDocumentsUpdated = Event("ClaimDocumentsUpdated", listOf(
        TypeReference.create(Uint256::class.java, true),
        TypeReference.create(Uint256::class.java)
    ))

    // --- Invoice ---
    private val InvoiceCreated = Event("InvoiceCreated", listOf(
        TypeReference.create(Uint256::class.java, true),
        TypeReference.create(Uint256::class.java, true),
        TypeReference.create(Uint8::class.java)
    ))
    private val InvoiceStatusChanged = Event("InvoiceStatusChanged", listOf(
        TypeReference.create(Uint256::class.java, true),
        TypeReference.create(Uint8::class.java)
    ))
    private val InvoiceDocumentsUpdated = Event("InvoiceDocumentsUpdated", listOf(
        TypeReference.create(Uint256::class.java, true),
        TypeReference.create(Uint256::class.java)
    ))

    // --- PaymentOrder / Payment ---
    private val PaymentOrderCreated = Event("PaymentOrderCreated", listOf(
        TypeReference.create(Uint256::class.java, true),
        TypeReference.create(Uint256::class.java, true),
        TypeReference.create(Uint8::class.java)
    ))
    private val PaymentOrderStatusChanged = Event("PaymentOrderStatusChanged", listOf(
        TypeReference.create(Uint256::class.java, true),
        TypeReference.create(Uint8::class.java)
    ))
    private val PaymentCreatedForOrder = Event("PaymentCreatedForOrder", listOf(
        TypeReference.create(Uint256::class.java, true),
        TypeReference.create(Uint256::class.java, true),
        TypeReference.create(Uint256::class.java, true)
    ))
    private val BankPaymentRequested = Event("BankPaymentRequested", listOf(
        TypeReference.create(Uint256::class.java, true),
        TypeReference.create(Uint256::class.java, true),
        TypeReference.create(Utf8String::class.java)
    ))
    private val BankPaymentReferenceRecorded = Event("BankPaymentReferenceRecorded", listOf(
        TypeReference.create(Uint256::class.java, true),
        TypeReference.create(Utf8String::class.java)
    ))

    private val byTopic0: Map<String, Event> = listOf(
        ProjectCreated, ProjectUpdated, ProjectStatusChanged, ProjectApproverRemoved,
        ClaimCreated, ClaimStatusChanged, ClaimDocumentsUpdated,
        InvoiceCreated, InvoiceStatusChanged, InvoiceDocumentsUpdated,
        PaymentOrderCreated, PaymentOrderStatusChanged, PaymentCreatedForOrder,
        BankPaymentRequested, BankPaymentReferenceRecorded
    ).associateBy { EventEncoder.encode(it) }

    fun decode(log: Log): RoutedEvent? {
        val topic0 = log.topics?.firstOrNull() ?: return null
        val event = byTopic0[topic0] ?: return null

        val ev = bridge?.extract(event, log) ?: return null

        fun b32ToHex(b: ByteArray) = "0x" + b.joinToString("") { "%02x".format(it) }
        fun uint8ToInt(v: Any) = (v as BigInteger).toInt()

        val meta = ChainMeta(
            txHash = log.transactionHash,
            blockNumber = log.blockNumber,
            logIndex = log.logIndex
        )

        val parsed: TopazLifecycleEvent = when (event.name) {
            "ProjectCreated" -> TopazLifecycleEvent.ProjectCreated(
                projectId = ev.indexedValues[0].value as BigInteger,
                externalProjectId = ev.nonIndexedValues[0].value as String,
                developerWallet = ev.indexedValues[1].value as String
            )
            "ProjectUpdated" -> TopazLifecycleEvent.ProjectUpdated(
                projectId = ev.indexedValues[0].value as BigInteger,
                externalProjectId = ev.nonIndexedValues[0].value as String
            )
            "ProjectStatusChanged" -> TopazLifecycleEvent.ProjectStatusChanged(
                projectId = ev.indexedValues[0].value as BigInteger,
                statusCode = uint8ToInt(ev.nonIndexedValues[0].value)
            )
            "ProjectApproverRemoved" -> TopazLifecycleEvent.ProjectApproverRemoved(
                projectId = ev.indexedValues[0].value as BigInteger,
                userHashHex = b32ToHex(ev.indexedValues[1].value as ByteArray)
            )

            "ClaimCreated" -> TopazLifecycleEvent.ClaimCreated(
                claimId = ev.indexedValues[0].value as BigInteger,
                projectId = ev.indexedValues[1].value as BigInteger,
                contractorWallet = ev.indexedValues[2].value as String,
                statusCode = uint8ToInt(ev.nonIndexedValues[0].value)
            )
            "ClaimStatusChanged" -> TopazLifecycleEvent.ClaimStatusChanged(
                claimId = ev.indexedValues[0].value as BigInteger,
                statusCode = uint8ToInt(ev.nonIndexedValues[0].value)
            )
            "ClaimDocumentsUpdated" -> TopazLifecycleEvent.ClaimDocumentsUpdated(
                claimId = ev.indexedValues[0].value as BigInteger,
                documentCount = ev.nonIndexedValues[0].value as BigInteger
            )

            "InvoiceCreated" -> TopazLifecycleEvent.InvoiceCreated(
                invoiceId = ev.indexedValues[0].value as BigInteger,
                claimId = ev.indexedValues[1].value as BigInteger,
                statusCode = uint8ToInt(ev.nonIndexedValues[0].value)
            )
            "InvoiceStatusChanged" -> TopazLifecycleEvent.InvoiceStatusChanged(
                invoiceId = ev.indexedValues[0].value as BigInteger,
                statusCode = uint8ToInt(ev.nonIndexedValues[0].value)
            )
            "InvoiceDocumentsUpdated" -> TopazLifecycleEvent.InvoiceDocumentsUpdated(
                invoiceId = ev.indexedValues[0].value as BigInteger,
                documentCount = ev.nonIndexedValues[0].value as BigInteger
            )

            "PaymentOrderCreated" -> TopazLifecycleEvent.PaymentOrderCreated(
                paymentOrderId = ev.indexedValues[0].value as BigInteger,
                invoiceId = ev.indexedValues[1].value as BigInteger,
                statusCode = uint8ToInt(ev.nonIndexedValues[0].value)
            )
            "PaymentOrderStatusChanged" -> TopazLifecycleEvent.PaymentOrderStatusChanged(
                paymentOrderId = ev.indexedValues[0].value as BigInteger,
                statusCode = uint8ToInt(ev.nonIndexedValues[0].value)
            )
            "PaymentCreatedForOrder" -> TopazLifecycleEvent.PaymentCreatedForOrder(
                paymentOrderId = ev.indexedValues[0].value as BigInteger,
                paymentId = ev.indexedValues[1].value as BigInteger,
                invoiceId = ev.indexedValues[2].value as BigInteger
            )
            "BankPaymentRequested" -> TopazLifecycleEvent.BankPaymentRequested(
                paymentOrderId = ev.indexedValues[0].value as BigInteger,
                invoiceId = ev.indexedValues[1].value as BigInteger,
                customerRefNumber = ev.nonIndexedValues[0].value as String
            )
            "BankPaymentReferenceRecorded" -> TopazLifecycleEvent.BankPaymentReferenceRecorded(
                paymentOrderId = ev.indexedValues[0].value as BigInteger,
                bankPaymentRef = ev.nonIndexedValues[0].value as String
            )

            else -> return null
        }

        return RoutedEvent(meta = meta, event = parsed)
    }
}
