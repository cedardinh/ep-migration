# 合约交互封装调研报告：通用 wrapper 设计

## 推荐结构

推荐后端封装形态：

```text
ProjectController
  -> ProjectService
    -> TopazLifecycleGateway
      -> Web3j JSON-RPC client
```

`TopazLifecycleGateway` 是唯一知道合约入口、contract address、交易参数和签名提交细节的地方。Controller 和业务 service 不应该直接拼 calldata 或接触 `RawTransactionManager`。

## 行业通用模式

主流 Ethereum 客户端的抽象高度一致：

- Provider/Public Client：读链、查区块、查交易、查 logs。
- Signer/Wallet Client：签名和发送交易。
- Contract wrapper：用 ABI 把业务方法映射成 typed function call。
- Transaction submission：状态变更交易先返回 txHash，最终执行结果要等 receipt。

对应到项目：

- Provider：web3j `Web3j`。
- Signer：web3j `Credentials` + `RawTransactionManager`。
- Contract wrapper：`TopazLifecycleGateway`。
- 第一版只提交交易并返回 txHash，不等待 receipt。

这个结构与 ethers `Contract + Signer`、viem `publicClient + walletClient`、web3j generated wrapper 的思想一致，可移植性强。

## 生成 wrapper vs 手写 wrapper

### 生成 wrapper

优点：

- 类型强。
- 事件解析辅助代码自动生成。
- 与 web3j 文档推荐方向一致。

实测结论：

- web3j 4.8.7 codegen 对当前 `TopazLifecycle` ABI 失败，错误为 `Unsupported type encountered: tuple`。
- web3j 4.9.8 codegen 可以生成 `TopazLifecycle` wrapper，且 Java 8 可编译运行。
- 生成 wrapper 已自动包含 `CreateProjectInput`、`Participant`、`ApproverConfig` 等 tuple 类型，项目不再需要维护 `ContractAbi`、tuple canonical type 或 `participantArray` 等手写适配。

### 手写薄 wrapper

优点：

- 只覆盖当前需要的 `createProject`，复杂度可控。
- 可以作为 codegen 不可用时的兜底方案。
- 遇到 custom errors 可以按 ABI selector 做少量手写解析。
- 对外接口稳定，后续可替换为 generated wrapper。

代价：

- ABI 字段顺序必须手动维护。
- 会在项目里留下 tuple、array、canonical type 的手工代码，迁移成本高于 generated wrapper。

本项目推荐：升级到 web3j 4.9.8 并使用生成 wrapper。HTTP 入参 `CreateProjectRequest` 本身继承 generated `TopazLifecycle.CreateProjectInput`，`TopazLifecycleContract` 只负责把这个 input 传给 generated wrapper 并生成 `ContractWriteCall`。这样既符合 web3j 官方推荐的 typed wrapper 思路，也避免在项目内维护自研 ABI 编码。

当前生成命令已固化为 Gradle 任务：

```text
./gradlew generateTopazLifecycleWrapper
```

该任务从 `contracts/artifacts/contracts/TopazLifecycle.sol/TopazLifecycle.json` 提取 ABI，生成 `src/main/java/com/demo/server/epmigration/chain/generated/TopazLifecycle.java`。

## wrapper 对外接口

建议 gateway 对业务层暴露一个非常窄的接口：

```kotlin
interface TopazLifecycleGateway {
    fun createProject(input: CreateProjectRequest): CreateProjectChainResult
}
```

`CreateProjectChainResult` 包含：

- `transactionHash`
- `externalProjectId`
- `from`
- `to`

不要把 web3j 的底层响应对象直接泄漏到 Controller，这样后续替换 Kaleido REST Gateway、Eth Wallet、ethers sidecar 或生成 wrapper 时不影响 HTTP 层。

## ABI 编码策略

`createProject` 的 ABI 参数是单个 tuple。当前由 web3j 4.9.8 生成的 `TopazLifecycle.CreateProjectInput` 负责组织以下层级：

```text
CreateProjectInputStruct: DynamicStruct
  Utf8String externalProjectId
  Utf8String name
  ParticipantStruct developer
  DynamicArray<DynamicStruct> mainContractors
  DynamicArray<DynamicStruct> claimApprovers
  DynamicArray<DynamicStruct> paymentApprovers
  DynamicArray<Utf8String> bankAccountRefs
```

`ParticipantStruct`：

```text
Address wallet
Utf8String legalName
Utf8String addressLine1
Utf8String addressLine2
Utf8String bic
Utf8String lei
Utf8String externalRef
```

`ApproverStruct`：

```text
Address wallet
Bytes32 userHash
Utf8String roleName
Utf8String externalRef
```

实现不再从 `src/main/resources/abi/topaz-lifecycle.json` 读取 ABI components；ABI 到 Java 类型的映射已经进入生成代码。项目仍保留 ethers parity test：用 `interface.encodeFunctionData("createProject", [input])` 与 generated wrapper 的 `encodeFunctionCall()` 输出对齐，防止生成代码升级或 ABI 更新后字段顺序漂移。

## 交易发送策略

推荐顺序：

1. 构造 calldata。
2. 从 `ResilientNonceManager` 获取 signer nonce。
3. `gasPrice` 固定设置为 `0`。
4. 使用配置的 gas limit 构造 raw transaction。
5. 按 chainId 本地签名并通过 `eth_sendRawTransaction` 发送。
6. 拿到 txHash 后立即返回。

当前不做 gas price 策略，不调用 `eth_gasPrice`。gas limit 建议作为配置项管理，第一版不要引入动态估算链路。

这等价于 Web3j 文档里的 no-receipt 异步提交思路，但第一版不引入 `NoOpProcessor` 或 receipt processor 配置，直接用 raw transaction submission 返回 txHash，编码量更少。

## chainId 和 replay protection

web3j `RawTransactionManager` 有 chainId 构造函数：

```text
RawTransactionManager(Web3j, Credentials, long)
RawTransactionManager(Web3j, Credentials, long, TransactionReceiptProcessor)
```

当前目标链是 Docker Besu，已确认 `eth_chainId = 0x539`，即 `1337`。后端应显式配置 `chainId = 1337`，或启动时读取并校验 RPC 返回值一致。

## nonce 并发

同一 signer 的所有写交易必须通过统一 nonce manager 发送，避免并发请求复用 nonce。

第一版推荐使用项目内 `ResilientNonceManager`：

- 单 signer 串行分配 nonce。
- 从 `eth_getTransactionCount(address, PENDING)` 初始化和重同步。
- nonce 类 RPC 错误后自动 reset 并重试一次。

详细方案见：[07-nonce-management-plan.md](/Users/dinglujie/myself/workspace/ep/ep-migration/ep-migration/docs/research/07-nonce-management-plan.md)。

## 异常处理模式

推荐异常层级：

- `BadProjectRequestException`
  - 地址格式错误、bytes32 格式错误、必填字段明显缺失。
- `ContractRejectedException`
  - 保留为后续 receipt/event 查询能力使用。
- `DuplicateProjectException`
  - 后续查询到 `DuplicateProjectId(string)` 时使用；第一版提交接口不等待 receipt。
- `ContractAuthorizationException`
  - 启动校验发现 signer 没有 `PROJECT_OFFICER_ROLE`。
- `ChainUnavailableException`
  - RPC 连接失败、超时、节点不可用。
- `TransactionSubmissionUnknownException`
  - raw transaction 已本地签名，但 RPC 响应状态不确定。

HTTP 映射建议：

- 400：本地入参格式错误，或合约 `InvalidInput`。
- 403：签名账户缺少合约角色。
- 409：`DuplicateProjectId`。
- 502：交易提交被节点拒绝。
- 503：链节点不可用。

不要把所有链异常都包成 500。前端需要区分“输入可修正”“重复提交”“链服务不可用”。

异常携带的解码信息（errorName、reason、rpc data）不在 gateway/service 里打日志，统一交给独立的错误日志组件在唯一出口格式化输出。详见：[09-observability-and-revert-diagnostics.md](/Users/dinglujie/myself/workspace/ep/ep-migration/ep-migration/docs/research/09-observability-and-revert-diagnostics.md)。

## 幂等策略

合约重复时会 revert `DuplicateProjectId(externalProjectId)`。第一版建议直接返回 409。

如果业务需要“重复请求返回第一次 txHash”，需要后端自己存储请求标识和 txHash。当前项目没有数据库，不建议在第一版假装幂等；重复判断保持由现有合约负责。

## Kotlin 1.3.72 编码风格约束

后续实现应避免：

- Kotlin value class。
- sealed interface。
- fun interface。
- trailing comma 依赖。
- Java 9+ API。
- Spring Boot 2.4+ 配置特性。

可以使用：

- `data class`
- 普通 `class`
- `@ConfigurationProperties`
- `@RestController`
- `@ControllerAdvice`
- web3j 4.9.8 的现有类。

## 资料来源

- web3j：[Quickstart](https://docs.web3j.io/4.8.7/quickstart/)。
- web3j：[Interacting with smart contracts](https://docs.web3j.io/4.8.7/smart_contracts/interacting_with_smart_contract/)。
- web3j：[Transactions and smart contracts](https://docs.web3j.io/4.8.7/transactions/transactions_and_smart_contracts/)。
- web3j：[Transaction nonce](https://docs.web3j.io/4.8.7/transactions/transaction_nonce/)。
- web3j：[Credentials](https://docs.web3j.io/4.8.7/transactions/credentials/)。
- ethers v6：[Contract API](https://docs.ethers.org/v6/api/contract/)。
- viem：[Public Client](https://viem.sh/docs/clients/public)、[writeContract](https://v1.viem.sh/docs/contract/writeContract.html)、[simulateContract](https://v1.viem.sh/docs/contract/simulateContract.html)。
- Hardhat v2：[Artifacts](https://v2.hardhat.org/hardhat-runner/docs/advanced/artifacts)、[Hardhat Network](https://v2.hardhat.org/hardhat-network/docs/overview)。
