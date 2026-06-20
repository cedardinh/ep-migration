package com.demo.server.epmigration.observability

import com.demo.server.epmigration.chain.gateway.CreateProjectChainResult
import com.demo.server.epmigration.chain.tx.ChainCallContext
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class ChainCallReporter {
    private val log = LoggerFactory.getLogger(ChainCallReporter::class.java)

    fun submitted(result: CreateProjectChainResult) {
        log.info(
            "CHAIN CALL SUBMITTED correlationId={} op=createProject externalId={} nonce={} txHash={} from={} to={}",
            result.correlationId,
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
                " correlationId : {}\n" +
                " op            : {}\n" +
                " externalId    : {}\n" +
                " phase         : {}\n" +
                " chainId       : {}\n" +
                " from          : {}\n" +
                " to            : {}\n" +
                " nonce         : {}\n" +
                " txHash        : {}\n" +
                " rpc.code      : {}\n" +
                " rpc.message   : {}\n" +
                " http          : {}\n" +
                "+-------------------------------------------------------------+",
            context.correlationId,
            context.op,
            context.externalProjectId,
            context.phase,
            context.chainId,
            context.from,
            context.to,
            context.nonce,
            context.transactionHash,
            context.rpcCode,
            truncate(context.rpcMessage),
            context.httpStatus
        )
    }

    private fun truncate(value: String?): String? {
        if (value == null || value.length <= 500) {
            return value
        }
        return value.substring(0, 500) + "...(truncated)"
    }
}
