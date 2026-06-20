# 需求拆解报告：Create Project 端到端链路

## 版本边界

实现和后续示例必须停留在当前项目版本能力内：

- 后端：Gradle wrapper 5.6.4，Kotlin 1.3.72，Spring Boot 2.3.12.RELEASE，Spring Framework 5.2.19.RELEASE，Java/JVM target 1.8。
- 后端依赖：`spring-boot-starter-web`、`jackson-module-kotlin`、`kotlin-reflect`、`kotlin-stdlib-jdk8`、`org.web3j:core:4.9.8`、`spring-boot-starter-test`。
- 合约：Solidity 0.8.24，Hardhat 2.28.6，`@nomicfoundation/hardhat-toolbox` 6.1.2，OpenZeppelin Contracts 5.6.1，ethers 6.17.0，viem 2.52.2。
- 运行：后端端口 8081；目标链使用 Docker 中运行的 Besu，宿主机 RPC 为 `http://127.0.0.1:8546`，chainId 为 `1337`。

这意味着后端代码不能使用 Kotlin 1.4+、Spring Boot 2.4+、Java 9+、web3j 5.x/6.x 的语法或 API 假设。合约和脚本不能使用超出现有 Hardhat/ethers 6.17.0 的写法。web3j 升级到 4.9.8 的原因是该版本仍兼容 Java 8，并实测可以生成当前复杂 tuple ABI wrapper。

## 业务目标

前端提交一个 Create Project JSON，请求进入后端，后端调用链上 `TopazLifecycle.createProject`。当前接口只等待节点接受 raw transaction，随后立即返回交易哈希和请求关联信息；不等待 receipt，也不承诺项目已经链上创建完成。

推荐 HTTP 入口：

```text
POST /api/projects
Content-Type: application/json
```

请求体与合约 `TopazTypes.CreateProjectInput` 对齐：

```json
{
  "externalProjectId": "1",
  "name": "1",
  "developer": {
    "wallet": "0x628d684197485c054cda7d3def46e8be6b3d174c",
    "legalName": "DEVELOPERACCOUNTA",
    "addressLine1": "",
    "addressLine2": "",
    "bic": "",
    "lei": "",
    "externalRef": ""
  },
  "mainContractors": [
    {
      "wallet": "0x628d684197485c054cda7d3def46e8be6b3d174c",
      "legalName": "A1CordaAccVick1Con",
      "addressLine1": "",
      "addressLine2": "",
      "bic": "",
      "lei": "",
      "externalRef": "A1CordaAccVick1Con"
    }
  ],
  "claimApprovers": [
    {
      "wallet": "0x628d684197485c054cda7d3def46e8be6b3d174c",
      "userHash": "0x61533c4c2e198353cde1c7df7a23852535a93a5d1f2ee39863bb3cf118855a53",
      "roleName": "1",
      "externalRef": "Approver Entity"
    }
  ],
  "paymentApprovers": [
    {
      "wallet": "0x628d684197485c054cda7d3def46e8be6b3d174c",
      "userHash": "0x06a649d9b77f6b7a90a57443026f693d362b91ab6d64aac3557edef254d5efeb",
      "roleName": "1",
      "externalRef": "Approver Entity"
    }
  ],
  "bankAccountRefs": ["dev-bank"]
}
```

## 输出契约建议

Create Project 是链上状态变更。当前策略是交易提交成功后立即返回 `transactionHash`，不等待 receipt。

```json
{
  "transactionHash": "0x...",
  "externalProjectId": "1",
  "from": "0x...",
  "to": "0x...",
  "nonce": "12"
}
```

字段说明：

- `transactionHash`：交易唯一标识，前端和运维都可用它查交易。
- `externalProjectId`：请求里的业务项目号，方便前端关联请求。
- `from`、`to`：提交交易使用的 signer 和合约地址。
- `nonce`：提交阶段使用的 signer nonce，用于排查 pending、重复 nonce 和 RPC 拒绝问题。

不返回 `projectId`、`status`、`blockNumber`。这些必须等交易出块后才能确定，当前接口不等待 receipt。

## 端到端拆解

1. 前端提交 JSON 到后端。
2. Controller 只负责 HTTP 入参/出参，不直接依赖 web3j。
3. Service 做轻量业务校验、重复请求响应策略、调用合约 gateway。
4. Contract gateway 把 DTO 转成 ABI 参数，构造 `createProject` calldata。
5. Gateway 调用 nonce manager 分配 signer nonce。
6. Gateway 用后端配置的项目 officer 签名账户签名并发送 raw transaction。
7. 后端拿到 `eth_sendRawTransaction` 返回的 transaction hash 后立即返回。
8. 后端不等待 receipt，也不解析 `ProjectCreated` / `ProjectStatusChanged`。

## 必须拆出的实现单元

- `CreateProjectRequest`/`ParticipantRequest`/`ApproverRequest` DTO。
- `CreateProjectResponse` DTO，只包含提交结果字段。
- `EpChainProperties` 或类似配置类，保存 RPC URL、chainId、contract address、签名账户来源。
- `TopazLifecycleGateway`：合约交互边界，封装 ABI 编码和交易提交。
- `ResilientNonceManager`：统一分配 nonce、签名并提交 raw transaction。
- `ChainCallReporter`：统一输出链调用提交/失败诊断日志，不在 gateway/service/controller 里散落日志。
- `ProjectService`：业务流程入口，处理请求和异常转换。
- `ProjectController`：HTTP API。
- `RestExceptionHandler`：把常见链上/输入/RPC 异常映射为 HTTP 状态码。

## 验收标准

- 使用给定 JSON 调用 `POST /api/projects`，链上创建项目成功。
- 返回中包含 `transactionHash`、`externalProjectId`、`from`、`to`、`nonce`。
- 合约 duplicate project、invalid input、权限不足、RPC 不可用、交易提交失败都能得到清晰错误。
- 不引入超出现有技术栈的新依赖，除非后续明确批准。
- 后续实现保持简洁：controller/service/gateway 三层即可，不引入过度抽象。

## 资料来源

- 本地项目：`build.gradle.kts`、`src/main/kotlin/com/demo/server/epmigration/ApiController.kt`、`src/main/resources/application.yaml`。
- 本地合约：`contracts/contracts/TopazLifecycle.sol`、`contracts/contracts/TopazTypes.sol`、`contracts/test/topaz.test.js`。
- Besu API 文档：[Use Besu APIs](https://docs.besu-eth.org/public-networks/how-to/use-besu-api)、[JSON-RPC APIs](https://docs.besu-eth.org/public-networks/how-to/use-besu-api/json-rpc)、[Send transactions](https://docs.besu-eth.org/public-networks/how-to/send-transactions)。
- web3j 文档：[Interacting with smart contracts](https://docs.web3j.io/4.8.7/smart_contracts/interacting_with_smart_contract/)、[Transactions and smart contracts](https://docs.web3j.io/4.8.7/transactions/transactions_and_smart_contracts/)。
