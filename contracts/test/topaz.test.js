const { expect } = require("chai");
const { ethers } = require("hardhat");

const ApprovalStatus = {
  PENDING: 0n,
  APPROVED: 1n,
};

const ProjectStatus = {
  CREATED: 1n,
};

const ClaimStatus = {
  SUBMITTED: 1n,
  PARTIAL_CA_APPROVED: 3n,
  ALL_CA_APPROVED: 4n,
  APPROVED: 5n,
};

const InvoiceStatus = {
  SUBMITTED: 1n,
  PROJECT_OFFICER_APPROVED: 2n,
  FINANCE_DEPARTMENT_APPROVED: 4n,
};

const PaymentOrderStatus = {
  CREATED: 1n,
  PARTIAL_APPROVED: 2n,
  ALL_APPROVED: 3n,
};

const PaymentStatus = {
  CREATED: 1n,
  ACCEPTED: 2n,
};

function participant(signer, legalName, externalRef) {
  return {
    wallet: signer.address,
    legalName,
    addressLine1: `${legalName} address line 1`,
    addressLine2: "",
    bic: `${externalRef}BIC`,
    lei: `${externalRef}LEI`,
    externalRef,
  };
}

function approver(signer, roleName) {
  return {
    wallet: signer.address,
    userHash: ethers.id(`user:${roleName}:${signer.address}`),
    roleName,
    externalRef: `external-${roleName}`,
  };
}

function documentInput(id) {
  return {
    documentId: id,
    documentHash: ethers.keccak256(ethers.toUtf8Bytes(id)),
  };
}

function bankAccount() {
  return {
    swiftAddress: "HSBCHKHHHKH",
    bankAccountHolderName: "Topaz Main Contractor Ltd",
    bankAccountNumberRef: "acct-ref-001",
    bankName: "HSBC Hong Kong",
    registeredAddress: "1 Queen's Road Central",
    currency: "HKD",
  };
}

function accountInfo() {
  return {
    accountName: "Topaz Developer Operating Account",
    accountNumber: "dev-acct-001",
    addressLine1: "8 Finance Street",
    addressLine2: "",
    bic: "HSBCHKHHHKH",
    ultimateName: "Topaz Developer Ltd",
  };
}

async function deployFixture() {
  const [
    admin,
    projectOfficer,
    finance,
    contractor,
    developer,
    claimApproverOne,
    claimApproverTwo,
    paymentApproverOne,
    paymentApproverTwo,
    paymentOperator,
    outsider,
  ] = await ethers.getSigners();

  const payment = await ethers.deployContract("TopazPayment", [admin.address]);
  await payment.waitForDeployment();

  const lifecycle = await ethers.deployContract("TopazLifecycle", [
    admin.address,
    await payment.getAddress(),
  ]);
  await lifecycle.waitForDeployment();

  const contacts = await ethers.deployContract("TopazContacts", [admin.address]);
  await contacts.waitForDeployment();

  await payment.grantRole(await payment.LIFECYCLE_ROLE(), await lifecycle.getAddress());
  await payment.grantRole(await payment.PAYMENT_OPERATOR_ROLE(), paymentOperator.address);
  await lifecycle.grantRole(await lifecycle.PROJECT_OFFICER_ROLE(), projectOfficer.address);
  await lifecycle.grantRole(await lifecycle.FINANCE_ROLE(), finance.address);

  const developerParticipant = participant(developer, "Topaz Developer Ltd", "DEV");
  const contractorParticipant = participant(contractor, "Topaz Main Contractor Ltd", "CON");
  const claimApprovers = [
    approver(claimApproverOne, "claim-approver-one"),
    approver(claimApproverTwo, "claim-approver-two"),
  ];
  const paymentApprovers = [
    approver(paymentApproverOne, "payment-approver-one"),
    approver(paymentApproverTwo, "payment-approver-two"),
  ];

  return {
    admin,
    projectOfficer,
    finance,
    contractor,
    developer,
    claimApproverOne,
    claimApproverTwo,
    paymentApproverOne,
    paymentApproverTwo,
    paymentOperator,
    outsider,
    payment,
    lifecycle,
    contacts,
    developerParticipant,
    contractorParticipant,
    claimApprovers,
    paymentApprovers,
  };
}

function createProjectInput(fixture) {
  return {
    externalProjectId: "TOPAZ-001",
    name: "Topaz Commercial Tower",
    developer: fixture.developerParticipant,
    mainContractors: [fixture.contractorParticipant],
    claimApprovers: fixture.claimApprovers,
    paymentApprovers: fixture.paymentApprovers,
    bankAccountRefs: ["dev-bank-ref-001"],
  };
}

async function createProject(fixture) {
  await fixture.lifecycle.connect(fixture.projectOfficer).createProject(createProjectInput(fixture));
}

describe("Topaz contracts", function () {
  it("deploys contracts and configures roles", async function () {
    const fixture = await deployFixture();

    expect(await fixture.payment.hasRole(await fixture.payment.LIFECYCLE_ROLE(), await fixture.lifecycle.getAddress()))
      .to.equal(true);
    expect(await fixture.lifecycle.hasRole(await fixture.lifecycle.PROJECT_OFFICER_ROLE(), fixture.projectOfficer.address))
      .to.equal(true);
    expect(await fixture.lifecycle.hasRole(await fixture.lifecycle.FINANCE_ROLE(), fixture.finance.address))
      .to.equal(true);
  });

  it("upserts and deactivates contacts with admin-only access", async function () {
    const { contacts, outsider } = await deployFixture();
    const input = {
      party: "Developer",
      contactType: "owner",
      name: "Topaz Developer Ltd",
      contactPerson: "Ada Wong",
      contactEmail: "ada@example.com",
      contactNumber: "+852-5555-0101",
      location: "Hong Kong",
      domain: "topaz.example",
      accountName: "Developer Operating Account",
    };

    await expect(contacts.connect(outsider).upsertContact(input))
      .to.be.revertedWithCustomError(contacts, "UnauthorizedCaller")
      .withArgs(outsider.address);

    await expect(contacts.upsertContact(input))
      .to.emit(contacts, "ContactUpserted")
      .withArgs(1n, input.party, input.accountName, input.contactType, true, true);

    expect(await contacts.getContactIdByPartyAccount("developer", "developer operating account")).to.equal(1n);
    expect(await contacts.getContactIdByAccount("DEVELOPER OPERATING ACCOUNT")).to.equal(1n);
    expect(await contacts.getActiveContactCountByParty("DEVELOPER")).to.equal(1n);

    await expect(contacts.deactivateContact("Developer", "Developer Operating Account"))
      .to.emit(contacts, "ContactDeactivated")
      .withArgs(1n, input.party, input.accountName);
    expect(await contacts.getContactIdByAccount(input.accountName)).to.equal(0n);
  });

  it("creates a project and rejects unauthorized or duplicate project creation", async function () {
    const fixture = await deployFixture();
    const input = createProjectInput(fixture);
    const projectOfficerRole = await fixture.lifecycle.PROJECT_OFFICER_ROLE();

    await expect(fixture.lifecycle.connect(fixture.outsider).createProject(input))
      .to.be.revertedWithCustomError(fixture.lifecycle, "AccessControlUnauthorizedAccount")
      .withArgs(fixture.outsider.address, projectOfficerRole);

    await expect(fixture.lifecycle.connect(fixture.projectOfficer).createProject(input))
      .to.emit(fixture.lifecycle, "ProjectCreated")
      .withArgs(1n, input.externalProjectId, fixture.developer.address)
      .and.to.emit(fixture.lifecycle, "ProjectStatusChanged")
      .withArgs(1n, ProjectStatus.CREATED);

    const summary = await fixture.lifecycle.getProjectSummary(1);
    expect(summary[0]).to.equal(input.externalProjectId);
    expect(summary[1]).to.equal(input.name);
    expect(summary[2]).to.equal(ProjectStatus.CREATED);
    expect(summary[4]).to.equal(0n);

    await expect(fixture.lifecycle.connect(fixture.projectOfficer).createProject(input))
      .to.be.revertedWithCustomError(fixture.lifecycle, "DuplicateProjectId")
      .withArgs(input.externalProjectId);
  });

  it("creates a project from the supplied JSON input", async function () {
    const fixture = await deployFixture();
    const input = {
      externalProjectId: "1",
      name: "1",
      developer: {
        wallet: "0x628d684197485c054cda7d3def46e8be6b3d174c",
        legalName: "DEVELOPERACCOUNTA",
        addressLine1: "",
        addressLine2: "",
        bic: "",
        lei: "",
        externalRef: "",
      },
      mainContractors: [
        {
          wallet: "0x628d684197485c054cda7d3def46e8be6b3d174c",
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
          wallet: "0x628d684197485c054cda7d3def46e8be6b3d174c",
          userHash: "0x61533c4c2e198353cde1c7df7a23852535a93a5d1f2ee39863bb3cf118855a53",
          roleName: "1",
          externalRef: "Approver Entity",
        },
      ],
      paymentApprovers: [
        {
          wallet: "0x628d684197485c054cda7d3def46e8be6b3d174c",
          userHash: "0x06a649d9b77f6b7a90a57443026f693d362b91ab6d64aac3557edef254d5efeb",
          roleName: "1",
          externalRef: "Approver Entity",
        },
      ],
      bankAccountRefs: ["dev-bank"],
    };

    await expect(fixture.lifecycle.connect(fixture.projectOfficer).createProject(input))
      .to.emit(fixture.lifecycle, "ProjectCreated")
      .withArgs(1n, input.externalProjectId, ethers.getAddress(input.developer.wallet))
      .and.to.emit(fixture.lifecycle, "ProjectStatusChanged")
      .withArgs(1n, ProjectStatus.CREATED);

    const summary = await fixture.lifecycle.getProjectSummary(1);
    expect(summary[0]).to.equal(input.externalProjectId);
    expect(summary[1]).to.equal(input.name);
    expect(summary[2]).to.equal(ProjectStatus.CREATED);
    expect(summary[3].wallet).to.equal(ethers.getAddress(input.developer.wallet));
    expect(summary[3].legalName).to.equal(input.developer.legalName);
    expect(await fixture.lifecycle.getProjectPaymentApproverCount(1)).to.equal(1n);
  });

  it("runs the project, claim, invoice, payment-order, and payment flow", async function () {
    const fixture = await deployFixture();
    await createProject(fixture);

    const claimDocuments = [documentInput("claim-doc-001")];
    await expect(
      fixture.lifecycle.connect(fixture.contractor).submitClaim({
        projectId: 1,
        descriptionRef: "ipfs://claim-001",
        documents: claimDocuments,
      }),
    )
      .to.emit(fixture.lifecycle, "ClaimCreated")
      .withArgs(1n, 1n, fixture.contractor.address, ClaimStatus.SUBMITTED);

    expect(await fixture.lifecycle.getClaimApproverCount(1)).to.equal(2n);
    const firstClaimApprover = await fixture.lifecycle.getClaimApprover(1, 0);
    expect(firstClaimApprover.status).to.equal(ApprovalStatus.PENDING);

    await fixture.lifecycle.connect(fixture.claimApproverOne).claimApproverApprove(1);
    let claimSummary = await fixture.lifecycle.getClaimSummary(1);
    expect(claimSummary[2]).to.equal(ClaimStatus.PARTIAL_CA_APPROVED);

    await fixture.lifecycle.connect(fixture.claimApproverTwo).claimApproverApprove(1);
    claimSummary = await fixture.lifecycle.getClaimSummary(1);
    expect(claimSummary[2]).to.equal(ClaimStatus.ALL_CA_APPROVED);

    await fixture.lifecycle.connect(fixture.projectOfficer).projectOfficerApproveClaim(1);
    claimSummary = await fixture.lifecycle.getClaimSummary(1);
    expect(claimSummary[2]).to.equal(ClaimStatus.APPROVED);

    await expect(
      fixture.lifecycle.connect(fixture.contractor).submitInvoice({
        claimId: 1,
        amountMinor: 1_250_000,
        bankAccount: bankAccount(),
        documents: [documentInput("invoice-doc-001")],
      }),
    )
      .to.emit(fixture.lifecycle, "InvoiceCreated")
      .withArgs(1n, 1n, InvoiceStatus.SUBMITTED);

    await fixture.lifecycle.connect(fixture.projectOfficer).projectOfficerApproveInvoice(1);
    let invoiceSummary = await fixture.lifecycle.getInvoiceSummary(1);
    expect(invoiceSummary[1]).to.equal(InvoiceStatus.PROJECT_OFFICER_APPROVED);

    await fixture.lifecycle.connect(fixture.finance).financeApproveInvoice(1);
    invoiceSummary = await fixture.lifecycle.getInvoiceSummary(1);
    expect(invoiceSummary[1]).to.equal(InvoiceStatus.FINANCE_DEPARTMENT_APPROVED);

    await expect(
      fixture.lifecycle.connect(fixture.finance).createPaymentOrder({
        invoiceId: 1,
        fromAccount: accountInfo(),
        customerRefNumber: "CUST-REF-001",
        chargeBearer: "SHA",
        remittanceInformation: ["Invoice 001"],
        purposeCode: "GDDS",
        valueDate: 1_735_689_600,
        bankInformation: ["priority"],
        paymentType: "FPS",
        preparerRef: "finance-user-001",
      }),
    )
      .to.emit(fixture.lifecycle, "PaymentOrderCreated")
      .withArgs(1n, 1n, PaymentOrderStatus.CREATED);

    expect(await fixture.lifecycle.getPaymentOrderApproverCount(1)).to.equal(2n);
    await expect(fixture.lifecycle.connect(fixture.paymentApproverTwo).approvePaymentOrder(1))
      .to.be.revertedWithCustomError(fixture.lifecycle, "InvalidApproverTurn")
      .withArgs(fixture.paymentApproverOne.address, fixture.paymentApproverTwo.address);

    await fixture.lifecycle.connect(fixture.paymentApproverOne).approvePaymentOrder(1);
    let paymentOrderSummary = await fixture.lifecycle.getPaymentOrderSummary(1);
    expect(paymentOrderSummary[1]).to.equal(PaymentOrderStatus.PARTIAL_APPROVED);
    expect(paymentOrderSummary[5]).to.equal(0n);

    await expect(fixture.lifecycle.connect(fixture.paymentApproverTwo).approvePaymentOrder(1))
      .to.emit(fixture.lifecycle, "PaymentCreatedForOrder")
      .withArgs(1n, 1n, 1n);

    paymentOrderSummary = await fixture.lifecycle.getPaymentOrderSummary(1);
    expect(paymentOrderSummary[1]).to.equal(PaymentOrderStatus.ALL_APPROVED);
    expect(paymentOrderSummary[3]).to.equal(1_250_000n);
    expect(paymentOrderSummary[4]).to.equal("HKD");
    expect(paymentOrderSummary[5]).to.equal(1n);
    expect(await fixture.payment.getPaymentIdByPaymentOrderId(1)).to.equal(1n);

    let paymentSummary = await fixture.payment.getPaymentSummary(1);
    expect(paymentSummary[0]).to.equal(1n);
    expect(paymentSummary[1]).to.equal(1n);
    expect(paymentSummary[2]).to.equal(PaymentStatus.CREATED);
    expect(paymentSummary[3]).to.equal("CUST-REF-001");
    expect(paymentSummary[4]).to.equal(1_250_000n);
    expect(paymentSummary[5]).to.equal("HKD");

    await fixture.lifecycle.connect(fixture.finance).recordBankPaymentReference(1, "BANK-PAY-001");
    paymentOrderSummary = await fixture.lifecycle.getPaymentOrderSummary(1);
    expect(paymentOrderSummary[6]).to.equal("BANK-PAY-001");

    await fixture.payment.connect(fixture.paymentOperator).acceptPayment({
      paymentId: 1,
      settlementBankRef: "SETTLED-001",
      settlementDate: 1_735_776_000,
    });
    paymentSummary = await fixture.payment.getPaymentSummary(1);
    expect(paymentSummary[2]).to.equal(PaymentStatus.ACCEPTED);
    expect(paymentSummary[6]).to.equal("SETTLED-001");

    await expect(
      fixture.payment.connect(fixture.paymentOperator).createPaymentReceipt({
        paymentId: 1,
        transactionRefNum: "TXN-001",
        relatedReference: "CUST-REF-001",
        orderingCustomer: "Topaz Developer Ltd",
        orderingInstitution: "HSBC Hong Kong",
        remittanceInfo: "Invoice 001",
        valueDate: "2025-01-02",
      }),
    )
      .to.emit(fixture.payment, "PaymentReceiptCreated")
      .withArgs(1n, 1n, 1n, "TXN-001");
    expect(await fixture.payment.getPaymentReceiptIdByPaymentId(1)).to.equal(1n);
  });
});
