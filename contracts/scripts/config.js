const fs = require("fs");
const path = require("path");

const defaultConfigFile = path.join(__dirname, "..", "config", "docker-besu.local.json");

function readJson(file) {
  return JSON.parse(fs.readFileSync(file, "utf8"));
}

function readConfig() {
  const file = process.env.CONTRACTS_CONFIG_FILE || defaultConfigFile;
  if (!fs.existsSync(file)) {
    throw new Error(`Contracts config file not found: ${file}`);
  }
  return {
    file,
    value: readJson(file),
  };
}

function networkName(networkNameFromHardhat, config) {
  if (networkNameFromHardhat === "localhost") {
    return (config.network && config.network.name) || "docker-besu";
  }
  return networkNameFromHardhat;
}

function networkConfig(config) {
  const network = config.network || {};
  return {
    rpcUrl: network.rpcUrl || "http://127.0.0.1:8546",
    chainId: Number(network.chainId || 1337),
  };
}

function deployerPrivateKey(config) {
  return config.deployment && config.deployment.deployerPrivateKey
    ? config.deployment.deployerPrivateKey
    : undefined;
}

function deploymentOutputFile(config, networkNameFromHardhat, chainId) {
  if (config.deployment && config.deployment.outputFile) {
    return path.resolve(__dirname, "..", config.deployment.outputFile);
  }
  const name = networkName(networkNameFromHardhat, config);
  return path.join(__dirname, "..", "deployments", `${name}-${chainId}.json`);
}

function rolesOutputFile(config, networkNameFromHardhat, chainId) {
  const name = networkName(networkNameFromHardhat, config);
  return path.join(__dirname, "..", "deployments", `${name}-${chainId}.roles.json`);
}

function roleAddress(config, roleName, fallback) {
  const value = config.roles && config.roles[roleName] ? config.roles[roleName] : fallback;
  return value || undefined;
}

function requireAddress(value, label) {
  if (!value || !/^0x[0-9a-fA-F]{40}$/.test(value)) {
    throw new Error(`${label} must be set to a 20-byte hex address`);
  }
  return value;
}

module.exports = {
  readConfig,
  networkName,
  networkConfig,
  deployerPrivateKey,
  deploymentOutputFile,
  rolesOutputFile,
  roleAddress,
  requireAddress,
};
