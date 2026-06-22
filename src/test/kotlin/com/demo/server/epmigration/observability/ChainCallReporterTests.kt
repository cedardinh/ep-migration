package com.demo.server.epmigration.observability

import com.demo.server.epmigration.chain.tx.ChainCallContext
import com.demo.server.epmigration.chain.tx.SubmittedTransaction
import org.junit.jupiter.api.Test

class ChainCallReporterTests {
    private val reporter = ChainCallReporter()

    @Test
    fun `submitted and failed log paths are executable`() {
        reporter.submitted(
            SubmittedTransaction(
                transactionHash = "0xhash",
                nonce = "1",
                from = "0xfrom",
                to = "0xto",
                functionName = "createProject",
                externalProjectId = "project-1"
            )
        )
        reporter.failed(
            ChainCallContext(
                op = "createProject",
                externalProjectId = "project-1",
                from = "0xfrom",
                to = "0xto",
                rpcMessage = "rpc failed"
            )
        )
    }
}
