# 稳定钱包与角色授权脚本方案

## 结论

推荐新增一套脚本化 bootstrap 流程：

```text
generate stable role wallets
  -> deploy contracts with deployerAdmin wallet
  -> grant wallet roles and contract roles with idempotent role script
  -> write deployment/role summary JSON
```

核心目标：

- 不手动 grant 业务角色。
- 使用几个稳定角色钱包，避免每次重启/部署 signer 变化；角色别名不强制对应不同地址。
- 明确区分钱包主体和合约主体：`TopazPayment.LIFECYCLE_ROLE` 授给 `TopazLifecycle` 合约地址，不授给钱包。
- 脚本可重复执行，已授权则跳过。
- 不改 Solidity 合约。
- 当前仍是方案阶段，不创建脚本文件。

## 当前链前提

当前 Docker Besu：

```text
container: ep-besu
image: hyperledger/besu:24.9.1
host rpc: http://127.0.0.1:8546
chainId: 1337
eth_accounts: []
command includes: --network=dev --min-gas-price=0
```

`eth_accounts` 为空意味着 Besu 不负责签名。部署和 grant role 必须由脚本使用本地 private key 签 raw transaction。

具体到 Hardhat：`hardhat.config.js` 的 `localhost` 网络必须配 `accounts: [<deployerAdmin.privateKey>]`，否则 `hre.ethers.getSigners()` 返回空，`deploy.js` 和 `grant-roles.js` 都会因为拿不到 signer 直接报错。`localhost` 还要配 `chainId: 1337`，让 ethers 用正确的 chainId 签 EIP-155 交易。

另一个关键点：脚本生成的钱包如果没有余额，不能部署或发交易。当前容器没有挂载自定义 genesis，因此稳定钱包生成脚本本身不能保证新钱包有资金。

## 推荐角色主体集合

第一版建议固定这些钱包和合约主体：

```text
deployerAdmin                 // admin signer
projectOfficerBackend          // backend signer for PROJECT_OFFICER_ROLE
financeOperator                // backend/operator signer for FINANCE_ROLE
paymentOperator                // operator signer for TopazPayment.PAYMENT_OPERATOR_ROLE
topazLifecycle contract address // contract principal for TopazPayment.LIFECYCLE_ROLE
```

这里的 `projectOfficerBackend`、`financeOperator`、`paymentOperator` 是角色别名，不要求第一版必须是三个不同钱包。为了降低配置和运维复杂度，第一版可以让这三个别名都指向同一个后端业务钱包地址；授权脚本仍按角色矩阵 grant，只是 grantee address 相同。这样不改变多角色设计，后续需要职责分离时再把别名指向不同钱包即可。

用途：

- `deployerAdmin`
  - 部署 `TopazPayment`、`TopazLifecycle`、`TopazContacts`。
  - 合约构造函数里的 admin。
  - 拥有 `DEFAULT_ADMIN_ROLE`、`SUPER_ADMIN_ROLE`、`ADMIN_ROLE`。
  - 执行 role grant 脚本。
- `projectOfficerBackend`
  - 后端 `application.yaml` 中的 `ep.chain.signer-private-key`。
  - 必须被授予 `TopazLifecycle.PROJECT_OFFICER_ROLE`。
  - 后端调用 `createProject`、`updateProject`、project officer approval/rejection 类写操作时使用这个钱包签名。
- `financeOperator`
  - invoice/payment order 流程需要 `TopazLifecycle.FINANCE_ROLE` 时使用。
- `paymentOperator`
  - 后续 payment 模块需要 `TopazPayment.PAYMENT_OPERATOR_ROLE` 时使用。
- `topazLifecycle contract address`
  - 必须被授予 `TopazPayment.LIFECYCLE_ROLE`。
  - 原因：`TopazLifecycle.createPaymentOrder(...)` 内部会调用 `TopazPayment.createPayment(...)`，此时 `msg.sender` 是 `TopazLifecycle` 合约地址。

当前 createProject 最小必要钱包只有：

```text
deployerAdmin
projectOfficerBackend
```

但多角色设计需要同时覆盖后续 payment/order 流程，因此建议第一版 bootstrap 就保留 `financeOperator` 和 `paymentOperator` 这两个角色别名。它们可以暂时复用 `projectOfficerBackend` 的同一个地址和 private key。`SETTLEMENT_BANK_ROLE` 当前合约没有直接函数使用，先不作为必授角色；后续合约或流程真正使用时再增加 `settlementBankOperator`。

## 钱包生成方式

推荐用一个脚本生成并持久化钱包配置：

```text
contracts/scripts/generate-wallets.js
```

设计要求：

- 第一次运行时生成稳定钱包配置文件。
- 如果配置文件已存在，直接读取并打印地址，不覆盖。
- 每个角色别名输出 address 和 privateKey；第一版允许多个业务角色别名的 address/privateKey 相同。
- 输出 role name，方便后续脚本读取。
- 不在控制台打印完整 private key，除非显式 `--show-private-keys`。

建议配置文件形态：

```json
{
  "chainId": 1337,
  "wallets": {
    "deployerAdmin": {
      "address": "0x...",
      "privateKey": "0x..."
    },
    "projectOfficerBackend": {
      "address": "0x...",
      "privateKey": "0x..."
    },
    "financeOperator": {
      "address": "0x...",
      "privateKey": "0x..."
    },
    "paymentOperator": {
      "address": "0x...",
      "privateKey": "0x..."
    }
  }
}
```

如果第一版只配置一个后端业务钱包，`projectOfficerBackend`、`financeOperator`、`paymentOperator` 三个条目可以写成同一个 `address` 和 `privateKey`。这样比删除角色别名更好，因为脚本输出、授权矩阵和后端后续拆分路径都保持稳定。

文件名建议：

```text
contracts/config/wallets.local.json
```

后端 `application.yaml` 中的 `ep.chain.signer-private-key` 使用 `projectOfficerBackend.privateKey`。

部署脚本使用 `deployerAdmin.privateKey`。

## 稳定性的两种实现

### 方案 A：生成一次并保存

第一次运行：

```text
npm run wallets:generate
```

脚本随机生成钱包，写入 `wallets.local.json`。之后所有脚本读取这个文件。

优点：

- 简单。
- 钱包不会每次变化。
- 不需要固定 mnemonic。

缺点：

- 文件丢失后钱包不可恢复。
- 如果 Docker 链没有给这些地址余额，需要额外 funding。

### 方案 B：固定 mnemonic 派生

配置一个本地开发 mnemonic，用 BIP-44 路径派生：

```text
m/44'/60'/0'/0/0 deployerAdmin
m/44'/60'/0'/0/1 projectOfficerBackend
m/44'/60'/0'/0/2 financeOperator
m/44'/60'/0'/0/3 paymentOperator
```

如果采用单后端业务钱包，`financeOperator` 和 `paymentOperator` 可以直接复用 `projectOfficerBackend` 的派生结果，不必额外派生新地址。

优点：

- 可重建。
- 行业内本地开发链很常见。
- 方便生成 genesis alloc。

缺点：

- mnemonic 仍是敏感配置。

推荐第一版用方案 A，最简单；如果后续要重建 Docker 链并保持地址完全一致，再切到方案 B。

## 资金前提

稳定钱包要能执行部署和授权，必须有余额。

当前 Docker Besu 使用 `--network=dev`，没有项目内 genesis 文件。可选路径：

1. 使用 Docker dev network 已有预置资金账户作为 `deployerAdmin`。
   - 需要拿到该账户 private key。
   - 用它部署合约，并给后端业务钱包地址转一些 native token。
2. 后续改 Docker 链为自定义 genesis。
   - 先运行 `wallets:generate`。
   - 把 `deployerAdmin` 和业务钱包地址写入 genesis `alloc`。如果三个业务角色别名共用同一地址，只需要 alloc 一次。
   - 重建 Docker 链。

最稳的长期方案是第 2 种：稳定钱包 + 自定义 genesis alloc。当前方案阶段先记录，不改 Docker。

## 授权脚本方案

推荐新增：

```text
contracts/scripts/grant-roles.js
```

输入：

- `contracts/config/wallets.local.json`
- deployment output，例如 `contracts/deployments/docker-besu-1337.json`

脚本连接：

```text
rpcUrl: http://127.0.0.1:8546
chainId: 1337
signer: deployerAdmin
```

授权动作：

```text
TopazPayment.LIFECYCLE_ROLE -> TopazLifecycle contract address
TopazLifecycle.PROJECT_OFFICER_ROLE -> projectOfficerBackend.address
TopazLifecycle.FINANCE_ROLE -> financeOperator.address
TopazPayment.PAYMENT_OPERATOR_ROLE -> paymentOperator.address
```

当前 createProject 最小必需：

```text
TopazPayment.LIFECYCLE_ROLE -> TopazLifecycle contract address
TopazLifecycle.PROJECT_OFFICER_ROLE -> projectOfficerBackend.address
```

`TopazPayment.LIFECYCLE_ROLE` 虽然不是 createProject 直接需要，但它是当前部署脚本已经执行的跨合约授权，属于 bootstrap 必检项。`FINANCE_ROLE` 和 `PAYMENT_OPERATOR_ROLE` 可在脚本中作为默认授权项保留，方便后续 invoice/payment order 流程。`SETTLEMENT_BANK_ROLE` 当前没有直接函数使用，不建议第一版默认 grant。

## 角色覆盖矩阵

矩阵中的主体名表示角色别名。第一版可以让 `projectOfficerBackend`、`financeOperator`、`paymentOperator` 指向同一个钱包地址；矩阵仍保持三行，因为链上检查的是“某个地址是否拥有某个角色”。

| 主体 | 合约 | 角色 | 当前用途 | 第一版状态 |
| --- | --- | --- | --- | --- |
| `deployerAdmin` | `TopazLifecycle` | `DEFAULT_ADMIN_ROLE` / `SUPER_ADMIN_ROLE` / `ADMIN_ROLE` | 合约构造函数授予，执行 lifecycle 授权管理 | 必需 |
| `deployerAdmin` | `TopazPayment` | `DEFAULT_ADMIN_ROLE` | 合约构造函数授予，执行 payment 授权管理 | 必需 |
| `TopazLifecycle` 合约地址 | `TopazPayment` | `LIFECYCLE_ROLE` | 允许 lifecycle 调 `TopazPayment.createPayment` | 必需 |
| `projectOfficerBackend` | `TopazLifecycle` | `PROJECT_OFFICER_ROLE` | create/update project、project officer approve/reject claim/invoice | createProject 必需 |
| `financeOperator` | `TopazLifecycle` | `FINANCE_ROLE` | update bank accounts、finance approve/reject invoice、create/resubmit payment order、record bank ref | 后续流程建议默认授予 |
| `paymentOperator` | `TopazPayment` | `PAYMENT_OPERATOR_ROLE` | accept/reject payment、create payment receipt | 后续流程建议默认授予 |
| `settlementBankOperator` | `TopazLifecycle` | `SETTLEMENT_BANK_ROLE` | 当前合约未直接使用 | 暂不授予 |

## 授权脚本必须幂等

脚本执行每个 grant 前先查：

```text
hasRole(role, address)
```

如果已经有 role：

```text
skip
```

如果没有：

```text
grantRole(role, address)
wait receipt
verify hasRole(role, address)
```

这样脚本可以重复跑，失败后也可以重跑。

## 推荐 npm scripts

后续实施可增加：

```json
{
  "wallets:generate": "hardhat run scripts/generate-wallets.js",
  "deploy:docker-besu": "hardhat run --network localhost scripts/deploy.js",
  "roles:grant": "hardhat run --network localhost scripts/grant-roles.js",
  "bootstrap:docker-besu": "npm run wallets:generate && npm run deploy:docker-besu && npm run roles:grant"
}
```

当前 `deploy:local` 名字也可以沿用，但从可读性看，`deploy:docker-besu` 更准确。

## 部署输出建议

`deploy.js` 后续应输出：

```json
{
  "network": "docker-besu",
  "chainId": 1337,
  "deployer": "0x...",
  "contracts": {
    "topazPayment": "0x...",
    "topazLifecycle": "0x...",
    "topazContacts": "0x..."
  }
}
```

建议路径：

```text
contracts/deployments/docker-besu-1337.json
```

授权脚本读取该文件，不靠人工复制合约地址。

## 授权输出建议

`grant-roles.js` 后续应输出：

```json
{
  "chainId": 1337,
  "topazLifecycle": "0x...",
  "topazPayment": "0x...",
  "roles": {
    "TopazPayment.LIFECYCLE_ROLE": {
      "granteeType": "contract",
      "address": "0x...",
      "contract": "topazLifecycle",
      "granted": true
    },
    "PROJECT_OFFICER_ROLE": {
      "contract": "topazLifecycle",
      "address": "0x...",
      "wallet": "projectOfficerBackend",
      "granted": true
    },
    "FINANCE_ROLE": {
      "contract": "topazLifecycle",
      "address": "0x...",
      "wallet": "financeOperator",
      "granted": true
    },
    "PAYMENT_OPERATOR_ROLE": {
      "contract": "topazPayment",
      "address": "0x...",
      "wallet": "paymentOperator",
      "granted": true
    }
  }
}
```

建议路径：

```text
contracts/deployments/docker-besu-1337.roles.json
```

## 后端配置联动

后端 `application.yaml` 后续使用：

```yaml
ep:
  chain:
    rpc-url: http://127.0.0.1:8546
    chain-id: 1337
    lifecycle-contract-address: "0x..." # from deployment output
    signer-private-key: "0x..."         # projectOfficerBackend.privateKey
```

`projectOfficerBackend` 必须和 `grant-roles.js` 里授予 `PROJECT_OFFICER_ROLE` 的地址一致。

第一版 `POST /api/projects` 只需要 `projectOfficerBackend`。如果当前希望一个后端业务钱包完成所有后端写操作，就让 `financeOperator`、`paymentOperator` 在钱包配置里复用同一个地址，并把 `FINANCE_ROLE`、`PAYMENT_OPERATOR_ROLE` 也授给这个地址。

后续如果需要拆分职责，有两种选择：

1. 每类业务配置独立 private key，例如 `project-officer-private-key`、`finance-private-key`、`payment-operator-private-key`。
2. 继续使用一个 backend signer 并给它授多个角色。

更清晰的长期方案是第 1 种；如果当前只实现 createProject，不要提前把多个业务 signer 注入后端。

## 验证步骤

脚本化验证：

```text
npm run wallets:generate
npm run deploy:docker-besu
npm run roles:grant
```

然后检查：

```text
TopazPayment.hasRole(LIFECYCLE_ROLE, topazLifecycle.address) == true
TopazLifecycle.hasRole(PROJECT_OFFICER_ROLE, projectOfficerBackend.address) == true
TopazLifecycle.hasRole(FINANCE_ROLE, financeOperator.address) == true
TopazPayment.hasRole(PAYMENT_OPERATOR_ROLE, paymentOperator.address) == true
```

后端启动前可选做启动检查：

```text
hasRole(PROJECT_OFFICER_ROLE, signerAddress)
```

如果 false，直接启动失败，避免 createProject 请求时才暴露配置错误。

## 自愈能力

授权脚本的自愈点：

- 钱包配置存在则复用，不覆盖。
- deployment output 存在则读取，不要求人工复制地址。
- role 已存在则跳过。
- grant 交易失败后可重跑。
- grant 后立即 `hasRole` 校验。
- nonce 错误时可用 ethers `NonceManager` 或项目 nonce 方案重试一次。

## 不做的事

- 不改 Solidity 合约。
- 不在后端动态 grant role。
- 不把角色授权放进业务请求路径。
- 不依赖 Besu `eth_accounts`。
- 不手动在控制台执行 grant 命令作为常态流程。

## 资料来源

- 本地部署脚本：`contracts/scripts/deploy.js`。
- 本地测试 fixture：`contracts/test/topaz.test.js`，其中已用脚本方式执行 `LIFECYCLE_ROLE`、`PAYMENT_OPERATOR_ROLE`、`PROJECT_OFFICER_ROLE`、`FINANCE_ROLE` 授权。
- Besu 当前容器启动参数：`--network=dev`、`--min-gas-price=0`，且 `eth_accounts` 返回空数组。
- ethers v6 钱包能力：[Wallet](https://docs.ethers.org/v6/api/wallet/)。
