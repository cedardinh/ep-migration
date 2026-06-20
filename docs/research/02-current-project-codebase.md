# 本地项目调研报告：后端与合约现状

## 后端项目结构

当前后端是一个极简 Spring Boot Kotlin 项目：

- `build.gradle.kts`
  - Kotlin `1.3.72`
  - Spring Boot `2.3.12.RELEASE`
  - Spring Framework override `5.2.19.RELEASE`
  - web3j core `4.9.8`
  - Java source/target `1.8`
- `src/main/resources/application.yaml`
  - `server.port: 8081`
  - 尚未配置链节点、合约地址、chainId、签名账户。
- `src/main/kotlin/com/demo/server/epmigration/ApiController.kt`
  - 只有 `GET /api/ping`。
  - 当前没有 create project API、DTO、service、合约 client、异常处理。
- `src/test/kotlin/com/demo/server/epmigration/EpMigrationApplicationTests.kt`
  - 只有 Spring context 测试。

结论：后端还没有链上交互骨架。下一步实现应补齐最小三层：Controller -> Service -> Contract Gateway。

## 当前目标链

当前任务明确使用 Docker 中部署的 Besu 链。已在本机确认：

- Docker 容器名：`ep-besu`
- 镜像：`hyperledger/besu:24.9.1`
- 宿主机 RPC：`http://127.0.0.1:8546`
- 容器内 RPC：`8545`
- `eth_chainId`：`0x539`，即十进制 `1337`
- `eth_accounts`：空数组，Besu 不提供节点托管 signer
- `web3_clientVersion`：`besu/v24.9.1/linux-aarch_64/openjdk-java-21`

后续方案中，后端配置、Hardhat 部署网络、合约调用都应使用这条 Docker Besu 链，而不是再启动 Hardhat node。部署和后端交易都必须使用配置文件里的私钥本地签名。

## 合约项目结构

合约模块位于 `contracts/`：

- `contracts/hardhat.config.js`
  - Solidity `0.8.24`
  - optimizer enabled，runs 200
  - `viaIR: true`
  - networks：`hardhat`、`localhost`
  - 实施时应改为 Docker Besu：`http://127.0.0.1:8546`
  - 后续实施时应设置 `localhost.chainId = 1337`
  - 后续实施时应从部署配置文件读取 deployer private key
  - 关键：`deploy.js` 用 `hre.ethers.getSigners()` 取部署账户。Docker Besu `eth_accounts` 为空，如果 `localhost` 网络没有显式配置 `accounts: [deployerPrivateKey]`，`getSigners()` 会返回空数组，`const [deployer] = ...` 得到 `undefined`，部署在第一行就崩。所以 `localhost` 必须同时配 `url`、`chainId: 1337`、`accounts: [deployerPrivateKey]` 三项。
- `contracts/package.json`
  - `compile`
  - `node`
  - `deploy:local`
  - `test`
- `contracts/contracts/TopazLifecycle.sol`
  - 项目、claim、invoice、payment order 生命周期主合约。
- `contracts/contracts/TopazTypes.sol`
  - 统一 struct/enum 定义。
- `contracts/contracts/TopazAccessControl.sol`
  - 角色定义和访问控制。
- `contracts/contracts/TopazPayment.sol`
  - payment 子模块。
- `contracts/scripts/deploy.js`
  - 部署 `TopazPayment`、`TopazLifecycle`、`TopazContacts`。

## createProject 合约接口

`TopazLifecycle.createProject` 函数签名：

```solidity
function createProject(TopazTypes.CreateProjectInput calldata input)
    external
    onlyRole(PROJECT_OFFICER_ROLE)
    returns (uint256 projectId)
```

ABI 类型是一个 tuple：

```text
createProject((
  string externalProjectId,
  string name,
  (address wallet,string legalName,string addressLine1,string addressLine2,string bic,string lei,string externalRef) developer,
  (address wallet,string legalName,string addressLine1,string addressLine2,string bic,string lei,string externalRef)[] mainContractors,
  (address wallet,bytes32 userHash,string roleName,string externalRef)[] claimApprovers,
  (address wallet,bytes32 userHash,string roleName,string externalRef)[] paymentApprovers,
  string[] bankAccountRefs
) input)
```

`TopazTypes.CreateProjectInput` 字段与用户提供 JSON 完全对齐：

- `externalProjectId: string`
- `name: string`
- `developer: Participant`
- `mainContractors: Participant[]`
- `claimApprovers: ApproverConfig[]`
- `paymentApprovers: ApproverConfig[]`
- `bankAccountRefs: string[]`

`Participant`：

- `wallet: address`
- `legalName: string`
- `addressLine1: string`
- `addressLine2: string`
- `bic: string`
- `lei: string`
- `externalRef: string`

`ApproverConfig`：

- `wallet: address`
- `userHash: bytes32`
- `roleName: string`
- `externalRef: string`

## createProject 事件

成功创建项目时会 emit：

```solidity
event ProjectCreated(uint256 indexed projectId, string externalProjectId, address indexed developerWallet);
event ProjectStatusChanged(uint256 indexed projectId, TopazTypes.ProjectStatus status);
```

如果后续需要确认最终链上结果，可以从 receipt logs 解析：

- `ProjectCreated.projectId`
- `ProjectCreated.externalProjectId`
- `ProjectCreated.developerWallet`
- `ProjectStatusChanged.status`

原因：普通交易发送后拿不到 Solidity `returns (uint256 projectId)` 的直接返回值；链上最终结果需要 event 或后续 view call 获取。当前 createProject API 策略是立即返回 txHash，不等待这些结果。

## createProject 合约校验

`createProject` 的核心校验：

- 调用者必须有 `PROJECT_OFFICER_ROLE`。
- `externalProjectId` 不能为空。
- `name` 不能为空。
- `developer.wallet != address(0)`。
- `developer.legalName` 不能为空。
- `mainContractors.length >= 1`。
- `mainContractors.length <= 15`。
- 每个 contractor 的 wallet 和 legalName 必须有效。
- 同一项目内 contractor 业务身份不能重复。
- `paymentApprovers.length >= 1`。
- `paymentApprovers.length <= 15`。
- approver 的 `wallet != address(0)`。
- approver 的 `userHash != bytes32(0)`。
- 同一 approver 列表内 `userHash` 不能重复。

注意：`claimApprovers` 可以为空，因为合约只对其调用 `_validateApproverConfigs`，没有要求长度大于 0。

## 重复项目处理边界

重复项目判断已经由现有 `TopazLifecycle` 合约负责，后端不重新实现、不优化、不改 Solidity 合约规则。

后端只需要处理合约返回的 `DuplicateProjectId(string externalProjectId)`：

- 交易执行遇到该错误时，当前请求不会等待 receipt，因此第一版无法同步返回 409。
- 后续如果补 receipt/event 查询或状态同步，再把该错误映射为 HTTP 409 或查询结果。
- 不在后端额外做重复项目判定。
- 不在本轮改造合约内部去重逻辑。

## 角色和部署脚本现状

`TopazAccessControl` 定义：

- `SUPER_ADMIN_ROLE`
- `ADMIN_ROLE`
- `PROJECT_OFFICER_ROLE`
- `FINANCE_ROLE`
- `SETTLEMENT_BANK_ROLE`

构造函数只给 admin：

- `DEFAULT_ADMIN_ROLE`
- `SUPER_ADMIN_ROLE`
- `ADMIN_ROLE`

`contracts/scripts/deploy.js` 当前只额外执行：

```javascript
payment.grantRole(payment.LIFECYCLE_ROLE(), lifecycleAddress)
```

这一步是必要的跨合约授权：`TopazLifecycle` 合约地址必须持有 `TopazPayment.LIFECYCLE_ROLE`，否则 lifecycle 内部无法调用 `TopazPayment.createPayment`。

当前部署脚本还没有给业务签名钱包授予：

```text
TopazLifecycle.PROJECT_OFFICER_ROLE
TopazLifecycle.FINANCE_ROLE
TopazPayment.PAYMENT_OPERATOR_ROLE
```

测试 fixture 里有授予 `PROJECT_OFFICER_ROLE`、`FINANCE_ROLE`、`PAYMENT_OPERATOR_ROLE` 的逻辑，所以测试能覆盖更多流程。真实部署如果后端 createProject signer 没有 `PROJECT_OFFICER_ROLE`，调用 `createProject` 会因权限不足 revert。这里是部署/授权前置条件，不是 Solidity 合约修改建议。后续应通过脚本生成/复用稳定钱包，并用授权脚本完成角色矩阵 grant；第一版可以把这些业务角色 grant 到同一个后端业务钱包地址。

## 本地测试已覆盖用户 JSON

`contracts/test/topaz.test.js` 里已有用用户提供 JSON 创建项目的测试：

- 通过 `fixture.lifecycle.connect(fixture.projectOfficer).createProject(input)` 调用。
- 期望 emit `ProjectCreated(1, input.externalProjectId, developer.wallet)`。
- 期望 emit `ProjectStatusChanged(1, CREATED)`。
- `getProjectSummary(1)` 验证 externalProjectId、name、status、developer。
- `getProjectPaymentApproverCount(1) == 1`。

结论：用户给的 JSON 在合约侧是有效样本，不需要后端额外改造字段名。

## ABI 与 web3j 风险点

Hardhat artifact 已生成：

```text
contracts/artifacts/contracts/TopazLifecycle.sol/TopazLifecycle.json
```

`createProject` 使用嵌套 tuple、tuple array、string array、custom errors。实测结论：

- web3j 4.8.7 codegen 失败，错误为 `Unsupported type encountered: tuple`。
- web3j 4.9.8 codegen 成功生成 `TopazLifecycle` wrapper，并且当前 Java 8 项目可编译通过。
- 运行时代码使用 generated wrapper 的 `CreateProjectInput`、`Participant`、`ApproverConfig`，不再维护自定义 `ContractAbi`。

当前推荐使用 generated wrapper，而不是继续手写 ABI wrapper：

- wrapper 负责复杂 ABI 类型和函数名。
- `CreateProjectRequest` 本身继承 generated input；`TopazLifecycleContract` 只做 generated input 到 `ContractWriteCall` 的薄适配。
- 对外仍暴露 `TopazLifecycleGateway.createProject(request)`，后续替换提交方式不影响 HTTP/service 层。

## 后端缺口清单

- 缺少 `/api/projects`。
- 缺少请求/响应 DTO。
- 缺少链节点配置。
- 缺少合约地址配置。
- 缺少签名账户配置。
- 缺少 ABI 编码/交易发送。
- 缺少异常映射。
- 缺少部署或环境中确认后端 signer 已授权的步骤。

## 资料来源

- 本地后端：`build.gradle.kts`、`src/main/resources/application.yaml`、`src/main/kotlin/com/demo/server/epmigration/ApiController.kt`。
- 本地合约：`contracts/contracts/TopazLifecycle.sol`、`contracts/contracts/TopazTypes.sol`、`contracts/contracts/TopazAccessControl.sol`。
- 本地测试：`contracts/test/topaz.test.js`。
- 本地部署：`contracts/scripts/deploy.js`。
- 本地 ABI：`contracts/artifacts/contracts/TopazLifecycle.sol/TopazLifecycle.json`。
