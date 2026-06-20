# 开放问题与风险报告

## 需要确认的问题

### 1. 目标链环境

目标链已确定为 Docker 中部署的 Besu：

- Docker 容器名：`ep-besu`
- 镜像：`hyperledger/besu:24.9.1`
- 宿主机 RPC：`http://127.0.0.1:8546`
- chainId：`1337`
- `eth_accounts`：空数组，部署和后端交易必须使用配置文件里的私钥本地签名

第一版后端和部署脚本都使用这条 Docker Besu 链，不再以 Hardhat node 作为目标链。

### 2. chainId

chainId 已确认：

- `eth_chainId = 0x539`
- 十进制 `1337`

后端签名 raw transaction 使用 `chainId = 1337`。

### 3. TopazLifecycle 合约地址

后端必须配置 `TopazLifecycle` 地址。当前项目没有部署输出文件或配置注入机制。

可以由现有部署流程提供一个稳定 JSON，例如：

```json
{
  "topazPayment": "0x...",
  "topazLifecycle": "0x...",
  "topazContacts": "0x...",
  "chainId": "1337"
}
```

后端再通过配置文件读取。这里不要求修改 Solidity 合约；是否调整部署脚本由部署流程决定。

### 4. 后端 signer 来源

当前私钥策略：

- 部署私钥配置在部署配置文件的 `deployerPrivateKey`。
- 后端项目 officer 私钥配置在 `src/main/resources/application.yaml` 的 `ep.chain.signer-private-key`。

生产上仍建议迁移到 wallet file、KMS、HSM 或 Kaleido wallet。

### 5. 角色授权矩阵

当前部署脚本只授了 `TopazPayment.LIFECYCLE_ROLE -> TopazLifecycle`，没有给后端 signer 授 `PROJECT_OFFICER_ROLE`。没有该角色，`createProject` 必定失败。

推荐方案：

- 通过脚本生成/复用稳定钱包。
- `projectOfficerBackend` 钱包作为后端 createProject signer；第一版也可以让 `financeOperator`、`paymentOperator` 复用同一个钱包地址。
- 部署后运行幂等授权脚本，至少校验并授予：
  - `TopazPayment.LIFECYCLE_ROLE -> TopazLifecycle contract address`
  - `TopazLifecycle.PROJECT_OFFICER_ROLE -> projectOfficerBackend`
  - `TopazLifecycle.FINANCE_ROLE -> financeOperator`
  - `TopazPayment.PAYMENT_OPERATOR_ROLE -> paymentOperator`
- `SETTLEMENT_BANK_ROLE` 当前合约没有直接函数使用，暂不作为第一版必授角色。
- 角色别名保留不变，即使多个角色暂时 grant 到同一个地址，后续拆分钱包也不需要改角色模型。
- 授权脚本先 `hasRole`，没有再 `grantRole`，grant 后再次校验。

详细方案见：[08-stable-wallets-and-role-grant-plan.md](/Users/dinglujie/myself/workspace/ep/ep-migration/ep-migration/docs/research/08-stable-wallets-and-role-grant-plan.md)。

### 6. 同步等待策略

当前策略已确定：

- 立即返回 txHash。
- 不等待 receipt。
- 不返回 projectId、blockNumber 或链上 status。
- 交易最终是否成功由后续查询或链上浏览工具确认。

### 7. 重复请求语义

合约遇到重复项目会 revert `DuplicateProjectId`。

需要确认业务期望：

- 返回 409，提示重复。
- 查询已有 project 并返回。
- 后端保存第一次 txHash，实现真正幂等。

当前没有数据库，推荐第一版返回 409。

## 技术风险

### web3j wrapper 生成风险

`createProject` ABI 有嵌套 tuple 和 tuple arrays，合约使用 custom errors。实测 web3j 4.8.7 codegen 不支持当前 tuple ABI；web3j 4.9.8 codegen 可以生成并编译通过。

缓解：

- 当前使用 web3j 4.9.8 generated wrapper。
- 加 calldata 与 ethers 编码一致性测试。
- 不在运行时代码中维护自定义 ABI parser 或自定义 encoder。
- event decoding 不进入第一版 createProject API；后续查询能力需要时再补。

### custom errors 解析风险

合约错误包括：

- `DuplicateProjectId(string)`
- `InvalidInput(string)`
- `AccessControlUnauthorizedAccount(address,bytes32)`

web3j 4.9.8 不应被假设能自动解析 Solidity custom errors。

当前第一版不等待 receipt。因此这些 custom errors 不会在当前 HTTP 请求内被稳定解析。

缓解：

- 第一版只把 `eth_sendRawTransaction` 阶段的 RPC code/message、nonce、from、to、txHash 记录清楚。
- 接口成功只表示 submitted，不承诺合约业务执行成功。
- 后续如果需要同步判断 `DuplicateProjectId`、`InvalidInput` 或权限错误，应单独设计 receipt/event 查询或状态同步能力。

### nonce 并发风险

多个请求用同一 signer 并发发送交易时，可能出现 nonce 竞争。

缓解：

- 第一版使用 `ResilientNonceManager` 对单 signer 串行分配 nonce。
- nonce 错误后从 `eth_getTransactionCount(PENDING)` 重同步并重试一次。
- 暂不引入 Redis/数据库分布式 nonce store。

### 立即返回 txHash 的语义风险

后端返回 txHash 只表示交易已被节点接受，不表示交易已经出块或执行成功。

缓解：

- 响应字段命名使用 `transactionHash` 和提交状态，不写成“project created”。
- 前端用 txHash 展示 pending 状态。
- 后续如需要最终状态，再补交易查询接口或状态同步能力。

### Revert 可观测性风险

revert 交易不会保留合约 event logs，当前 Docker Besu 也没有开启 `--revert-reason-enabled` 或 `DEBUG` / `TRACE` API。因此不能承诺“已上链失败交易一定能从 event logs 看到原因”。

缓解：

- 每个请求打印结构化日志和 `correlationId`。
- 记录提交阶段 RPC code/message、nonce、from、to、contractAddress、transactionHash。
- 提交成功后立即返回 txHash，不把 submitted 误写成 created。
- 后续需要最终执行结果时，再补 receipt/event 查询或状态同步能力。

详细方案见：[09-observability-and-revert-diagnostics.md](/Users/dinglujie/myself/workspace/ep/ep-migration/ep-migration/docs/research/09-observability-and-revert-diagnostics.md)。

### 合约地址错误风险

如果配置了错误的 `TopazLifecycle` 地址：

- raw transaction 仍可能被节点接受。
- 交易提交成功后，后端当前不会等待 receipt 验证最终结果。

缓解：

- 启动时检查 `eth_getCode(contractAddress)` 非空。
- 可选检查 `hasRole(PROJECT_OFFICER_ROLE, signer)`。

### 重复项目处理边界风险

重复项目判断由现有合约负责。后端如果自行复刻或优化去重规则，可能与合约实际行为不一致。

缓解：

- 后端不额外做重复项目判定。
- 后端只捕获 `DuplicateProjectId` 并返回 409。
- 不改 Solidity 合约的去重逻辑。

## 安全风险

### 私钥泄漏

后端 signer 私钥是高敏感信息。

缓解：

- 不打印完整 private key。
- 当前开发阶段按要求写在配置文件里；不要在日志、异常响应或报告里打印真实值。
- 生产考虑 wallet file、KMS、HSM、Kaleido wallet。

### RPC 暴露

Besu JSON-RPC 如果绑定 `0.0.0.0` 且无保护，会暴露链访问面。

缓解：

- RPC 默认内网访问。
- Kaleido 使用 App Credentials。
- 自建 Besu 使用防火墙、反向代理认证、TLS 或 Besu authentication。
- 不把 admin/debug API 暴露给业务后端，除非明确需要。

### 过度信任前端地址

前端传入 developer、contractor、approver wallet。后端只做格式校验，业务真实性需要外部身份系统保证。

缓解：

- 当前迁移项目可以保留链上规则为准。
- 如果需要账户权限校验，应接入业务身份系统，而不是在 wrapper 层硬编码。

## 推荐下一步

1. 使用 Docker Besu RPC `http://127.0.0.1:8546` 和 chainId `1337`。
2. 确认 signer 地址和授权状态。
3. 设计稳定钱包生成脚本和角色授权矩阵脚本。
4. 实现后端 wrapper 和 API。
5. 用 Docker Besu 完成端到端验证。
6. 验证交易提交后能立即返回 txHash。

## 资料来源

- 本地部署脚本：`contracts/scripts/deploy.js`。
- 本地访问控制：`contracts/contracts/TopazAccessControl.sol`。
- 本地生命周期合约：`contracts/contracts/TopazLifecycle.sol`。
- Besu API：[Use Besu APIs](https://docs.besu-eth.org/public-networks/how-to/use-besu-api)。
- Besu authentication：[Authenticate JSON-RPC requests](https://docs.besu-eth.org/public-networks/how-to/use-besu-api/authenticate)。
- Besu transactions：[Send transactions](https://docs.besu-eth.org/public-networks/how-to/send-transactions)。
- Kaleido Platform API：[Kaleido Platform API](https://api.kaleido.io/platform.html)。
- Kaleido Eth Wallet API：[Eth Wallet API](https://api.kaleido.io/ethwallet.html)。
- Kaleido EthConnect API：[EthConnect API](https://api.kaleido.io/ethconnect.html)。
