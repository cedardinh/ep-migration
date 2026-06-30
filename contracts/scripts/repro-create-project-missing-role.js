const hre = require("hardhat");

const { ethers } = hre;

const SCREENSHOT_WALLET = "0x628d684197485c054cda7d3def46e8be6b3d174c";

function screenshotCreateProjectInput() {
  const wallet = ethers.getAddress(SCREENSHOT_WALLET);

  return {
    externalProjectId: "11111",
    name: "project test1",
    developer: {
      wallet,
      legalName: "deva",
      addressLine1: "",
      addressLine2: "",
      bic: "",
      lei: "",
      externalRef: "",
    },
    mainContractors: [
      {
        wallet,
        legalName: "A1CordaAccVick1Con",
        addressLine1: "",
        addressLine2: "",
        bic: "",
        lei: "",
        externalRef: "A1CordaAccVick1Con",
      },
    ],
    claimApprovers: [
      {
        wallet,
        userHash: "0xf09b66dfb6bd1bb5e7d2be0b15a80542e02b79b94ea63cd7e918ac65b1164a9a",
        email: "claim1@example.com",
        firstName: "Claim",
        lastName: "Approver1",
        userProfileName: "claim-approver-1",
        roleName: "Claim Approver1",
        externalRef: "Approver Entity",
      },
      {
        wallet,
        userHash: "0x0e5f9d27cd5b71f3bf06a70750958de5885e55e855457fc31af5be47a26fc121",
        email: "claim2@example.com",
        firstName: "Claim",
        lastName: "Approver2",
        userProfileName: "claim-approver-2",
        roleName: "Claim Approver2",
        externalRef: "Approver Entity",
      },
    ],
    paymentApprovers: [
      {
        wallet,
        userHash: "0x14927bc2b8fc4ebeda7c867c786fff80ee813765a1ae4301f293e9c32f606181",
        email: "payment1@example.com",
        firstName: "Payment",
        lastName: "Approver1",
        userProfileName: "payment-approver-1",
        roleName: "Payment Order Approver1",
        externalRef: "Approver Entity",
      },
      {
        wallet,
        userHash: "0x06a649d9b77f6b7a90a57443026f693d362b91ab6d64aac3557edef254d5efeb",
        email: "payment2@example.com",
        firstName: "Payment",
        lastName: "Approver2",
        userProfileName: "payment-approver-2",
        roleName: "Payment Order Approver2",
        externalRef: "Approver Entity",
      },
    ],
    bankAccountRefs: [],
  };
}

async function main() {
  const [admin] = await ethers.getSigners();
  const transactionSenderAddress = ethers.getAddress(SCREENSHOT_WALLET);

  await hre.network.provider.send("hardhat_impersonateAccount", [transactionSenderAddress]);
  await hre.network.provider.send("hardhat_setBalance", [
    transactionSenderAddress,
    "0x3635c9adc5dea00000",
  ]);
  const transactionSender = await ethers.getSigner(transactionSenderAddress);

  const payment = await ethers.deployContract("TopazPayment", [admin.address]);
  await payment.waitForDeployment();

  const lifecycle = await ethers.deployContract("TopazLifecycle", [
    admin.address,
    await payment.getAddress(),
  ]);
  await lifecycle.waitForDeployment();

  const role = await lifecycle.PROJECT_OFFICER_ROLE();
  const hasRole = await lifecycle.hasRole(role, transactionSender.address);

  console.log("Reproducing createProject without PROJECT_OFFICER_ROLE");
  console.log(`transactionSender=${transactionSender.address}`);
  console.log(`PROJECT_OFFICER_ROLE=${role}`);
  console.log(`hasRole(PROJECT_OFFICER_ROLE, transactionSender)=${hasRole}`);

  try {
    await lifecycle.connect(transactionSender).createProject(screenshotCreateProjectInput());
    console.log("Unexpected success");
  } catch (error) {
    console.log("createProject failed as expected");
    console.log(`error.name=${error.name}`);
    console.log(`error.message=${error.message}`);

    const data = error.data || error.error?.data;
    if (data) {
      console.log(`error.data=${data}`);
      const parsed = lifecycle.interface.parseError(data);
      console.log(`decodedError=${parsed.name}`);
      console.log(`decoded.account=${parsed.args[0]}`);
      console.log(`decoded.neededRole=${parsed.args[1]}`);
    }
  }
}

main().catch((error) => {
  console.error(error);
  process.exitCode = 1;
});
