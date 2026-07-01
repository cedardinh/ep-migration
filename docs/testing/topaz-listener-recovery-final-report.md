# Topaz 事件监听与重启补扫最终测试报告

生成时间：2026-07-01

## 结论

本轮真实环境验收通过。服务在监听过程中被故意停止，后续交易继续产生链上事件；服务重启后从 checkpoint 之后自动补扫缺失区块，最终所有事件均被监听到，且 workflow 日志中的事件参数与链上 receipt 解码参数一致。

针对“同一区块内多条事件，处理到中间宕机后只按 `processed_block` 恢复会跳过剩余事件”的问题，checkpoint 已升级为事件级游标：`processed_block + processed_tx_hash + processed_log_index`。重启补扫时，如果 checkpoint 带有 `processed_log_index`，会从同一区块重新扫描并跳过已处理到的 logIndex，继续处理同区块后续事件。

最终未发现事件漏监听 bug。

## 测试环境

- Besu 容器：`ep-besu`
- Postgres 容器：`ep-postgres`
- RPC：`http://127.0.0.1:8546`
- Chain ID：`1337`
- 服务端口：`18081`
- Checkpoint 表：`topaz_event_checkpoint`

本轮重新部署合约：

| 合约 | 地址 |
|---|---|
| `TopazPayment` | `0x2D762ABe697729146Cf7EEBf62f086a86f529Df3` |
| `TopazLifecycle` | `0x7E19791314f569Ca4ad98905d16AB55a7e976dE8` |
| `TopazContacts` | `0x36C394B94C2b765fB925AeD01e527be28571E91C` |

## 总体测试统计

| 测试类别 | 数量 | 通过 | 未通过 | 跳过 |
|---|---:|---:|---:|---:|
| Gradle 单元测试 | 80 | 80 | 0 | 0 |
| Docker Besu JUnit 集成测试 | 1 | 1 | 0 | 0 |
| Hardhat 合约测试 | 12 | 12 | 0 | 0 |
| 事件监听重启补扫验收：事件类型 | 30 | 30 | 0 | 0 |
| 事件监听重启补扫验收：事件检查 | 52 | 52 | 0 | 0 |

按自动化测试用例计：93 个测试用例全部通过。

按“自动化测试用例 + 事件验收检查”计：145 项验证全部通过。

## 已通过测试

### Gradle 单元测试

| 测试类 | 数量 | 通过 |
|---|---:|---:|
| `ApiControllerTests` | 1 | 1 |
| `EpMigrationApplicationTests` | 2 | 2 |
| `BesuJsonRpcErrorsTests` | 6 | 6 |
| `ChainExceptionsTests` | 4 | 4 |
| `TopazLifecycleGatewayTests` | 2 | 2 |
| `ContractRevertDecoderTests` | 7 | 7 |
| `ContractTransactionSenderTests` | 7 | 7 |
| `ResilientNonceManagerTests` | 14 | 14 |
| `Web3jConfigTests` | 2 | 2 |
| `TopazTypesSelectorDebugTests` | 1 | 1 |
| `ChainCallReporterTests` | 1 | 1 |
| `ProjectRequestValidatorTests` | 3 | 3 |
| `ProjectServiceAndControllerTests` | 3 | 3 |
| `ProjectDtoTests` | 7 | 7 |
| `RestExceptionHandlerTests` | 5 | 5 |
| `TopazContractEventListenerTests` | 10 | 10 |
| `TopazEventRegistryTests` | 5 | 5 |

### Docker Besu JUnit 集成测试

| 测试类 | 数量 | 通过 |
|---|---:|---:|
| `DockerBesuCreateProjectIntegrationTests` | 1 | 1 |

### Hardhat 合约测试

| 套件 | 数量 | 通过 |
|---|---:|---:|
| `createProject screenshot input reproduction` | 2 | 2 |
| `Topaz branch coverage` | 5 | 5 |
| `Topaz contracts` | 5 | 5 |

### 事件监听重启补扫验收

最终报告：`build/topaz-smoke-event-checkpoint-after-restart.md`

验收脚本先生成所有链上事件，然后用服务日志验证 workflow 是否接收并输出同样参数。最终结果：

- 事件类型：30 / 30 通过
- 事件检查：52 / 52 通过
- 链上 emitted 参数：全部通过
- workflow 接收：全部通过
- workflow 参数比对：全部通过

覆盖事件如下：

| 合约 | 事件 |
|---|---|
| `lifecycle` | `ProjectCreated` |
| `lifecycle` | `ProjectStatusChanged` |
| `lifecycle` | `ProjectUpdated` |
| `lifecycle` | `ProjectApproverRemoved` |
| `lifecycle` | `ClaimCreated` |
| `lifecycle` | `ClaimDocumentsUpdated` |
| `lifecycle` | `ClaimStatusChanged` |
| `lifecycle` | `InvoiceCreated` |
| `lifecycle` | `InvoiceDocumentsUpdated` |
| `lifecycle` | `InvoiceStatusChanged` |
| `lifecycle` | `PaymentOrderCreated` |
| `lifecycle` | `PaymentOrderStatusChanged` |
| `lifecycle` | `PaymentCreatedForOrder` |
| `lifecycle` | `BankPaymentRequested` |
| `lifecycle` | `BankPaymentReferenceRecorded` |
| `lifecycle` | `RoleAdminChanged` |
| `lifecycle` | `RoleGranted` |
| `lifecycle` | `RoleRevoked` |
| `payment` | `PaymentCreated` |
| `payment` | `PaymentAccepted` |
| `payment` | `PaymentRejected` |
| `payment` | `PaymentReceiptCreated` |
| `payment` | `RoleAdminChanged` |
| `payment` | `RoleGranted` |
| `payment` | `RoleRevoked` |
| `contacts` | `ContactUpserted` |
| `contacts` | `ContactDeactivated` |
| `contacts` | `RoleAdminChanged` |
| `contacts` | `RoleGranted` |
| `contacts` | `RoleRevoked` |

## 未通过测试

最终结果中没有未通过测试。

说明：`build/topaz-smoke-event-checkpoint-missed.md` 是故意停止服务后生成的中间报告，里面 30 类事件、52 项 workflow 检查缺失是测试场景本身用于制造漏监听，不是最终失败。服务重启后，`build/topaz-smoke-event-checkpoint-after-restart.md` 已验证这些缺失全部补齐。

## 重启补扫证据

服务重启后日志记录：

```text
Topaz event compensation scanned blocks 267010..267100 logs=52 handled=52 completedThroughBlock=267100
```

当前代码补扫完成时使用 `completedThroughBlock` 字段；如果补扫中有事件失败，该字段会记录为 `null`，checkpoint 会停留在最后一个成功事件游标。

本轮还验证了旧表兼容升级。启动前 `topaz_event_checkpoint` 只有 `listener_name`、`processed_block` 两列；服务启动后自动补充：

```text
processed_tx_hash varchar(66)
processed_log_index numeric(78, 0)
```

数据库 checkpoint：

```text
listener_name: topaz-contract-event-listener
processed_block: 267578
processed_tx_hash: null
processed_log_index: null
```

引入事务管理器后，额外启动真实服务验证 Spring listener 上下文可正常注入 `PlatformTransactionManager`，并执行补扫：

```text
Topaz event compensation scanned blocks 267101..267358 logs=2 handled=2 completedThroughBlock=267358
```

新增 `ep.chain.listener-from-block` 配置后，再次启动真实服务验证空配置会按“未配置”处理，已有 checkpoint 时仍按 checkpoint 继续补扫：

```text
Topaz event compensation scanned blocks 267359..267578 logs=2 handled=2 completedThroughBlock=267578
```

## 本轮修复与增强

- workflow 事件日志新增 `params`，用于返回足够事件信息并支持字段级比对。
- `bytes32` 事件字段输出为稳定 `0x...` 格式，避免被 JSON 序列化成不可比对格式。
- checkpoint repository 改为 JDBC 实现，避免服务启动依赖运行期 JPA。
- checkpoint 表启动时自动创建，并兼容已有表自动补充 `processed_tx_hash`、`processed_log_index` 列。
- 首次启动没有 checkpoint 时支持配置 `ep.chain.listener-from-block`，用于从指定区块回扫；未配置时保持以最新块初始化 checkpoint 的原行为。
- checkpoint 从只记录已完整处理区块升级为事件级游标，事件处理成功后立即保存 `blockNumber + transactionHash + logIndex`。
- 补扫事件处理与事件 checkpoint 写入放进同一个 Spring 事务；checkpoint 写入失败会 rollback 本条事件事务并停止本轮补扫。
- 区块完成 checkpoint 也通过事务提交，失败时会保留最后一个成功事件游标，下一次从该事件之后继续补扫。
- 缺少 `blockNumber`、`transactionHash` 或 `logIndex` 的事件不会推进 checkpoint，避免保存不完整游标后错误跳过事件。
- 重启补扫遇到事件级游标时从同一区块恢复，并按 `logIndex` 跳过已处理日志，避免同一区块剩余事件被跳过。
- 补扫时按 `blockNumber + transactionIndex + logIndex` 排序，确保多区块、多交易、多日志的处理顺序稳定。
- handler 或 checkpoint 保存失败时停止本轮补扫，不再把处理进度推进到区块末尾，避免越过失败事件。
- 监听器和 checkpoint repository 增加 `ep.chain.listener-enabled` 开关，便于普通 Spring context 测试关闭真实链监听。
- `smoke-events.js` 增加 JSON 基准输出和 verify-only 模式，用于重启后按同一批交易校验补扫结果。

## 待优化点

1. workflow 处理需要按 `chainId + contract + txHash + logIndex` 做幂等。补扫和实时订阅在重启边界可能重复处理事件，后续如果落库或触发外部流程必须避免重复副作用。
2. 当前 handler 失败后会停止推进 checkpoint，但还没有失败事件表、重试队列或 dead-letter 机制。建议持久化失败原因、重试次数、下一次重试时间，并提供人工重放入口。
3. 将本轮手工编排的 Docker Besu 断档恢复验收固化为 npm/Gradle 任务，并纳入 CI。
4. 增加监听运行指标：当前链高、checkpoint 高度、checkpoint 事件游标、落后区块数、补扫日志数、handler 失败数、重试次数。
5. 事件日志不宜长期只依赖 stdout 解析。建议输出结构化日志或持久化事件审计表，便于生产环境追踪和重放。

## 产物路径

- `build/topaz-smoke-event-checkpoint-after-restart.md`：最终通过报告
- `build/topaz-smoke-event-checkpoint-after-restart.json`：最终机器可读报告
- `build/topaz-smoke-event-checkpoint-missed.md`：故意断档后的中间报告
- `build/topaz-smoke-event-checkpoint-missed.json`：故意断档后的机器可读基准
- `build/topaz-listener-event-checkpoint.log`：旧表自动补列与启动补扫日志
- `build/topaz-listener-event-checkpoint-restart.log`：服务重启补扫日志
- `build/topaz-listener-transaction-startup.log`：事务改造后真实服务启动与补扫日志
- `build/topaz-listener-from-block-startup.log`：首次回扫起点配置改造后真实服务启动与补扫日志
