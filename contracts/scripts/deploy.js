const hre = require("hardhat");
const fs = require("fs");
const {
  readConfig,
  networkName,
  deploymentOutputFile,
} = require("./config");

function ensureSigner(deployer) {
  if (!deployer) {
    throw new Error(
      "No deployer signer available. Set deployment.deployerPrivateKey in contracts/config/docker-besu.local.json."
    );
  }
}

function writeJson(file, value) {
  fs.mkdirSync(require("path").dirname(file), { recursive: true });
  fs.writeFileSync(file, `${JSON.stringify(value, null, 2)}\n`);
}

async function grantIfMissing(contract, role, grantee, label) {
  if (await contract.hasRole(role, grantee)) {
    console.log(`Role already granted: ${label} -> ${grantee}`);
    return false;
  }

  const tx = await contract.grantRole(role, grantee);
  await tx.wait();

  if (!(await contract.hasRole(role, grantee))) {
    throw new Error(`Role grant verification failed: ${label} -> ${grantee}`);
  }

  console.log(`Granted ${label} to: ${grantee}`);
  return true;
}

async function main() {
  const contractsConfig = readConfig().value;
  const [deployer] = await hre.ethers.getSigners();
  ensureSigner(deployer);
  const admin = deployer.address;
  const network = await hre.ethers.provider.getNetwork();
  const chainId = Number(network.chainId);
  const resolvedNetworkName = networkName(hre.network.name, contractsConfig);

  console.log(`Deploying with account: ${admin}`);
  console.log(`Network: ${resolvedNetworkName} chainId=${chainId}`);

  const payment = await hre.ethers.deployContract("TopazPayment", [admin]);
  await payment.waitForDeployment();
  const paymentAddress = await payment.getAddress();
  console.log(`TopazPayment deployed to: ${paymentAddress}`);

  const lifecycle = await hre.ethers.deployContract("TopazLifecycle", [admin, paymentAddress]);
  await lifecycle.waitForDeployment();
  const lifecycleAddress = await lifecycle.getAddress();
  console.log(`TopazLifecycle deployed to: ${lifecycleAddress}`);

  const contacts = await hre.ethers.deployContract("TopazContacts", [admin]);
  await contacts.waitForDeployment();
  const contactsAddress = await contacts.getAddress();
  console.log(`TopazContacts deployed to: ${contactsAddress}`);

  const lifecycleRole = await payment.LIFECYCLE_ROLE();
  await grantIfMissing(payment, lifecycleRole, lifecycleAddress, "TopazPayment.LIFECYCLE_ROLE");

  const output = {
    network: resolvedNetworkName,
    chainId,
    deployer: admin,
    contracts: {
      topazPayment: paymentAddress,
      topazLifecycle: lifecycleAddress,
      topazContacts: contactsAddress,
    },
  };
  const outputFile = deploymentOutputFile(contractsConfig, hre.network.name, chainId);
  writeJson(outputFile, output);
  console.log(`Wrote deployment output: ${outputFile}`);
}

main().catch((error) => {
  console.error(error);
  process.exitCode = 1;
});
