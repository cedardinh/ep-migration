# topaz-elite 生产级链路改造方案

## 结论

topaz-elite 当前问题不是缺少 web3j 调用能力，而是边界混在一起：

- `Web3jLedgerClient` 同时做业务组装、合约调用、交易发送、receipt 等待、事件读取、异常翻译和后续同步。
- 截图里 `bankAccountRefs` 出现临时硬编码，说明业务适配逻辑和链调用逻辑没有清晰隔离。
- 参考 Java 项目把 nonce/hash/status 暴露得更明确，但它的 `GAS_PRICE` 常量、每笔交易读取 chainId、自定义 encoder、业务类里直接 raw transaction 都不适合直接照搬。
- topaz-elite 里的 generated-style `TopazLifecycle`/`TopazTypes` 是对的方向，应该保留并加强，而不是退回到字符串拼 function 的低层写法。

推荐改造为：

```text
REST/Flow Adapter
  -> Application Service
    -> Ledger Use Case
      -> Typed Contract Wrapper
      -> Contract Transaction Submitter
      -> Nonce Manager / Transaction Manager
      -> Receipt/Event Synchronizer
```

第一阶段先把“提交交易”改造好，不强行补数据库、队列、后台同步器。当前 ep-migration 已落地这个骨架，代码按可迁移边界拆包：

```text
chain/
  contract/
    TopazLifecycleContract.kt
  generated/
    TopazLifecycle.java
  gateway/
    TopazLifecycleGateway.kt
  tx/
    ChainTransaction.kt
    ResilientNonceManager.kt
  error/
    ChainExceptions.kt

project/
  ProjectRequestValidator.kt
  ProjectService.kt
```

## 从截图中应该吸收的设计

### 1. 类型化合约入口

topaz-elite 的 `TopazLifecycle.createProject(input)` 这种写法有价值。业务层不应该知道 ABI 编码细节，也不应该直接构造 `Function` 或 `RawTransaction`。

当前迁移项目对应实现：

```text
chain/contract/TopazLifecycleContract.kt
chain/generated/TopazLifecycle.java
```

生成 wrapper 承担 ABI 类型和合约方法定义；HTTP/业务入参本身继承 generated input，`TopazLifecycleContract` 只把 `createProject(input)` 收敛为一个 `ContractWriteCall`。后续扩展 `updateProject`、`submitClaim`、`approveClaim` 时优先复用 generated wrapper 的对应方法。

当前 ep-migration 已提供可重复生成入口：

```text
./gradlew generateTopazLifecycleWrapper
```

### 2. 统一交易提交入口

参考 Java 项目的 `submitTransaction(...)` 方向有价值，但不应把它写成业务大方法。生产级应该让所有写交易都经过一个提交器：

```text
ContractTransactionSubmitter.submit(call, context)
```

提交器只负责：

- 使用配置里的 chainId/gasLimit。
- 调 nonce manager。
- 返回 `txHash + nonce`。
- 不解析业务事件。

当前迁移项目对应实现：

```text
chain/tx/ChainTransaction.kt
chain/tx/ResilientNonceManager.kt
```

### 3. nonce/hash 可观测性

参考 Java 项目里返回 nonce、hash、状态的思路有价值。当前接口不等待 receipt，因此不应该返回链上完成状态，但可以返回提交阶段的 nonce，便于排查重复 nonce、pending 堵塞、RPC 拒绝。

当前迁移项目已把 `nonce` 加入：

```text
project/dto/CreateProjectResponse.kt
```

## 不建议照搬的设计

### 不建议引入自定义 FunctionEncoder

参考项目的 `UpgradeFunctionEncoder` 是通过重写 web3j `FunctionEncoder`，自行计算 selector、动态参数 offset 和 array/struct 长度来绕过编码问题。这种方案对 web3j 内部实现耦合太深，后续 web3j 升级或 ABI 复杂度变化时维护成本高。

当前项目更适合：

```text
Hardhat artifact -> web3j 4.9.8 codegen -> TopazLifecycle.java -> TopazLifecycleContract
```

也就是：ABI schema 由合约 artifact 提供，tuple/array 类型由生成代码承担，最终编码走 `RemoteFunctionCall.encodeFunctionCall()`，并用 ethers parity test 锁定输出。

### 不建议每笔交易读取 chainId

生产里 chainId 应该来自配置，并在启动或健康检查时校验 RPC 返回是否一致。每笔交易读取 chainId 增加 RPC 调用，也让交易路径多一个失败点。

### 不建议业务类直接 raw transaction

`RawTransaction.createTransaction(...)`、签名、`ethSendRawTransaction(...)` 应该只存在于交易提交组件。业务类只看到 `createProject(input)`。

### 不建议同步等待 receipt 作为默认提交接口

topaz-elite 截图中 `sendTransaction(...): TransactionReceipt` 适合同步流程，但生产 API 不应默认把 HTTP 请求阻塞到出块。更稳的模式是：

```text
提交接口：返回 txHash/nonce/SUBMITTED
同步器：根据 txHash 轮询 receipt、解析事件、更新业务状态
查询接口：返回链上确认后的业务状态
```

当前 ep-migration 第一阶段只实现提交接口，符合“不额外引入非必须实现”的边界。

## topaz-elite 目标包结构

建议把当前 `Web3jLedgerClient` 拆成以下职责。这个结构与当前 ep-migration 的实现包保持一致，迁移时可以按包复制和适配：

```text
rest/api/ledger/
  LedgerController.kt
  LedgerRequestMapper.kt

ledger/project/
  ProjectRequestValidator.kt

ledger/application/
  CreateProjectUseCase.kt
  UpdateProjectUseCase.kt
  ClaimUseCase.kt

ledger/chain/contract/
  TopazLifecycleContract.kt
  TopazPaymentContract.kt
ledger/chain/generated/
  TopazLifecycle.java
  TopazPayment.java

ledger/chain/tx/
  ChainTransaction.kt

ledger/chain/nonce/
  ResilientNonceManager.kt

ledger/chain/error/
  ChainExceptions.kt

ledger/chain/events/
  ReceiptReader.kt
  TopazEventMapper.kt
  LedgerEventSynchronizer.kt

ledger/observability/
  ChainCallReporter.kt
```

如果 topaz-elite 已经有 generated `TopazLifecycle` 和 `TopazTypes`，优先保留它们；只在它们外面包一层 `TopazLifecycleContract`，不要让业务 use case 直接依赖 web3j `RemoteCall`。

## createProject 生产级流程

```text
CreateProjectUseCase
  1. 校验明显格式错误和业务必填
  2. request already is generated TopazLifecycle.CreateProjectInput
  3. lifecycleContract.createProject(request)
  4. txSubmitter.submit(call, context)
  5. 保存或返回 txHash/nonce/SUBMITTED
```

当前没有数据库时直接返回：

```json
{
  "transactionHash": "0x...",
  "externalProjectId": "1",
  "from": "0x...",
  "to": "0x...",
  "nonce": "12"
}
```

如果 topaz-elite 已有数据库，生产级应落库：

```text
requestId
businessType
externalProjectId
functionName
from
to
nonce
transactionHash
submissionStatus
receiptStatus
blockNumber
errorCode
errorMessage
createdAt
updatedAt
```

## 迁移顺序

1. 先冻结 `Web3jLedgerClient` 的外部接口，不直接大改业务调用方。
2. 新建 `TopazLifecycleContract`，把 `lifecycle.createProject(input)`、`updateProject`、`claim` 等合约方法集中进去。
3. 新建 `ContractTransactionSubmitter`，把 `call.send().requireSuccess()` 或 raw transaction 逻辑迁出业务流程。
4. 所有写交易统一通过 nonce manager 或 web3j `FastRawTransactionManager` 包装层。
5. HTTP/Flow 同步调用只返回提交结果；receipt/event 解析迁到独立同步器。
6. 最后再清理硬编码、异常 message contains、散落日志和重复的 DTO 转换。

## 当前迁移项目已对应完成的改造点

- `TopazLifecycle.java`：由 web3j 4.9.8 根据 Hardhat ABI 生成，承接合约方法和复杂 tuple 类型。
- `TopazLifecycleContract`：承接 topaz-elite 的 typed wrapper 风格，只保留 generated input 到 `ContractWriteCall` 的适配。
- `ChainTransaction`：集中描述待提交调用、提交结果、提交器和调用上下文。
- `ResilientNonceManager`：单 signer nonce 串行、自愈和重试。
- `CreateProjectResponse.nonce`：提交阶段诊断字段。

这套结构可以直接作为 topaz-elite 改造时的目标形态，不要求一次移植全部业务方法；先迁 createProject，再按相同模式迁 update/claim/payment。

## 参考资料

- web3j 官方文档说明智能合约交互应通过 TransactionManager 控制签名和提交方式，并可传入合约 wrapper。
- web3j 官方文档推荐显式 chainId 以避免跨链重放。
- web3j 官方文档说明 generated smart contract wrappers 可以隐藏底层实现细节。
- Ethereum 文档说明交易 nonce 是账户级递增序号，是并发提交时必须集中管理的核心字段。
