# Topaz 事件监听全方位 QA 最终报告

生成时间：2026-07-01

## 总结

本轮按 Todo 顺序完成环境、覆盖面、静态一致性、自动化测试、真实链监听、故意断档、重启补扫和边界测试。

结论：核心事件监听与重启补扫通过。30 个事件类型全部覆盖，实时监听 52/52 通过，断档后重启补扫 52/52 通过，同一区块中间游标恢复通过。

发现 1 个 P1 边界缺陷：首次启动无 checkpoint 且 `ep.chain.listener-from-block` 大于当前链头时，服务只记录 ERROR，但仍继续启动并保持 `checkpoint=null`。

后续已优化 checkpoint 部署隔离：checkpoint identity 已绑定 `listener_name + chain_id + lifecycle/payment/contacts contract address`，重新部署合约后不会复用旧合约处理进度。

## Todo 执行结果

| Todo | 测试点 | 结果 | 证据 |
|---:|---|---|---|
| 1 | 环境与进程基线 | 通过 | Besu/Postgres 运行，RPC 返回区块，Postgres 可用，18081/18082 无残留服务 |
| 2 | 30 个事件覆盖面审计 | 通过 | ABI、注册表、workflow handler、smoke 目标均为 18 + 7 + 5 = 30 |
| 3 | checkpoint、事务、回扫起点静态一致性 | 通过 | 事件级 cursor、事务封装、恢复排序、配置项均存在；`git diff --check` 通过 |
| 4 | Gradle JVM 单元测试 | 通过 | 81 / 81 |
| 5 | Docker Besu 集成测试 | 通过 | 1 / 1，强制 `--rerun-tasks` 执行 |
| 6 | Hardhat 合约测试 | 通过 | 12 / 12 |
| 7 | 真实链部署与角色准备 | 通过 | 三合约重新部署，角色授权完成 |
| 8 | 实时监听全事件验收 | 通过 | 30 个事件类型、52 个事件检查全部 PASS |
| 9 | 故意漏监听验收 | 通过 | 服务停止后产生 52 个链上事件，workflow 0/52，符合预期断档 |
| 10 | 重启补扫验收 | 通过 | 补扫 `268258..268405`，`logs=52 handled=52`，恢复后 52/52 PASS |
| 11 | checkpoint 边界验收 | 部分通过 | 负数起点、有效起点、同区块恢复通过；超链头起点发现 P1 |
| 12 | 最终报告 | 完成 | 本文档 |
| 13 | checkpoint 部署 identity 优化回归 | 通过 | 旧 4 列表自动补 identity 列；legacy checkpoint 保留但不复用；当前部署写入独立 checkpoint |

## 测试统计

| 类别 | 数量 | 通过 | 未通过 | 备注 |
|---|---:|---:|---:|---|
| Gradle 单元测试 | 81 | 81 | 0 | `./gradlew cleanTest test` |
| Docker Besu 集成测试 | 1 | 1 | 0 | `./gradlew integrationTest --rerun-tasks` |
| Hardhat 合约测试 | 12 | 12 | 0 | `npm test` |
| 实时监听事件检查 | 52 | 52 | 0 | 30 个事件类型全部覆盖 |
| 断档链上事件检查 | 52 | 52 | 0 | workflow 缺失为预期，不计缺陷 |
| 重启补扫事件检查 | 52 | 52 | 0 | workflow 和参数全部补回 |
| 边界场景 | 4 | 3 | 1 | 发现 1 个 P1 |
| checkpoint identity 迁移验证 | 1 | 1 | 0 | 真实 Postgres 旧表补列、旧部署处理进度隔离 |

可计数合计：255 项验证，其中 254 项通过，1 项发现缺陷。

## 缺陷表

| 优先级 | 类型 | 问题 | 影响 | 证据 | 建议修复 |
|---|---|---|---|---|---|
| P1 | Bug | 首次启动无 checkpoint 且 `listener-from-block > latestBlock` 时，服务记录 ERROR 后仍启动，checkpoint 保持 null | 配置错误时服务看似可用，但历史补扫没有完成；后续实时订阅仍可能运行，容易造成运维误判和数据不一致 | `build/full-qa-too-high-from-block.log`：`Topaz event compensation failed on startup...` 后仍出现 `Topaz contract event listener started ... checkpoint=null` | 启动前校验 `listener-from-block <= latestBlock`；对配置错误不要被 `logFailure` 吞掉。建议补扫失败时不设置 running，释放订阅，并让应用启动失败或暴露 unhealthy |

## 需优化项

| 优先级 | 优化点 | 原因 | 优化方式 |
|---|---|---|---|
| P1 | workflow 幂等 | 补扫和实时监听边界可能重复处理同一事件，未来如果 handler 落库或调外部系统会产生重复副作用 | 以 `chainId + contractAddress + txHash + logIndex` 建唯一键；handler 先写事件处理表，已处理则跳过 |
| P1 | 补扫失败状态显式化 | 现在部分启动补扫异常会被日志捕获，外部不一定知道服务处于不完整状态 | 区分配置错误、RPC 错误、handler 错误；配置错误直接 fail fast，运行时错误写健康状态和告警指标 |
| P2 | 失败事件表和重试队列 | handler 或 checkpoint 失败时目前只停止本轮补扫，没有 dead-letter 或人工重放入口 | 建 `topaz_event_failure` 表，记录事件坐标、异常、重试次数、next_retry_at；提供重放任务 |
| P2 | 将真实链断档恢复验收固化到 CI | 本轮端到端测试是手工编排，回归成本高 | 封装 Gradle/npm 任务：启动服务、生成断档、重启补扫、读取 JSON 报告，CI 中跑 Docker Besu profile |
| P2 | 监听指标 | 生产环境需要知道落后高度、补扫速度、失败数量 | 暴露当前链高、checkpoint 高度、event cursor、lag blocks、handled logs、failed handlers、retry count |
| P3 | 结构化审计表 | 只依赖 stdout 日志不利于长期审计和重放 | 持久化事件审计表，保存事件参数摘要、处理状态、处理耗时 |
| P3 | 清理编译 warning | Gradle 编译存在 deprecated API warning，长期会影响升级 | 替换 deprecated tuple getter / Web3j load API；处理 codegen 阶段 SLF4J NOP warning |

## 已修复项

| 优先级 | 已修复内容 | 验证 |
|---|---|---|
| P0 | checkpoint 从区块级升级为事件级 cursor：`processed_block + processed_tx_hash + processed_log_index` | 单测通过；真实链同区块中间游标恢复 `logs=3 handled=3` |
| P0 | 事件处理和事件 checkpoint 写入放入同一事务 | 单测验证 commit/rollback；checkpoint 写入失败会 rollback 并停止 |
| P0 | handler 失败不推进完整区块 checkpoint | 单测验证失败后不越过失败事件 |
| P1 | checkpoint 绑定部署 identity，避免重新部署合约后复用旧处理进度 | 新增单测验证旧部署 checkpoint 被忽略；真实 Postgres 验证旧表自动补列并写入当前部署独立 checkpoint |
| P1 | 首次启动支持 `ep.chain.listener-from-block` 配置回扫起点 | 有效起点真实启动回扫 `268553..268566` 通过 |
| P1 | 30 个事件统一注册、解码、路由、workflow 输出参数 | 覆盖审计和 smoke 验证均通过 |

## 产物

| 文件 | 内容 |
|---|---|
| `build/full-qa-realtime-listener.log` | 实时监听服务日志 |
| `build/full-qa-realtime-smoke.md` / `.json` | 实时监听事件验收报告 |
| `build/full-qa-missed-smoke.md` / `.json` | 故意断档事件基准 |
| `build/full-qa-restart-listener.log` | 重启补扫服务日志 |
| `build/full-qa-recovered-smoke.md` / `.json` | 补扫恢复验收报告 |
| `build/full-qa-negative-from-block.log` | 负数回扫起点失败验证 |
| `build/full-qa-too-high-from-block.log` | 超链头回扫起点缺陷证据 |
| `build/full-qa-valid-from-block.log` | 有效首次起点验证 |
| `build/full-qa-midblock-recovery.log` | 同一区块事件游标恢复验证 |
| `build/checkpoint-identity-startup.log` | checkpoint identity 迁移与当前部署独立 checkpoint 验证 |
