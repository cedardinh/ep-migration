package io.cryptoblk.migration.demo

import com.demo.server.epmigration.chain.generated.TopazLifecycle
import org.web3j.crypto.Credentials
import org.web3j.protocol.Web3j
import org.web3j.protocol.core.methods.response.TransactionReceipt
import org.web3j.protocol.http.HttpService
import java.math.BigInteger

/**
 * Demo: 同步发送 createProject 交易，并在「下一行」直接拿到链上 projectId。
 *
 * 关键点：
 *  1. RemoteFunctionCall.send() 是同步阻塞调用，会一直等到交易被打包出回执。
 *  2. 链上写交易拿不到 Solidity 的 return 值，projectId 只能从回执里的
 *     ProjectCreated 事件解析。
 */
object CreateProjectDemo {

    private const val RPC_URL = "http://localhost:8545"
    private const val CONTRACT_ADDRESS = "0x0000000000000000000000000000000000000000"
    private const val PRIVATE_KEY = "0x<your-private-key>"

    @JvmStatic
    fun main(args: Array<String>) {
        val web3j = Web3j.build(HttpService(RPC_URL))
        val credentials = Credentials.create(PRIVATE_KEY)

        val contract = TopazLifecycle.load(
            CONTRACT_ADDRESS,
            web3j,
            credentials,
            BigInteger.ZERO,                  // gasPrice
            BigInteger.valueOf(9_000_000)     // gasLimit
        )

        val input = buildSampleInput()

        // 发送交易，下一行立即拿到 projectId
        val projectId = contract.createProjectAndGetId(input)
        println("createProject 成功, projectId = $projectId")

        web3j.shutdown()
    }

    private fun buildSampleInput(): TopazLifecycle.CreateProjectInput {
        val developer = TopazLifecycle.Participant(
            "0x0000000000000000000000000000000000000001",
            "Demo Developer",
            "Address Line 1",
            "Address Line 2",
            "DEMOBIC",
            "DEMOLEI",
            "ext-developer"
        )
        return TopazLifecycle.CreateProjectInput(
            "EXT-PROJECT-001",
            "Demo Project",
            developer,
            emptyList(),
            emptyList(),
            emptyList(),
            listOf("BANK-REF-1")
        )
    }
}

/**
 * 同步发送 createProject 并直接返回链上 projectId（取自 ProjectCreated 事件）。
 */
fun TopazLifecycle.createProjectAndGetId(
    input: TopazLifecycle.CreateProjectInput
): BigInteger {
    val receipt: TransactionReceipt = createProject(input).send()
    check(receipt.isStatusOK) { "createProject 交易失败: status=${receipt.status}" }
    return TopazLifecycle.getProjectCreatedEvents(receipt).single().projectId
}
