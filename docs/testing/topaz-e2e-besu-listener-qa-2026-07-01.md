# Topaz Docker Besu 真实交易与漏监听 E2E QA 报告

生成时间：2026-07-01

## 结论

本轮按 todo 执行真实 Docker Besu 合约部署、真实 API 交易、实时监听、服务离线漏监听、重启补扫和启动边界测试。当前执行结果未发现未修复的 P0/P1 阻断缺陷。

已修复历史 P1：首次启动无 checkpoint 且 `ep.chain.listener-from-block` 大于当前链头时，监听器过去只记录错误但仍启动。现在启动补扫失败会清理订阅、重置 running 状态并向上抛错，Spring 启动失败，不再伪装可用。

工程上不能承诺绝对 0 风险；本报告只声明本轮覆盖范围内没有未处理失败。剩余生产化风险列在文末，不能隐藏。

## 环境

| 项 | 值 |
|---|---|
| Besu 容器 | `ep-besu` |
| Besu 镜像 | `hyperledger/besu:24.9.1` |
| RPC | `http://127.0.0.1:8546` |
| chainId | `1337` |
| Postgres 容器 | `ep-postgres` |
| Postgres | `127.0.0.1:15432/ep_migration` |
| 服务端口 | `18081` |

本轮重新部署合约：

| 合约 | 地址 |
|---|---|
| `TopazPayment` | `0xFa40DafE65dC1999f270Ca709D294eFf59783CBA` |
| `TopazLifecycle` | `0xC524040473a4AFc9f036E37343e431b220EFda5f` |
| `TopazContacts` | `0x67a44fDa468CA6Cfc44450E7ECFecc9A761d22a9` |

## Todo 执行结果

| Todo | 验收点 | 结果 | 证据 |
|---:|---|---|---|
| 1 | 环境基线 | 通过 | Docker Besu/Postgres 运行；RPC `eth_chainId=0x539`；18081/8081 无残留监听 |
| 2 | 合约部署与角色 | 通过 | `npm run deploy:docker-besu && npm run roles:grant` 成功 |
| 3 | JVM 单元测试 | 通过 | 83 tests, 0 failure, 0 error, 0 skipped |
| 4 | Hardhat 合约测试 | 通过 | 12 passing |
| 5 | Docker Besu JUnit 集成测试 | 通过 | 1 test, 0 failure |
| 6 | API 真实交易 | 通过 | tx `0x0059661f5ca070e2b1bdd909fc3c72819c55535ced065d415d60f34c43797e00`，receipt status=1 |
| 7 | 链上读回 | 通过 | `ProjectCreated`、`ProjectStatusChanged` 存在；`getProjectSummary(projectId=2)` 与请求一致 |
| 8 | Postgres 落库 | 通过 | `project/project_participant/project_approver` 与链上 summary 一致 |
| 9 | 实时监听 | 通过 | 30 行事件类型、52 个事件检查，emitted/workflow/参数均 52/52 |
| 10 | 故意漏监听 | 通过 | 服务停止后发 52 个链上事件，workflow 0/52，证明断档真实存在 |
| 11 | 重启补扫 | 通过 | 补扫 `273021..273149 logs=52 handled=52 completedThroughBlock=273149` |
| 12 | 恢复校验 | 通过 | 断档 JSON verify-only：workflow/参数恢复 52/52 |
| 13 | 超链头起点边界 | 通过 | `listener-from-block=999999999` 启动失败，且没有 `listener started` |
| 14 | 静态检查 | 通过 | `git diff --check` 无输出 |

## 关键修复

- `TopazContractEventListener.start()` 在启动补扫失败时不再吞异常。
- 失败时会 dispose 当前订阅、清空 `subscriptionStream`、将 `running=false`，然后重新抛出异常。
- 新增单元测试：
  - `start fails and stops subscription when startup compensation fails`
  - `listener rejects negative configured listener block`

## 关键产物

| 文件 | 内容 |
|---|---|
| `build/e2e-api-response.json` | API 真实交易响应 |
| `build/e2e-api-chain-verification.json` | receipt/event/链上 summary 校验 |
| `build/e2e-realtime-smoke.json` / `.md` | 实时监听 30 类事件报告 |
| `build/e2e-missed-smoke.json` / `.md` | 服务离线期间断档基准 |
| `build/e2e-recovered-smoke.json` / `.md` | 重启补扫恢复报告 |
| `build/e2e-restart-service.log` | 重启补扫日志 |
| `build/e2e-too-high-from-block.log` | 超链头起点 fail-fast 证据 |

## 仍需生产化处理

1. workflow 幂等仍需落到持久化事件处理表。建议唯一键使用 `chainId + contractAddress + txHash + logIndex`，否则未来 handler 一旦落库或调用外部系统，重复补扫可能产生重复副作用。
2. handler 失败目前会停止推进 checkpoint，但还缺失败事件表、重试队列、dead-letter 和人工重放入口。
3. 真实链断档恢复脚本仍是手工编排，应固化成 Gradle/npm 任务并纳入 CI。
4. 生产监控还缺 listener lag、当前链高、checkpoint 高度、处理速率、失败次数、重试次数等指标。
5. 当前 workflow 只输出日志，不等同于完整业务补偿落库；如果业务要求事件驱动落库，需要实现并重新做 E2E。
