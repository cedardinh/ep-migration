require("@nomicfoundation/hardhat-toolbox");
const { readConfig, networkConfig, deployerPrivateKey } = require("./scripts/config");

const contractsConfig = readConfig().value;
const localhostConfig = networkConfig(contractsConfig);
const configuredDeployerPrivateKey = deployerPrivateKey(contractsConfig);
const useConfiguredHardhatAccount = process.env.HARDHAT_CONFIGURED_DEPLOYER_ACCOUNT === "true";

const localhost = {
  url: localhostConfig.rpcUrl,
  chainId: localhostConfig.chainId,
};

if (configuredDeployerPrivateKey) {
  localhost.accounts = [configuredDeployerPrivateKey];
}

const hardhat = {
  allowUnlimitedContractSize: true,
  chainId: localhostConfig.chainId,
  initialBaseFeePerGas: 0,
};

if (useConfiguredHardhatAccount && configuredDeployerPrivateKey) {
  hardhat.accounts = [
    {
      privateKey: configuredDeployerPrivateKey,
      balance: "1000000000000000000000000",
    },
  ];
}

/** @type import("hardhat/config").HardhatUserConfig */
module.exports = {
  solidity: {
    version: "0.8.24",
    settings: {
      optimizer: {
        enabled: true,
        runs: 200,
      },
      viaIR: true,
    },
  },
  networks: {
    hardhat,
    localhost,
  },
};
