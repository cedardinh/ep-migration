const { expect } = require("chai");
const { ethers } = require("hardhat");

const ProjectStatus = {
  CREATED: 1n,
};

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

async function deployLifecycle() {
  const [admin, transactionSender] = await ethers.getSigners();

  const payment = await ethers.deployContract("TopazPayment", [admin.address]);
  await payment.waitForDeployment();

  const lifecycle = await ethers.deployContract("TopazLifecycle", [
    admin.address,
    await payment.getAddress(),
  ]);
  await lifecycle.waitForDeployment();

  return { admin, transactionSender, lifecycle };
}

describe("createProject screenshot input reproduction", function () {
  it("rejects the screenshot-shaped call until the transaction sender has PROJECT_OFFICER_ROLE", async function () {
    const { transactionSender, lifecycle } = await deployLifecycle();
    const input = screenshotCreateProjectInput();
    const projectOfficerRole = await lifecycle.PROJECT_OFFICER_ROLE();

    await expect(lifecycle.connect(transactionSender).createProject(input))
      .to.be.revertedWithCustomError(lifecycle, "AccessControlUnauthorizedAccount")
      .withArgs(transactionSender.address, projectOfficerRole);
  });

  it("accepts the exact screenshot-shaped input after role grant, then rejects the duplicate id", async function () {
    const { transactionSender, lifecycle } = await deployLifecycle();
    const input = screenshotCreateProjectInput();

    await lifecycle.grantRole(await lifecycle.PROJECT_OFFICER_ROLE(), transactionSender.address);

    await expect(lifecycle.connect(transactionSender).createProject(input))
      .to.emit(lifecycle, "ProjectCreated")
      .withArgs(1n, input.externalProjectId, input.developer.wallet)
      .and.to.emit(lifecycle, "ProjectStatusChanged")
      .withArgs(1n, ProjectStatus.CREATED);

    const summary = await lifecycle.getProjectSummary(1);
    expect(summary[0]).to.equal(input.externalProjectId);
    expect(summary[1]).to.equal(input.name);
    expect(summary[2]).to.equal(ProjectStatus.CREATED);
    expect(summary[3].wallet).to.equal(input.developer.wallet);
    expect(summary[3].legalName).to.equal(input.developer.legalName);
    expect(summary[4].length).to.equal(input.mainContractors.length);
    expect(summary[5].length).to.equal(input.claimApprovers.length);
    expect(summary[5][0].email).to.equal(input.claimApprovers[0].email);
    expect(summary[5][0].userProfileName).to.equal(input.claimApprovers[0].userProfileName);
    expect(summary[6].length).to.equal(input.paymentApprovers.length);
    expect(summary[6][0].email).to.equal(input.paymentApprovers[0].email);
    expect(summary[6][0].userProfileName).to.equal(input.paymentApprovers[0].userProfileName);
    expect(summary[7]).to.deep.equal(input.bankAccountRefs);
    expect(await lifecycle.getProjectPaymentApproverCount(1)).to.equal(2n);

    await expect(lifecycle.connect(transactionSender).createProject(input))
      .to.be.revertedWithCustomError(lifecycle, "DuplicateProjectId")
      .withArgs(input.externalProjectId);
  });
});
