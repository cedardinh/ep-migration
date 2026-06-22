# EP Migration Docker Besu 测试设计报告

生成时间：2026-06-21

## 1. 测试目标

本轮测试目标是对 `ep-migration` 系统进行尽量全量的分支覆盖验证：

- 后端 Kotlin/Spring Boot：请求 DTO、业务校验、配置、Web 异常处理、Web3j 交易发送、nonce 管理、Besu RPC 错误映射、revert 解码。
- Solidity 合约：`TopazLifecycle`、`TopazPayment`、`TopazContacts` 的正常路径、状态机、权限、输入校验和 custom error。
- Docker Besu：使用本地 Docker 容器 `ep-besu` 上的 Hyperledger Besu 24.9.1 做真实 `createProject` 交易、receipt、事件、链上读取和 revert 解码验收。
- 无法稳定通过真实链路触发的异常路径：使用 Web3j mock、组件 fake、Hardhat fixture 覆盖。

## 2. 被测范围

### 后端模块

- `project`：`CreateProjectRequest`、`ParticipantRequest`、`ApproverRequest`、`ProjectRequestValidator`、`ProjectService`、`ProjectController`
- `config`：`EpChainProperties`、`Web3jConfig`
- `chain/tx`：`ContractTransactionSender`、`ResilientNonceManager`、`ContractRevertDecoder`
- `chain/error`：`BesuJsonRpcErrors`、`ChainException` 类型映射
- `web`：`RestExceptionHandler`、`ApiErrorResponse`
- `observability`：`ChainCallReporter`

### 合约模块

- `TopazContacts`
- `TopazLifecycle`
- `TopazPayment`
- `TopazAccessControl`
- `TopazTypes`

### 排除说明

JaCoCo 单测报告排除了 `src/main/java/com/demo/server/epmigration/chain/generated/**`。该目录为 Web3j 生成 wrapper，不是手写业务逻辑，不计入单测覆盖率。

生成 wrapper 的真实链路不再只依赖 mock：`DockerBesuCreateProjectIntegrationTests` 被标记为 `@Tag("integration")`，通过独立的 `integrationTest` 任务连接本地 Docker Besu，覆盖真实 ABI 编码、私钥签名、`eth_sendRawTransaction`、receipt、`TopazLifecycle.getProjectCreatedEvents`、`TopazLifecycle.load(...).getProjectSummary(...)` 和 Besu 返回的 custom revert data 解码。

## 3. 测试分层设计

### 单元测试

- DTO 输入适配：非法 address / bytes32 映射为零值。
- 项目请求校验：空 project id、空项目名、非法 developer、非法 contractor、空 contractor、非法 approver、空 payment approver。
- 配置校验：合约地址、私钥、chainId、gasPrice、gasLimit。
- Revert 解码：标准 `Error(string)`、`Panic(uint256)`、未知 custom error selector、短 payload、畸形字符串。
- Besu RPC 错误分类：覆盖代码中列出的 Besu 24.9.1 sendRawTransaction 文案。
- `ResilientNonceManager` 并发正确性：8 线程同时提交，断言 nonce 为连续区间、`eth_getTransactionCount(PENDING)` 只调用一次、`eth_sendRawTransaction` 调用 8 次。

### Mock/组件测试

- Web3j mock：
  - nonce 获取成功/失败
  - sendRawTransaction 成功
  - Known transaction
  - Nonce too high 单次失败并清空本地 nonce 缓存
  - Nonce too low
  - RPC unavailable
  - invalid signature
  - unknown error
  - empty/null transaction hash
  - nonce/send IO exception
  - 并发提交串行化
- 组件 fake：
  - `ProjectService` 验证后才调用 gateway
  - `ProjectController` 委托 service
  - `TopazLifecycleGateway` 修剪合约地址并映射提交结果

### Hardhat 合约测试

新增 `contracts/test/topaz-branch-coverage.test.js`，覆盖：

- Contacts：权限、必填字段、batch upsert、索引、重复账户、查询越界、注销、重复注销。
- Project：构造参数、创建校验、重复 project id、update、无法移除已有 claim 的 contractor、approver 移除和级联 invalidation、删除请求、删除确认。
- Claim：提交校验、非法 actor、更新、CA reject、PO reject、resubmit、discard、delete、无 claim approver 自动进入 `ALL_CA_APPROVED`。
- Invoice：提交校验、bank account 校验、更新、PO/Finance 审批与拒绝、discard、delete。
- Payment order：创建校验、审批顺序、reject、resubmit、全部审批后创建 payment、bank reference。
- Payment：直接合约级 create/accept/reject/receipt 的输入、状态、重复和 unknown 分支。

### Docker Besu 集成测试

新增 `src/test/kotlin/com/demo/server/epmigration/integration/DockerBesuCreateProjectIntegrationTests.kt`，通过 `./gradlew integrationTest` 单独运行，不并入 `test` 和 JaCoCo 单测覆盖率。

真实 Besu 集成链路覆盖：

1. 读取 `contracts/deployments/docker-besu-1337.json` 和 `contracts/config/docker-besu.local.json`。
2. 用真实 RPC、chainId、合约地址、部署私钥构造 `Web3j`、`Credentials`、`ResilientNonceManager`、`ContractTransactionSender`、`TopazLifecycleGateway` 和 `ProjectService`。
3. 调用 `ProjectService.createProject`，产生真实 raw transaction 并发送到 Docker Besu。
4. 查询 `eth_getTransactionReceipt`，断言 `status=0x1`。
5. 使用生成 wrapper 的 `TopazLifecycle.getProjectCreatedEvents(receipt)` 解析 `ProjectCreated`。
6. 使用生成 wrapper 的 `TopazLifecycle.load(...).getProjectSummary(projectId).send()` 从链上读取项目摘要。
7. 用相同 calldata 发 `eth_call` 触发重复 `createProject`，从 Besu JSON-RPC error data 提取 custom error selector；业务层测试侧确认该 selector 对应 `DuplicateProjectId(string)`。

## 4. 覆盖结果

### Kotlin / Spring Boot

命令：

```bash
./gradlew clean test jacocoTestReport
```

结果：通过，68 个 JUnit 单测全部通过，0 failure / 0 error / 0 skipped。

JaCoCo 汇总（排除 Web3j generated wrapper）：

| 指标 | Missed | Covered | Total | 覆盖率 |
| --- | ---: | ---: | ---: | ---: |
| Instruction | 0 | 3284 | 3284 | 100.00% |
| Branch | 0 | 102 | 102 | 100.00% |
| Line | 0 | 513 | 513 | 100.00% |
| Complexity | 0 | 218 | 218 | 100.00% |
| Method | 0 | 166 | 166 | 100.00% |
| Class | 0 | 47 | 47 | 100.00% |

报告路径：

- HTML：`build/reports/jacoco/test/html/index.html`
- XML：`build/reports/jacoco/test/jacocoTestReport.xml`

### Solidity / Hardhat

命令：

```bash
cd contracts
npm test
```

结果：

```text
12 passing
```

覆盖内容包括原有 happy path、截图输入复现、以及新增合约分支测试。

### Docker Besu 集成验收

Besu：

- 容器：`ep-besu`
- 镜像：`hyperledger/besu:24.9.1`
- RPC：`http://127.0.0.1:8546`
- chainId：`1337`

部署结果：

- `TopazPayment`: `0x7cA5543f9B2C35F0E972f1B45b61A2FE53fF1ed9`
- `TopazLifecycle`: `0x63491c5363329afb6f370E9D297025481E0277e6`
- `TopazContacts`: `0x338F940F4231662Dd9a689DdC4691450de932Be5`

API 验收：

- Endpoint：`POST http://127.0.0.1:18081/api/projects`
- HTTP：`200`
- externalProjectId：`API-1782027371140774000`
- txHash：`0x143a910c17a47e05177530fcd4f6dbcd545b0e165c9ab8b7c8fd12956bfc60cb`
- receipt status：`0x1`
- emitted logs：`ProjectCreated`、`ProjectStatusChanged`

JUnit 集成测试：

```bash
./gradlew integrationTest
```

结果：通过，1 个 `@Tag("integration")` 用例通过，0 failure / 0 error / 0 skipped。

本次链上样例：

- externalProjectId：`INT-1782027221757`
- txHash：`0x68833c9fd26493c82a22d6acc426c2eb02d9daa205fec13787491880e8f298e4`
- receipt status：`0x1`
- 事件解析：`ProjectCreated` 中的 `externalProjectId` 和 developer wallet 与请求一致。
- wrapper 读链：`getProjectSummary(projectId)` 返回 project id、名称、状态、developer、claim count 与请求一致。
- revert 解码：重复 `createProject` 的 `eth_call` 返回 `Execution reverted`，error data 以 `0x705be10a` 开头；公共 decoder 只输出 `CustomError(selector=0x705be10a)`，集成测试侧确认该 selector 对应 `DuplicateProjectId(string)`。

## 5. 已发现并修复的问题

### 私钥正则不符合实现意图

`Web3jConfig` 原正则为：

```kotlin
Regex("^0x?[0-9a-fA-F]{64}$")
```

该表达式会把无前缀 key 的第一个 `0` 当作前缀部分消费，导致无 `0x` 的 64 字节 hex 私钥无法通过。已修正为：

```kotlin
Regex("^(0x)?[0-9a-fA-F]{64}$")
```

对应测试已覆盖有前缀、无前缀和非法私钥。

## 6. 风险与限制

- 当前 `POST /api/projects` 只提交交易并返回 tx hash，不等待 receipt。因此合约执行 revert 无法通过 API 同步表现为 HTTP 错误；这类异常已在 Hardhat 合约层覆盖。
- Docker Besu 集成测试验证成功交易和一个真实 custom revert 解码；RPC 不可用、nonce 冲突、空 hash 等异常不适合靠真实节点稳定制造，已用 Web3j mock 覆盖。
- 合约没有引入 solidity coverage 插件；本轮通过明确的状态/异常矩阵和 Hardhat 断言覆盖分支。

## 7. 回归命令

```bash
./gradlew clean test jacocoTestReport
./gradlew integrationTest
cd contracts && npm test
curl -s -X POST http://127.0.0.1:8546 \
  -H 'Content-Type: application/json' \
  --data '{"jsonrpc":"2.0","method":"eth_chainId","params":[],"id":1}'
```
