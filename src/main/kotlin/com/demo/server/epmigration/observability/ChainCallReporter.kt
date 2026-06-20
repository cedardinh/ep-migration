package com.demo.server.epmigration.observability

import com.demo.server.epmigration.chain.tx.ChainCallContext
import com.demo.server.epmigration.project.dto.CreateProjectResponse
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class ChainCallReporter {
    private val log = LoggerFactory.getLogger(ChainCallReporter::class.java)

    fun submitted(result: CreateProjectResponse) {
        log.info(
            "CHAIN CALL SUBMITTED op=createProject externalId={} nonce={} txHash={} from={} to={}",
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
