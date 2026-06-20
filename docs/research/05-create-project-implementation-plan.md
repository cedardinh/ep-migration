# Create Project 实施方案报告

## 总体方案

第一版实现目标：

```text
POST /api/projects
  -> ProjectService
    -> TopazLifecycleGateway
      -> web3j JSON-RPC
        -> TopazLifecycle.createProject
```

保持实现简洁，避免引入数据库、消息队列、异步任务框架或新依赖。链交互封装在 gateway 内部，HTTP 层只处理请求和响应。

三层职责边界：

- Controller：只做 HTTP 路由、JSON 入参/出参、传递 `X-Request-Id`。
- Service：调用请求校验器和 gateway，不直接依赖 ABI 或 web3j。
- ProjectRequestValidator：做轻量业务校验和 EVM 基础格式校验。
- Contract wrapper：只做类型化合约调用和 ABI 编码。
- Transaction submitter：只做交易提交、nonce manager 调用和提交结果返回。

当前没有数据库，不需要引入 `@Transactional` 或仓储层。

## 后端包结构建议

在当前包 `com.demo.server.epmigration` 下新增：

```text
config/
  EpChainProperties.kt
  Web3jConfig.kt

project/
  ProjectController.kt
  ProjectRequestValidator.kt
  ProjectService.kt
  dto/CreateProjectRequest.kt
  dto/CreateProjectResponse.kt

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

web/
  RestExceptionHandler.kt

observability/
  ChainCallReporter.kt
```

`chain/error/ChainExceptions.kt` 里的 typed 异常额外携带一个 `ChainCallContext`，由 `RestExceptionHandler` 在唯一出口交给 `ChainCallReporter` 格式化打印；主流程不写日志。详见：[09-observability-and-revert-diagnostics.md](/Users/dinglujie/myself/workspace/ep/ep-migration/ep-migration/docs/research/09-observability-and-revert-diagnostics.md)。

如果希望更少文件，也可以先合并 DTO 文件，但不建议把 controller、service、ABI 编码放在同一个类里。

当前已实测 web3j 4.9.8 可以生成复杂 tuple wrapper，因此第一版采用 generated wrapper。`TopazLifecycleContract.kt` 只作为薄适配器，避免在项目内维护 ABI 解析、tuple type 和自定义 encoder。

ABI 更新后重生成 wrapper：

```text
./gradlew generateTopazLifecycleWrapper
```

## application.yaml 配置建议

```yaml
spring:
  application:
    name: ep-migration

server:
  port: 8081

ep:
  chain:
    rpc-url: http://127.0.0.1:8546
    chain-id: 1337
    lifecycle-contract-address: "0x..."
    signer-private-key: "0x..."
    gas-limit: 5000000
```

当前按要求直接在配置文件里配置 private key。后端 signer 用 `application.yaml` 的 `ep.chain.signer-private-key`；部署 signer 后续用部署配置文件中的 `deployerPrivateKey`。

## DTO 设计

请求 DTO 与 JSON 字段保持一致：

```kotlin
data class CreateProjectRequest(
    val externalProjectId: String,
    val name: String,
    val developer: ParticipantRequest,
    val mainContractors: List<ParticipantRequest>,
    val claimApprovers: List<ApproverRequest>,
    val paymentApprovers: List<ApproverRequest>,
    val bankAccountRefs: List<String>
)
```

Kotlin 1.3.72 下，Jackson Kotlin module 支持 data class 反序列化。为了简洁，不必引入额外 validation starter。基础格式校验在 service/gateway 做：

- address 是否 20 bytes hex。
- userHash 是否 bytes32 hex。
- 合约必填字段明显缺失时可提前返回 400。

不要把合约全部校验逻辑复制到后端。合约仍是最终规则来源。

## 合约 gateway 核心流程

```text
createProject(request):
  validate obvious formats
  data = encodeCreateProject(request)
  from = credentials.address
  to = lifecycleContractAddress
  submitted = nonceManager.sendRawTransaction(to, data, configuredGasLimit)
  return submitted transaction hash
```

当前不做动态 gas 策略，`gasPrice=0`，gas limit 作为配置项管理。链上失败诊断不在 gateway 内直接打日志；gateway 只把提交阶段的 RPC error、nonce、txHash 等上下文放入异常，`RestExceptionHandler` 再交给 `ChainCallReporter` 统一输出。详细方案见：[09-observability-and-revert-diagnostics.md](/Users/dinglujie/myself/workspace/ep/ep-migration/ep-migration/docs/research/09-observability-and-revert-diagnostics.md)。

## ABI 编码细节

`TopazLifecycleContract.kt` 不手写 function selector、function name 常量或 tuple canonical type。function name、tuple 结构、array 类型都来自 web3j 4.9.8 生成的 `TopazLifecycle.java`。

`TopazLifecycleContract.kt` 只负责：

- 接收已经是 `TopazLifecycle.CreateProjectInput` 的请求结构。
- 调 `TopazLifecycle.createProject(input).encodeFunctionCall()` 生成 calldata。
- 返回统一的 `ContractWriteCall`，继续交给交易提交器发送。

不建议引入类似 `UpgradeFunctionEncoder extends FunctionEncoder` 的自定义 encoder。它复制的是 web3j `FunctionEncoder`/`TypeEncoder` 的底层 offset 逻辑，适合作为历史 bug workaround；当前项目已经用 ethers 对齐测试证明 generated wrapper 的 `encodeFunctionCall()` 输出正确，继续复用 web3j 官方编码链路更稳。

编码用 web3j 4.9.8：

- generated `TopazLifecycle`
- `RemoteFunctionCall.encodeFunctionCall()`
- generated `CreateProjectInput`
- generated `Participant`
- generated `ApproverConfig`

值映射顺序必须完全等于 ABI：

```text
externalProjectId,
name,
developer,
mainContractors,
claimApprovers,
paymentApprovers,
bankAccountRefs
```

实现后应加一个测试：用 Hardhat/ethers 生成相同 input 的 calldata，和 Kotlin wrapper 编码结果比对。

## 交易返回值处理

不要尝试从发送交易的 RPC response 获取 Solidity `returns (uint256 projectId)`。状态变更交易的提交响应只包含 transaction hash。

当前策略是立即返回 txHash 和提交 nonce，不等待 receipt。因此第一版响应不包含 `projectId` 或链上 `status`。如果后续需要展示 projectId，应另做交易查询或状态同步能力。

## 异常处理设计

### 本地格式错误

触发条件：

- wallet 不是合法 `0x` + 40 hex。
- userHash 不是合法 `0x` + 64 hex。
- list 字段为 null。

HTTP：400。

### 合约 InvalidInput

触发条件示例：

- `externalProjectId` 为空。
- `name` 为空。
- 没有 main contractor。
- 没有 payment approver。
- duplicate approver userHash。

HTTP：400。

### DuplicateProjectId

触发条件：

- 合约判定项目重复，并返回 `DuplicateProjectId(externalProjectId)`。

HTTP：409。

### AccessControlUnauthorizedAccount

触发条件：

- 后端 signer 没有 `PROJECT_OFFICER_ROLE`。

HTTP：403。

建议响应提示里包含：

- signer address
- missing role name：`PROJECT_OFFICER_ROLE`

不要泄漏私钥。

### RPC 不可用

触发条件：

- 连接失败。
- HTTP 401/403。
- 超时。
- 节点未开启 JSON-RPC。

HTTP：503。

## 部署前置条件

必须确认：

1. 已部署 `TopazLifecycle`，并拿到合约地址。
2. createProject 后端配置的 signer 已被授予：

```text
TopazLifecycle.PROJECT_OFFICER_ROLE
```

3. `TopazPayment.LIFECYCLE_ROLE` 已授予 `TopazLifecycle` 地址。
4. 后续 finance/payment 流程需要的角色已在授权脚本中覆盖：

```text
TopazLifecycle.FINANCE_ROLE -> financeOperator
TopazPayment.PAYMENT_OPERATOR_ROLE -> paymentOperator
```

`financeOperator`、`paymentOperator` 是角色别名，第一版可以和 `projectOfficerBackend` 配成同一个钱包地址。这样保留多角色授权矩阵，但后端配置仍可以先只用一个业务私钥。

5. `SETTLEMENT_BANK_ROLE` 当前合约没有直接函数使用，不作为第一版必授。
6. 后端交易 `gasPrice` 固定为 0。
7. 后端配置的 chainId 与节点一致。

当前 `contracts/scripts/deploy.js` 只做了第 3 点，没有给后端 signer 授 `PROJECT_OFFICER_ROLE`，也没有覆盖 `FINANCE_ROLE` / `PAYMENT_OPERATOR_ROLE`。后续方案应通过脚本生成/复用稳定钱包，并用幂等授权脚本执行完整授权矩阵；不是手动授权，也不是 Solidity 合约修改点。详细方案见：[08-stable-wallets-and-role-grant-plan.md](/Users/dinglujie/myself/workspace/ep/ep-migration/ep-migration/docs/research/08-stable-wallets-and-role-grant-plan.md)。

## 测试计划

### 后端单元测试

- address 格式校验。
- bytes32 格式校验。
- DTO 到 ABI struct 的字段顺序。
- 异常到 HTTP 状态码映射。

### 合约编码一致性测试

生成一份给定 JSON 的 calldata：

- JS：ethers `Interface.encodeFunctionData("createProject", [input])`
- Kotlin：`TopazLifecycleContract.createProject(request).data`

断言完全一致。

### 本地集成测试

最小手动流程：

```text
cd contracts
run wallet generation script if wallet config does not exist
create/edit deployment config and set deployerPrivateKey
npm run deploy:docker-besu
npm run roles:grant
start Spring Boot
POST /api/projects
check response txHash
```

后续实施时，部署脚本应使用 `contracts/hardhat.config.js` 中的 Docker Besu 网络，并指向 `http://127.0.0.1:8546`，chainId 为 `1337`。不再启动 Hardhat node。由于 Docker Besu `eth_accounts` 为空，部署和授权必须通过稳定钱包配置里的 private key 本地签名。

自动化集成测试可以后置，因为当前后端还没有 testcontainers 或链测试依赖。第一版至少应保留清晰手动验证脚本。

## 分阶段实施顺序

1. 补配置类和 web3j bean。
2. 补 DTO、controller、service。
3. 实现 `TopazLifecycleContract.createProject`。
4. 实现 `TopazLifecycleGateway` 的 encode/send。
5. 实现 `ResilientNonceManager`，统一处理 signer nonce。
6. 实现异常类（携带 `ChainCallContext`）和 `RestExceptionHandler`。
7. 实现独立的 `ChainCallReporter`，在 `RestExceptionHandler` 唯一出口格式化打印人类可读错误块（correlationId 可选）。详见 09。
8. 补稳定钱包生成脚本和幂等角色授权脚本，确认授权矩阵完整。
9. 跑 Hardhat test、Gradle test、本地端到端请求。

## 不建议第一版做的事

- 不建议引入数据库做复杂幂等。
- 不建议把 Kaleido REST Gateway 作为唯一实现路径。
- 不建议复制所有合约校验逻辑到后端。
- 不建议在 Controller 里写 ABI 编码。
- 不建议把 web3j `TransactionReceipt` 原样暴露给前端。
- 不建议引入事件订阅。
- 不建议启动 Hardhat node 作为目标链；目标链是 Docker Besu。

## 资料来源

- 本地合约 ABI：`contracts/artifacts/contracts/TopazLifecycle.sol/TopazLifecycle.json`。
- 本地合约测试：`contracts/test/topaz.test.js`。
- 本地部署脚本：`contracts/scripts/deploy.js`。
- Besu transactions：[Send transactions](https://docs.besu-eth.org/public-networks/how-to/send-transactions)。
- Besu revert reason：[Revert reason](https://docs.besu-eth.org/private-networks/how-to/send-transactions/revert-reason)。
- web3j：[Interacting with smart contracts](https://docs.web3j.io/4.8.7/smart_contracts/interacting_with_smart_contract/)。
- web3j：[Transactions and smart contracts](https://docs.web3j.io/4.8.7/transactions/transactions_and_smart_contracts/)。
- Hardhat v2：[Hardhat Network](https://v2.hardhat.org/hardhat-network/docs/overview)。
