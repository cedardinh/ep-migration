# 链调用日志与提交诊断方案

## 结论

第一版不等待 receipt，也不解析合约 revert 原因。可观测性只覆盖当前请求内真实发生的阶段：

```text
HTTP request
  -> ABI encode
  -> nonce allocation
  -> sign raw transaction
  -> eth_sendRawTransaction
  -> return txHash or stable HTTP error
```

核心目标是：每次提交交易时，能通过 `correlationId` 查到请求摘要、signer、contract、nonce、txHash、RPC 错误和 nonce 自愈动作。

关键事实：

- `eth_sendRawTransaction` 成功只表示节点接受 raw transaction，不证明项目已经创建。
- 当前接口立即返回 txHash，不等待 receipt，所以不能同步返回 `projectId`、链上 status 或最终执行错误。
- EVM revert 会回滚 event logs。失败交易里没有可依赖的 `ProjectCreated` / `ProjectStatusChanged`。

## 设计原则

- 独立：gateway / service / controller 不散落 `log.xxx`。日志由 `ChainCallReporter` 统一输出。
- 简单：复用 Spring Boot 自带 slf4j + logback，不加 logging 依赖。
- 可读：失败日志按固定字段输出，方便人工排查。
- 克制：不引入数据库、队列、trace/debug RPC、metrics、ELK 或后台索引器。
- 明确语义：成功日志只能叫 `submitted`，不能叫 `created`。

## 组件边界

```text
Web3jTopazLifecycleGateway
  - 编码 calldata
  - 调 ResilientNonceManager 提交 raw transaction
  - RPC/提交错误时抛出携带 ChainCallContext 的 typed exception
  - 不打日志

ResilientNonceManager
  - 串行分配 nonce
  - gasPrice=0
  - 本地签名 raw transaction
  - 调 eth_sendRawTransaction
  - nonce 类错误 reset 并重试一次

RestExceptionHandler
  - typed exception -> HTTP status / error response
  - 把 ChainCallContext 交给 ChainCallReporter

ChainCallReporter
  - 唯一输出链调用诊断日志的组件
  - 负责排版、脱敏、长字段截断、日志级别
```

## ChainCallContext

异常里携带结构化上下文，reporter 只消费这个对象：

```kotlin
data class ChainCallContext(
    val correlationId: String?,
    val op: String,                 // "createProject"
    val externalProjectId: String?,
    val chainId: Long,
    val from: String,
    val to: String,
    val phase: String,              // "encode" / "nonce" / "send(eth_sendRawTransaction)"
    val rpcCode: Int?,
    val rpcMessage: String?,
    val transactionHash: String?,
    val nonce: String?,
    val httpStatus: Int
)
```

不要放：

- private key
- 完整 raw transaction
- 完整请求体
- 完整 calldata（除非以后临时打开 DEBUG）

## 成功日志

节点接受 raw transaction 后输出一行提交日志：

```text
CHAIN CALL SUBMITTED correlationId=... op=createProject externalId=1 nonce=7 txHash=0xabc... from=0x628d... to=0x9fE4...
```

注意：

- 这条日志只表示 transaction submitted。
- 不表示 project created。
- 不包含 receipt、blockNumber、projectId 或 event。

## 失败日志

失败时输出固定字段日志：

```text
+--------------------- CHAIN CALL FAILED ---------------------+
 correlationId : 9d02f2d7-0b45-45c4-bd50-60f248f87c13
 op            : createProject
 externalId    : 1
 phase         : send (eth_sendRawTransaction)
 chainId       : 1337
 from          : 0x628d684197485c054cda7d3def46e8be6b3d174c
 to            : 0x9fE4...A3c1
 nonce         : 7
 txHash        : 0xabc...      // 如果本地已经算出或节点返回
 rpc.code      : -32000
 rpc.message   : nonce too low
 http          : 503
+-------------------------------------------------------------+
```

## 日志级别

- `INFO`：请求进入、交易 submitted。
- `WARN`：nonce 类错误且已经触发 reset/retry；节点拒绝交易但请求可重试。
- `ERROR`：RPC 不可用、提交状态不确定、重试后仍失败。

本地格式错误可以由普通应用异常处理返回 400，不需要打印链调用失败块。

## HTTP 响应

HTTP 响应保持稳定、简洁，不把完整 RPC data 暴露给前端：

```json
{
  "errorCode": "CHAIN_RPC_UNAVAILABLE",
  "message": "Blockchain RPC is unavailable",
  "correlationId": "9d02f2d7-0b45-45c4-bd50-60f248f87c13"
}
```

建议映射：

```text
local format error                       -> 400 BAD_PROJECT_REQUEST
nonce error retry succeeded              -> 200 submitted response
nonce error retry failed                 -> 503 NONCE_UNAVAILABLE
RPC unavailable / timeout                -> 503 CHAIN_RPC_UNAVAILABLE
sendRawTransaction rejected              -> 502 TRANSACTION_SUBMISSION_FAILED
submission state unknown                 -> 503 TRANSACTION_SUBMISSION_UNKNOWN
```

## 第一版不做

- 不等待 receipt。
- 不解析失败交易 event。
- 不解析合约 custom error。
- 不启用 debug/trace RPC。
- 不做事件订阅、后台状态同步或交易索引器。
- 不新增 tx diagnostics API。
- 不引入 JSON logger、metrics、tracing、ELK 或队列。

这些属于下一阶段的“链上最终状态确认 / 生产运维观测”能力。第一版的成功语义仍然是 transaction submitted。

## 资料来源

- 本地合约：`contracts/contracts/TopazLifecycle.sol`、`contracts/contracts/TopazAccessControl.sol`。
- Besu transactions：[Send transactions](https://docs.besu-eth.org/public-networks/how-to/send-transactions)。
- Ethereum revert 与 event 行为：[Is it possible to retrieve an event log from a reverted transaction?](https://ethereum.stackexchange.com/questions/38019/is-it-possible-to-retrieve-an-event-log-from-a-reverted-transaction)。
