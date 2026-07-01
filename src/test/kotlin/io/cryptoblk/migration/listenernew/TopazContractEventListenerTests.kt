package io.cryptoblk.migration.listenernew

import com.demo.server.epmigration.config.EpChainProperties
import io.reactivex.Flowable
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.ArgumentCaptor
import org.mockito.Mockito
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.TransactionDefinition
import org.springframework.transaction.TransactionStatus
import org.springframework.transaction.support.SimpleTransactionStatus
import org.web3j.abi.FunctionEncoder
import org.web3j.abi.TypeEncoder
import org.web3j.abi.datatypes.Type
import org.web3j.abi.datatypes.generated.Uint256
import org.web3j.abi.datatypes.generated.Uint8
import org.web3j.protocol.Web3j
import org.web3j.protocol.core.Request
import org.web3j.protocol.core.methods.request.EthFilter
import org.web3j.protocol.core.methods.response.EthBlockNumber
import org.web3j.protocol.core.methods.response.EthLog
import org.web3j.protocol.core.methods.response.Log
import java.math.BigInteger
import java.util.Optional
import java.util.concurrent.atomic.AtomicBoolean

class TopazContractEventListenerTests {

    @Test
    fun `fresh compensation initializes checkpoint at latest block`() {
        val web3j = Mockito.mock(Web3j::class.java)
        val checkpointRepository = RecordingCheckpointRepository()
        val listener = listener(web3j, checkpointRepository)
        mockBlockNumber(web3j, BigInteger.TEN)

        val scanned = listener.compensateMissingEvents()

        assertEquals(0, scanned)
        val checkpoint = captureSavedCheckpoint(checkpointRepository)
        assertEquals(LISTENER_NAME, checkpoint.listenerName)
        assertEquals(CHECKPOINT_IDENTITY, checkpoint.identity())
        assertEquals(BigInteger.TEN, checkpoint.processedBlock)
        assertNull(checkpoint.processedTransactionHash)
        assertNull(checkpoint.processedLogIndex)
        Mockito.verify(web3j, Mockito.never()).ethGetLogs(Mockito.any(EthFilter::class.java))
    }

    @Test
    fun `fresh compensation backfills from configured listener block`() {
        val web3j = Mockito.mock(Web3j::class.java)
        val checkpointRepository = RecordingCheckpointRepository()
        val listener = listener(
            web3j,
            checkpointRepository,
            chainProperties = EpChainProperties().apply {
                listenerFromBlock = BigInteger.valueOf(5L)
            }
        )
        mockBlockNumber(web3j, BigInteger.TEN)
        mockLogs(web3j)

        val scanned = listener.compensateMissingEvents()

        assertEquals(0, scanned)
        val filter = captureLogFilter(web3j)
        assertEquals("0x5", filter.fromBlock.value)
        assertEquals("0xa", filter.toBlock.value)
        val checkpoint = captureSavedCheckpoint(checkpointRepository)
        assertEquals(LISTENER_NAME, checkpoint.listenerName)
        assertEquals(CHECKPOINT_IDENTITY, checkpoint.identity())
        assertEquals(BigInteger.TEN, checkpoint.processedBlock)
        assertNull(checkpoint.processedTransactionHash)
        assertNull(checkpoint.processedLogIndex)
    }

    @Test
    fun `fresh compensation ignores checkpoint from previous contract deployment`() {
        val web3j = Mockito.mock(Web3j::class.java)
        val previousDeploymentIdentity = CHECKPOINT_IDENTITY.copy(
            lifecycleContractAddress = "0x0000000000000000000000000000000000000009"
        )
        val checkpointRepository = RecordingCheckpointRepository(
            Optional.of(
                checkpoint(
                    processedBlock = BigInteger.valueOf(8L),
                    identity = previousDeploymentIdentity
                )
            )
        )
        val listener = listener(web3j, checkpointRepository)
        mockBlockNumber(web3j, BigInteger.TEN)

        val scanned = listener.compensateMissingEvents()

        assertEquals(0, scanned)
        Mockito.verify(web3j, Mockito.never()).ethGetLogs(Mockito.any(EthFilter::class.java))
        val checkpoint = captureSavedCheckpoint(checkpointRepository)
        assertEquals(CHECKPOINT_IDENTITY, checkpoint.identity())
        assertEquals(BigInteger.TEN, checkpoint.processedBlock)
        assertNull(checkpoint.processedTransactionHash)
        assertNull(checkpoint.processedLogIndex)
    }

    @Test
    fun `start fails and stops subscription when startup compensation fails`() {
        val web3j = Mockito.mock(Web3j::class.java)
        val checkpointRepository = RecordingCheckpointRepository()
        val listener = listener(
            web3j,
            checkpointRepository,
            chainProperties = EpChainProperties().apply {
                listenerFromBlock = BigInteger.valueOf(11L)
            }
        )
        val subscriptionCancelled = mockSubscription(web3j)
        mockBlockNumber(web3j, BigInteger.TEN)

        val error = assertThrows(IllegalArgumentException::class.java) {
            listener.start()
        }

        assertEquals("ep.chain.listener-from-block must be less than or equal to latest block 10", error.message)
        assertTrue(!listener.isRunning)
        assertTrue(subscriptionCancelled.get())
        assertTrue(checkpointRepository.saved.isEmpty())
    }

    @Test
    fun `listener rejects negative configured listener block`() {
        val web3j = Mockito.mock(Web3j::class.java)
        val checkpointRepository = RecordingCheckpointRepository()

        val error = assertThrows(IllegalArgumentException::class.java) {
            listener(
                web3j,
                checkpointRepository,
                chainProperties = EpChainProperties().apply {
                    listenerFromBlock = BigInteger.valueOf(-1L)
                }
            )
        }

        assertEquals("ep.chain.listener-from-block must be non-negative", error.message)
        assertTrue(checkpointRepository.saved.isEmpty())
    }

    @Test
    fun `compensation scans from persisted checkpoint plus one`() {
        val web3j = Mockito.mock(Web3j::class.java)
        val checkpointRepository = RecordingCheckpointRepository(
            Optional.of(checkpoint(processedBlock = BigInteger.valueOf(8L)))
        )
        val listener = listener(web3j, checkpointRepository)
        mockBlockNumber(web3j, BigInteger.TEN)
        mockLogs(web3j)

        val scanned = listener.compensateMissingEvents()

        assertEquals(0, scanned)
        val filter = captureLogFilter(web3j)
        assertEquals("0x9", filter.fromBlock.value)
        assertEquals("0xa", filter.toBlock.value)
        val checkpoint = captureSavedCheckpoint(checkpointRepository)
        assertEquals(LISTENER_NAME, checkpoint.listenerName)
        assertEquals(BigInteger.TEN, checkpoint.processedBlock)
        assertNull(checkpoint.processedTransactionHash)
        assertNull(checkpoint.processedLogIndex)
    }

    @Test
    fun `compensation resumes inside checkpoint block and skips already processed logs`() {
        val web3j = Mockito.mock(Web3j::class.java)
        val checkpointRepository = RecordingCheckpointRepository(
            Optional.of(
                checkpoint(
                    processedBlock = BigInteger.valueOf(8L),
                    processedTransactionHash = TX_HASH_1,
                    processedLogIndex = BigInteger.ONE
                )
            )
        )
        val listener = listener(web3j, checkpointRepository)
        mockBlockNumber(web3j, BigInteger.TEN)
        mockLogs(
            web3j,
            listOf(
                projectStatusChangedLog(txHash = TX_HASH_1, logIndex = BigInteger.ONE),
                projectStatusChangedLog(txHash = TX_HASH_2, logIndex = BigInteger.valueOf(2L))
            )
        )

        val scanned = listener.compensateMissingEvents()

        assertEquals(1, scanned)
        val filter = captureLogFilter(web3j)
        assertEquals("0x8", filter.fromBlock.value)
        assertEquals("0xa", filter.toBlock.value)
        assertEquals(2, checkpointRepository.saved.size)
        val eventCheckpoint = checkpointRepository.saved[0]
        assertEquals(CHECKPOINT_IDENTITY, eventCheckpoint.identity())
        assertEquals(BigInteger.valueOf(8L), eventCheckpoint.processedBlock)
        assertEquals(TX_HASH_2, eventCheckpoint.processedTransactionHash)
        assertEquals(BigInteger.valueOf(2L), eventCheckpoint.processedLogIndex)
        val blockCheckpoint = checkpointRepository.saved[1]
        assertEquals(CHECKPOINT_IDENTITY, blockCheckpoint.identity())
        assertEquals(BigInteger.TEN, blockCheckpoint.processedBlock)
        assertNull(blockCheckpoint.processedTransactionHash)
        assertNull(blockCheckpoint.processedLogIndex)
    }

    @Test
    fun `scan saves event cursor before marking block complete`() {
        val web3j = Mockito.mock(Web3j::class.java)
        val checkpointRepository = RecordingCheckpointRepository()
        val listener = listener(web3j, checkpointRepository)
        mockLogs(
            web3j,
            listOf(
                projectStatusChangedLog(
                    blockNumber = BigInteger.valueOf(8L),
                    txHash = TX_HASH_2,
                    logIndex = BigInteger.valueOf(4L)
                )
            )
        )

        val scanned = listener.scanEvents(BigInteger.valueOf(8L), BigInteger.valueOf(8L))

        assertEquals(1, scanned)
        assertEquals(2, checkpointRepository.saved.size)
        val eventCheckpoint = checkpointRepository.saved[0]
        assertEquals(CHECKPOINT_IDENTITY, eventCheckpoint.identity())
        assertEquals(BigInteger.valueOf(8L), eventCheckpoint.processedBlock)
        assertEquals(TX_HASH_2, eventCheckpoint.processedTransactionHash)
        assertEquals(BigInteger.valueOf(4L), eventCheckpoint.processedLogIndex)
        val blockCheckpoint = checkpointRepository.saved[1]
        assertEquals(CHECKPOINT_IDENTITY, blockCheckpoint.identity())
        assertEquals(BigInteger.valueOf(8L), blockCheckpoint.processedBlock)
        assertNull(blockCheckpoint.processedTransactionHash)
        assertNull(blockCheckpoint.processedLogIndex)
    }

    @Test
    fun `scan writes event and block checkpoints inside transactions`() {
        val web3j = Mockito.mock(Web3j::class.java)
        val transactionManager = RecordingTransactionManager()
        val checkpointRepository = RecordingCheckpointRepository(transactionManager = transactionManager)
        val listener = listener(web3j, checkpointRepository, transactionManager = transactionManager)
        mockLogs(
            web3j,
            listOf(
                projectStatusChangedLog(
                    blockNumber = BigInteger.valueOf(8L),
                    txHash = TX_HASH_2,
                    logIndex = BigInteger.valueOf(4L)
                )
            )
        )

        val scanned = listener.scanEvents(BigInteger.valueOf(8L), BigInteger.valueOf(8L))

        assertEquals(1, scanned)
        assertEquals(2, checkpointRepository.saved.size)
        assertEquals(listOf(true, true), checkpointRepository.savedWithinTransaction)
        assertEquals(2, transactionManager.commits)
        assertEquals(0, transactionManager.rollbacks)
    }

    @Test
    fun `scan stops at failed event without advancing checkpoint`() {
        val web3j = Mockito.mock(Web3j::class.java)
        val checkpointRepository = RecordingCheckpointRepository()
        val listener = listener(web3j, checkpointRepository)
        mockLogs(
            web3j,
            listOf(
                projectStatusChangedLog(data = "0x", logIndex = BigInteger.ONE),
                projectStatusChangedLog(txHash = TX_HASH_2, logIndex = BigInteger.valueOf(2L))
            )
        )

        val scanned = listener.scanEvents(BigInteger.valueOf(8L), BigInteger.valueOf(8L))

        assertEquals(1, scanned)
        assertTrue(checkpointRepository.saved.isEmpty())
    }

    @Test
    fun `checkpoint save failure rolls back event transaction and stops scan`() {
        val web3j = Mockito.mock(Web3j::class.java)
        val transactionManager = RecordingTransactionManager()
        val checkpointRepository = RecordingCheckpointRepository(
            transactionManager = transactionManager,
            failOnSave = true
        )
        val listener = listener(web3j, checkpointRepository, transactionManager = transactionManager)
        mockLogs(
            web3j,
            listOf(
                projectStatusChangedLog(logIndex = BigInteger.ONE),
                projectStatusChangedLog(txHash = TX_HASH_2, logIndex = BigInteger.valueOf(2L))
            )
        )

        val scanned = listener.scanEvents(BigInteger.valueOf(8L), BigInteger.valueOf(8L))

        assertEquals(1, scanned)
        assertTrue(checkpointRepository.saved.isEmpty())
        assertEquals(listOf(true), checkpointRepository.savedWithinTransaction)
        assertEquals(0, transactionManager.commits)
        assertEquals(1, transactionManager.rollbacks)
    }

    @Test
    fun `scan stops when processed event lacks checkpoint coordinates`() {
        val web3j = Mockito.mock(Web3j::class.java)
        val checkpointRepository = RecordingCheckpointRepository()
        val listener = listener(web3j, checkpointRepository)
        mockLogs(
            web3j,
            listOf(
                projectStatusChangedLog(logIndex = null),
                projectStatusChangedLog(txHash = TX_HASH_2, logIndex = BigInteger.valueOf(2L))
            )
        )

        val scanned = listener.scanEvents(BigInteger.valueOf(8L), BigInteger.valueOf(8L))

        assertEquals(1, scanned)
        assertTrue(checkpointRepository.saved.isEmpty())
    }

    @Test
    fun `empty reversed scan range does not call chain`() {
        val web3j = Mockito.mock(Web3j::class.java)
        val checkpointRepository = RecordingCheckpointRepository()
        val listener = listener(web3j, checkpointRepository)

        val scanned = listener.scanEvents(BigInteger.TEN, BigInteger.ONE)

        assertEquals(0, scanned)
        Mockito.verify(web3j, Mockito.never()).ethGetLogs(Mockito.any(EthFilter::class.java))
        assertTrue(checkpointRepository.saved.isEmpty())
    }

    private fun listener(
        web3j: Web3j,
        checkpointRepository: TopazEventCheckpointRepository,
        chainProperties: EpChainProperties = EpChainProperties(),
        transactionManager: PlatformTransactionManager = RecordingTransactionManager()
    ): TopazContractEventListener {
        return TopazContractEventListener(
            web3j = web3j,
            lifecycleContractAddress = LIFECYCLE_ADDRESS,
            paymentContractAddress = PAYMENT_ADDRESS,
            contactsContractAddress = CONTACTS_ADDRESS,
            workflow = TopazWorkflowService(),
            checkpointRepository = checkpointRepository,
            chainProperties = chainProperties,
            transactionManager = transactionManager
        )
    }

    private fun captureSavedCheckpoint(
        checkpointRepository: RecordingCheckpointRepository
    ): TopazEventCheckpointEntity {
        return checkpointRepository.saved.single()
    }

    private fun checkpoint(
        processedBlock: BigInteger,
        processedTransactionHash: String? = null,
        processedLogIndex: BigInteger? = null,
        identity: TopazEventCheckpointIdentity = CHECKPOINT_IDENTITY
    ): TopazEventCheckpointEntity {
        return TopazEventCheckpointEntity(
            listenerName = identity.listenerName,
            processedBlock = processedBlock,
            processedTransactionHash = processedTransactionHash,
            processedLogIndex = processedLogIndex,
            chainId = identity.chainId,
            lifecycleContractAddress = identity.lifecycleContractAddress,
            paymentContractAddress = identity.paymentContractAddress,
            contactsContractAddress = identity.contactsContractAddress
        )
    }

    @Suppress("UNCHECKED_CAST")
    private fun mockBlockNumber(web3j: Web3j, blockNumber: BigInteger) {
        val request = Mockito.mock(Request::class.java) as Request<Any, EthBlockNumber>
        val response = EthBlockNumber().apply {
            result = "0x${blockNumber.toString(16)}"
        }
        Mockito.`when`(web3j.ethBlockNumber()).thenReturn(request)
        Mockito.`when`(request.send()).thenReturn(response)
    }

    @Suppress("UNCHECKED_CAST")
    private fun mockLogs(web3j: Web3j, logs: List<EthLog.LogObject> = emptyList()) {
        val request = Mockito.mock(Request::class.java) as Request<Any, EthLog>
        val response = EthLog().apply {
            setResult(logs)
        }
        Mockito.`when`(web3j.ethGetLogs(Mockito.any(EthFilter::class.java))).thenReturn(request)
        Mockito.`when`(request.send()).thenReturn(response)
    }

    private fun captureLogFilter(web3j: Web3j): EthFilter {
        val captor = ArgumentCaptor.forClass(EthFilter::class.java)
        Mockito.verify(web3j).ethGetLogs(captor.capture())
        return captor.value
    }

    private fun mockSubscription(web3j: Web3j): AtomicBoolean {
        val subscriptionCancelled = AtomicBoolean(false)
        Mockito.`when`(web3j.ethLogFlowable(Mockito.any(EthFilter::class.java)))
            .thenReturn(Flowable.never<Log>().doOnCancel { subscriptionCancelled.set(true) })
        return subscriptionCancelled
    }

    private fun projectStatusChangedLog(
        blockNumber: BigInteger = BigInteger.valueOf(8L),
        txHash: String = TX_HASH_1,
        transactionIndex: BigInteger = BigInteger.ZERO,
        logIndex: BigInteger? = BigInteger.ONE,
        data: String = data(Uint8(BigInteger.valueOf(3L)))
    ): EthLog.LogObject {
        val subscription = TopazEventRegistry.subscriptions(
            lifecycleAddress = LIFECYCLE_ADDRESS,
            paymentAddress = PAYMENT_ADDRESS,
            contactsAddress = CONTACTS_ADDRESS,
            workflow = TopazWorkflowService()
        ).single { it.contractName == "lifecycle" && it.eventName == "ProjectStatusChanged" }

        return EthLog.LogObject().apply {
            setAddress(LIFECYCLE_ADDRESS)
            setTopics(
                listOf(
                    subscription.topic0,
                    topic(Uint256(BigInteger.valueOf(7L)))
                )
            )
            setData(data)
            setTransactionHash(txHash)
            setTransactionIndex("0x${transactionIndex.toString(16)}")
            setBlockNumber("0x${blockNumber.toString(16)}")
            logIndex?.let { setLogIndex("0x${it.toString(16)}") }
        }
    }

    private fun topic(value: Type<*>): String {
        return "0x" + TypeEncoder.encode(value)
    }

    private fun data(vararg values: Type<*>): String {
        return FunctionEncoder.encodeConstructor(values.toList())
    }

    private companion object {
        private const val LIFECYCLE_ADDRESS = "0x0000000000000000000000000000000000000001"
        private const val PAYMENT_ADDRESS = "0x0000000000000000000000000000000000000002"
        private const val CONTACTS_ADDRESS = "0x0000000000000000000000000000000000000003"
        private const val LISTENER_NAME = "topaz-contract-event-listener"
        private const val TX_HASH_1 = "0x0000000000000000000000000000000000000000000000000000000000000001"
        private const val TX_HASH_2 = "0x0000000000000000000000000000000000000000000000000000000000000002"
        private val CHECKPOINT_IDENTITY = TopazEventCheckpointIdentity(
            listenerName = LISTENER_NAME,
            chainId = 1337L,
            lifecycleContractAddress = LIFECYCLE_ADDRESS,
            paymentContractAddress = PAYMENT_ADDRESS,
            contactsContractAddress = CONTACTS_ADDRESS
        )
    }

    private class RecordingCheckpointRepository(
        private val current: Optional<TopazEventCheckpointEntity> = Optional.empty(),
        private val transactionManager: RecordingTransactionManager? = null,
        private val failOnSave: Boolean = false
    ) : TopazEventCheckpointRepository {
        val saved = mutableListOf<TopazEventCheckpointEntity>()
        val savedWithinTransaction = mutableListOf<Boolean>()
        private val currentByIdentity = current
            .map { mapOf(it.identity() to it) }
            .orElse(emptyMap())

        override fun findByIdentity(identity: TopazEventCheckpointIdentity): Optional<TopazEventCheckpointEntity> {
            return Optional.ofNullable(currentByIdentity[identity])
        }

        override fun save(entity: TopazEventCheckpointEntity): TopazEventCheckpointEntity {
            savedWithinTransaction.add(transactionManager?.active == true)
            if (failOnSave) {
                error("checkpoint save failed")
            }
            saved.add(entity)
            return entity
        }
    }

    private class RecordingTransactionManager : PlatformTransactionManager {
        var active = false
            private set
        var commits = 0
            private set
        var rollbacks = 0
            private set

        override fun getTransaction(definition: TransactionDefinition?): TransactionStatus {
            check(!active) { "Nested transactions are not expected in listener tests" }
            active = true
            return SimpleTransactionStatus()
        }

        override fun commit(status: TransactionStatus) {
            check(active) { "Cannot commit without an active transaction" }
            commits += 1
            active = false
        }

        override fun rollback(status: TransactionStatus) {
            check(active) { "Cannot rollback without an active transaction" }
            rollbacks += 1
            active = false
        }
    }
}
