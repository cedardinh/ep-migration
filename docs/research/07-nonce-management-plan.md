# Nonce Management 方案报告：简单、常用、可自愈

## 结论

推荐方案：

```text
单 signer 串行 nonce 分配
  + 本地 nextNonce 缓存
  + eth_getTransactionCount(address, "pending") 初始化/重同步
  + nonce 类错误自动 reset 并重试一次
```

这是后端服务直连 JSON-RPC、自己签 raw transaction 时最常见的 nonce 管理形态。它比“每次发送前直接问节点 nonce”更稳，也比引入数据库、Redis 队列、Kaleido async gateway 更简单。

当前项目第一版建议实现为一个 Spring singleton：

```text
ResilientNonceManager
```

它只服务一个后端 signer。所有 `TopazLifecycle.createProject` 交易都必须通过它发送。

## v1 简化建议（避免过度防御）

本方案分两层，落地时按需要取舍，别一次把所有自愈逻辑都写进 v1：

- 必做（核心，成本低、收益高）：
  - signer 级串行锁 + 本地 `nextNonce` 缓存。
  - 首次/重启从 `eth_getTransactionCount(address, "pending")` 加载。
  - 遇到 nonce 类错误时 reset 并重试一次。
  - 这部分其实可以直接复用 web3j `FastRawTransactionManager`（它已经做了 synchronized 本地 nonce、`resetNonce()`、`setNonce()`），外面只加一个统一发送入口和 nonce 错误识别即可，不必从零手写。
- 可后置（v1 可以先不做，属于偏防御性的能力）：
  - IO timeout 后用本地 txHash 调 `eth_getTransactionByHash` 自愈。
  - “提交状态不确定”异常的精细分支。

在当前“单 signer、单实例、低并发、gasPrice=0 dev 链”的前提下，IO-timeout 自愈出现概率低；先把它列为 v2 项，让 v1 保持简洁。真正上量或换到生产链时再补。

## 边界

本方案不改合约。

本方案不处理 gas price，交易 `gasPrice` 固定为 `0`。

本方案不做 Besu filter、event subscription 或后台事件同步。

本方案默认同一个 signer 只由一个后端进程使用。如果多个后端实例共享同一个 private key，需要加分布式锁/共享 nonce store，或者改为每个实例使用不同 signer。第一版不建议把这个复杂度引进来。

## 为什么需要 nonce manager

Ethereum account nonce 是每个发送账户递增的交易序号：

- 每个 nonce 只能被最终执行一次。
- 同一个账户的交易必须按 nonce 顺序执行。
- 低 nonce 会被拒绝。
- 高 nonce 会等待前面的 nonce 交易先完成。
- 并发请求如果拿到同一个 nonce，会出现 `nonce too low`、`nonce has already been used`、`replacement transaction underpriced` 等问题。

web3j 文档说明 nonce 是递增数值，用于唯一标识交易，可以通过 `eth_getTransactionCount` 获取。Ethereum JSON-RPC 文档说明 `eth_getTransactionCount` 支持 `"pending"` block parameter，可以查询 pending 状态下的交易计数。

## 不推荐的做法

### 每个请求新建 RawTransactionManager

风险：

- 并发请求可能同时从节点拿到同一个 pending nonce。
- 如果 RPC 节点 pending pool 可见性有延迟，重复 nonce 更容易出现。
- 出错后没有统一 reset 入口。

### 只用 latest nonce

风险：

- `latest` 只反映已出块状态，不包括 pending 交易。
- 后端连续发送两笔交易时，第二笔容易复用第一笔 pending 交易的 nonce。

### 多个后端实例共享一个 signer

风险：

- 每个 JVM 内的本地 nonce 缓存彼此不知道。
- 即使都用 `"pending"`，也可能因为节点 pending pool 同步延迟产生冲突。

第一版应该避免这个部署形态。

## 可用组件

### web3j RawTransactionManager

本地源码确认，web3j 4.9.8 `RawTransactionManager.getNonce()` 使用：

```text
eth_getTransactionCount(credentials.address, PENDING)
```

这是正确方向。但如果每次请求都各自触发取 nonce，仍然缺少应用层串行边界和错误自愈。

### web3j FastRawTransactionManager

web3j 4.9.8 提供 `FastRawTransactionManager`：

- 内部维护本地 `nonce`。
- `getNonce()` 是 synchronized。
- 第一次从父类读取 pending nonce。
- 后续本地递增。
- 提供 `resetNonce()` 和 `setNonce()`。

它适合作为参考或底层组件，但第一版仍建议在项目里包一层明确的 `ResilientNonceManager`，因为我们需要：

- 统一识别 nonce 类 RPC error。
- 出错后自动 reset。
- 避免业务 gateway 直接散落 nonce 处理逻辑。

### ethers NonceManager 的行业参考

ethers v6 `NonceManager` 的设计也是 wrapper signer：

- 包装 signer。
- 自动管理 nonce。
- 保证交易使用 serialized、sequential nonce。
- 提供 `reset()`，下一笔交易从链上重新加载 nonce。

这与本项目推荐的 `ResilientNonceManager` 思路一致。

## 推荐实现结构

新增：

```text
chain/
  ResilientNonceManager.kt
```

`Web3jTopazLifecycleGateway` 不再自己决定 nonce，而是把签名发送委托给：

```text
nonceManager.sendRawTransaction(to, data, gasLimit)
```

返回：

```kotlin
data class SubmittedTransaction(
    val transactionHash: String,
    val nonce: BigInteger
)
```

`SubmittedTransaction` 只表示交易已被节点接受，不表示已经出块。当前 createProject API 直接返回该 transaction hash。

## 发送流程

推荐流程：

1. Gateway 先做 ABI 编码。
2. Gateway 调用 `ResilientNonceManager.sendRawTransaction(...)`。
3. Nonce manager 进入 signer 级锁。
4. 如果 `nextNonce == null`，从 `eth_getTransactionCount(address, PENDING)` 加载。
5. 用当前 `nextNonce` 和配置的 gas limit 构造 raw transaction。
6. `gasPrice = 0`。
7. 本地签名。
8. 调 `eth_sendRawTransaction`。
9. 成功后 `nextNonce = nonce + 1`。
10. 释放锁。
11. Gateway 立即返回 txHash。

nonce 锁只覆盖 nonce 分配和交易提交，不覆盖后续链上确认。当前 API 不等待 receipt。

## 自愈策略

### 1. 应用重启自愈

`nextNonce` 不落盘。应用重启后第一次发送交易时，从：

```text
eth_getTransactionCount(address, "pending")
```

重新加载 nonce。

这样可以自然跳过已经 pending 或 mined 的交易。

### 2. nonce 错误自愈

如果 `eth_sendRawTransaction` 返回 nonce 类错误：

- `nonce too low`
- `nonce has already been used`
- `already known`
- `known transaction`
- `replacement transaction underpriced`
- `nonce too high`

处理方式：

1. 调 `eth_getTransactionCount(address, PENDING)`。
2. 重置 `nextNonce`。
3. 对 `nonce too low` / `nonce too high` 这类可恢复错误，使用重同步后的 nonce 重试一次。
4. 第二次仍失败，抛出链提交异常，保留原始 RPC error message。

只重试一次。更多重试会掩盖真实问题，也可能让请求耗时不可控。

### 3. RPC 超时/连接中断自愈（v2）

`eth_sendRawTransaction` 发生 IO timeout 时，状态是不确定的：

- 交易可能已经被节点接收，只是响应丢了。
- 交易可能根本没有到达节点。

因为 raw transaction 是本地签名的，后端理论上可以先算出本地 txHash。该能力第一版先不实现，后续生产化时再补：

1. 用本地 txHash 调 `eth_getTransactionByHash` 短暂查询。
2. 如果能查到，视为已提交，`nextNonce = nonce + 1`。
3. 如果查不到，重置 `nextNonce` 为 pending nonce。
4. 抛出“提交状态不确定”异常，响应里带本地 txHash，供后续排查。

不要在 IO timeout 后立刻用同一个 nonce 重发。那会把“可能已提交”的交易变成重复提交问题。

## Kotlin 1.3 兼容伪代码

```kotlin
class ResilientNonceManager(
    private val web3j: Web3j,
    private val credentials: Credentials,
    private val chainId: Long
) {
    private val lock = Object()
    private var nextNonce: BigInteger? = null

    fun sendRawTransaction(to: String, data: String, gasLimit: BigInteger): SubmittedTransaction {
        synchronized(lock) {
            return sendWithRetry(to, data, gasLimit, allowRetry = true)
        }
    }

    private fun sendWithRetry(
        to: String,
        data: String,
        gasLimit: BigInteger,
        allowRetry: Boolean
    ): SubmittedTransaction {
        val nonce = nextNonce ?: refreshNonce()
        val rawTx = RawTransaction.createTransaction(
            nonce,
            BigInteger.ZERO,
            gasLimit,
            to,
            BigInteger.ZERO,
            data
        )
        val signed = sign(rawTx)
        val localHash = Hash.sha3(signed)

        try {
            val response = web3j.ethSendRawTransaction(signed).send()
            if (response.hasError()) {
                if (allowRetry && isNonceError(response.error.message)) {
                    refreshNonce()
                    return sendWithRetry(to, data, gasLimit, allowRetry = false)
                }
                throw ChainSubmitException(response.error.message)
            }
            nextNonce = nonce.add(BigInteger.ONE)
            return SubmittedTransaction(response.transactionHash, nonce)
        } catch (ex: IOException) {
            if (transactionExists(localHash)) {
                nextNonce = nonce.add(BigInteger.ONE)
                return SubmittedTransaction(localHash, nonce)
            }
            refreshNonce()
            throw TransactionSubmissionUnknownException(localHash, ex)
        }
    }

    private fun refreshNonce(): BigInteger {
        val count = web3j.ethGetTransactionCount(
            credentials.address,
            DefaultBlockParameterName.PENDING
        ).send().transactionCount
        nextNonce = count
        return count
    }
}
```

这是完整版本结构示意，不是最终可直接提交代码。第一版可以先省略 `IOException` 里的本地 txHash 查询分支，只做 nonce 类错误 reset/retry；最终实现还要复用项目已有异常类命名，并把 `sign()`、`transactionExists()`、错误分类拆清楚。

## 错误映射建议

- nonce 可恢复错误，重同步并重试一次；成功则对前端透明。
- nonce 重试后仍失败：HTTP 503，说明链提交临时不可用，响应包含 RPC error message。
- 提交状态不确定：HTTP 503，响应包含本地 txHash。

不要把 nonce 错误映射成 400。它不是用户入参错误。

## 多实例部署规则

第一版明确只支持：

```text
一个 signer -> 一个后端进程负责发送交易
```

如果以后要水平扩容，有两个简单选择：

1. 每个后端实例配置不同 signer，并分别授予 `PROJECT_OFFICER_ROLE`。
2. 引入 Redis/数据库分布式 nonce store 和锁。

第二种会增加运维依赖，不适合当前“简洁清晰”的实现目标。

## 与当前 createProject 的集成点

`Web3jTopazLifecycleGateway.createProject` 应改成：

```text
data = encodeCreateProject(request)
submitted = nonceManager.sendRawTransaction(lifecycleAddress, data, configuredGasLimit)
return submitted.transactionHash
```

这样 nonce 管理与 ABI 编码解耦，后续其它合约写操作也能复用。

## 资料来源

- web3j nonce 文档：[Transaction Nonce](https://docs.web3j.io/4.8.7/transactions/transaction_nonce/)。
- Ethereum JSON-RPC 文档：[eth_getTransactionCount](https://ethereum.org/developers/docs/apis/json-rpc/#eth_gettransactioncount)。
- ethers v6 文档：[NonceManager](https://docs.ethers.org/v6/api/providers/#NonceManager)。
- 本地 web3j 4.9.8 源码：`RawTransactionManager` 使用 `DefaultBlockParameterName.PENDING` 获取 nonce；`FastRawTransactionManager` 提供本地 nonce 缓存、`resetNonce()`、`setNonce()`。
