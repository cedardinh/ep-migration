const hre = require("hardhat");
const fs = require("fs");
const {
  readConfig,
  networkName,
  deploymentOutputFile,
  rolesOutputFile,
  roleAddress,
  requireAddress,
} = require("./config");

function readJson(file) {
  return JSON.parse(fs.readFileSync(file, "utf8"));
}

function writeJson(file, value) {
  fs.mkdirSync(require("path").dirname(file), { recursive: true });
  fs.writeFileSync(file, `${JSON.stringify(value, null, 2)}\n`);
}

async function ensureSigner(deployer) {
  if (!deployer) {
    throw new Error(
      "No grant signer available. Set deployment.deployerPrivateKey in contracts/config/docker-besu.local.json."
    );
  }
}

async function grantIfMissing(contract, role, grantee, label, summary) {
  const alreadyGranted = await contract.hasRole(role, grantee);
  if (alreadyGranted) {
    console.log(`Role already granted: ${label} -> ${grantee}`);
    summary[label] = { address: grantee, granted: true, changed: false };
    return;
  }

  const tx = await contract.grantRole(role, grantee);
  const receipt = await tx.wait();
  const verified = await contract.hasRole(role, grantee);
  if (!verified) {
    throw new Error(`Role grant verification failed: ${label} -> ${grantee}`);
  }

  console.log(`Granted ${label} to: ${grantee} tx=${receipt.hash}`);
  summary[label] = { address: grantee, granted: true, changed: true, transactionHash: receipt.hash };
}

async function main() {
  const contractsConfig = readConfig().value;
  const [deployer] = await hre.ethers.getSigners();
  await ensureSigner(deployer);

  const network = await hre.ethers.provider.getNetwork();
  const chainId = Number(network.chainId);
  const file = deploymentOutputFile(contractsConfig, hre.network.name, chainId);
  if (!fs.existsSync(file)) {
    throw new Error(`Deployment output not found: ${file}`);
  }

  const deployment = readJson(file);

  const topazPayment = deployment.contracts && deployment.contracts.topazPayment;
  const topazLifecycle = deployment.contracts && deployment.contracts.topazLifecycle;
  requireAddress(topazPayment, "deployment.contracts.topazPayment");
  requireAddress(topazLifecycle, "deployment.contracts.topazLifecycle");

  const projectOfficer = requireAddress(
    roleAddress(contractsConfig, "projectOfficerBackend"),
    "roles.projectOfficerBackend"
  );
  const financeOperator = requireAddress(
    roleAddress(contractsConfig, "financeOperator", projectOfficer),
    "roles.financeOperator"
  );
  const paymentOperator = requireAddress(
    roleAddress(contractsConfig, "paymentOperator", projectOfficer),
    "roles.paymentOperator"
  );

  const payment = await hre.ethers.getContractAt("TopazPayment", topazPayment, deployer);
  const lifecycle = await hre.ethers.getContractAt("TopazLifecycle", topazLifecycle, deployer);

  const summary = {
    network: networkName(hre.network.name, contractsConfig),
    chainId,
    signer: deployer.address,
    topazLifecycle,
    topazPayment,
    roles: {},
  };

  await grantIfMissing(
    payment,
    await payment.LIFECYCLE_ROLE(),
    topazLifecycle,
    "TopazPayment.LIFECYCLE_ROLE",
    summary.roles
  );
  await grantIfMissing(
    lifecycle,
    await lifecycle.PROJECT_OFFICER_ROLE(),
    projectOfficer,
    "TopazLifecycle.PROJECT_OFFICER_ROLE",
    summary.roles
  );
  await grantIfMissing(
    lifecycle,
    await lifecycle.FINANCE_ROLE(),
    financeOperator,
    "TopazLifecycle.FINANCE_ROLE",
    summary.roles
  );
  await grantIfMissing(
    payment,
    await payment.PAYMENT_OPERATOR_ROLE(),
    paymentOperator,
    "TopazPayment.PAYMENT_OPERATOR_ROLE",
    summary.roles
  );

  const outputFile = rolesOutputFile(contractsConfig, hre.network.name, chainId);
  writeJson(outputFile, summary);
  console.log(`Wrote role summary: ${outputFile}`);
}

main().catch((error) => {
  console.error(error);
  process.exitCode = 1;
});
