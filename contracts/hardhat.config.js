require("@nomicfoundation/hardhat-toolbox");
const { readConfig, networkConfig, deployerPrivateKey } = require("./scripts/config");

const contractsConfig = readConfig().value;
const localhostConfig = networkConfig(contractsConfig);
const configuredDeployerPrivateKey = deployerPrivateKey(contractsConfig);

const localhost = {
  url: localhostConfig.rpcUrl,
  chainId: localhostConfig.chainId,
};

if (configuredDeployerPrivateKey) {
  localhost.accounts = [configuredDeployerPrivateKey];
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
    hardhat: {
      allowUnlimitedContractSize: true,
    },
    localhost,
  },
};
