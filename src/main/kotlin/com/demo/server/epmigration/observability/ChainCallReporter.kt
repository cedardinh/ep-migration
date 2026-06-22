package com.demo.server.epmigration.observability

import com.demo.server.epmigration.chain.tx.ChainCallContext
import com.demo.server.epmigration.chain.tx.SubmittedTransaction
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class ChainCallReporter {
    private val log = LoggerFactory.getLogger(ChainCallReporter::class.java)

    fun submitted(result: SubmittedTransaction) {
        log.info(
            "CHAIN CALL SUBMITTED op={} externalId={} nonce={} txHash={} from={} to={}",
            result.functionName,
            result.externalProjectId,
            result.nonce,
            result.transactionHash,
            result.from,
            result.to
        )
    }

    fun failed(context: ChainCallContext) {
        log.error(
            "\n+--------------------- CHAIN CALL FAILED ---------------------+\n" +
                " op            : {}\n" +
                " externalId    : {}\n" +
                " phase         : {}\n" +
                " from          : {}\n" +
                " to            : {}\n" +
                " nonce         : {}\n" +
                " rpc.code      : {}\n" +
                " rpc.message   : {}\n" +
                " http          : {}\n" +
                "+-------------------------------------------------------------+",
            context.op,
            context.externalProjectId,
            context.phase,
            context.from,
            context.to,
            context.nonce,
            context.rpcCode,
            context.rpcMessage,
            context.httpStatus
        )
    }
}
