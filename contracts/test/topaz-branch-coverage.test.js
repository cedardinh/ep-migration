const { expect } = require("chai");
const { ethers } = require("hardhat");

const ApprovalStatus = {
  PENDING: 0n,
  APPROVED: 1n,
  REJECTED: 2n,
  INVALID: 3n,
};

const ProjectStatus = {
  CREATED: 1n,
  UPDATED: 2n,
  PENDING_DELETE: 3n,
  DELETED: 4n,
};

const ClaimStatus = {
  SUBMITTED: 1n,
  RESUBMITTED: 2n,
  PARTIAL_CA_APPROVED: 3n,
  ALL_CA_APPROVED: 4n,
  APPROVED: 5n,
  CA_REJECTED: 6n,
  PO_REJECTED: 7n,
  DISCARDED: 8n,
  DELETED: 9n,
};

const InvoiceStatus = {
  SUBMITTED: 1n,
  PROJECT_OFFICER_APPROVED: 2n,
  PROJECT_OFFICER_REJECTED: 3n,
  FINANCE_DEPARTMENT_APPROVED: 4n,
  FINANCE_DEPARTMENT_REJECTED: 5n,
  DISCARDED: 6n,
  DELETED: 7n,
};

const PaymentOrderStatus = {
  CREATED: 1n,
  PARTIAL_APPROVED: 2n,
  ALL_APPROVED: 3n,
  REJECTED: 4n,
  RESUBMIT: 5n,
};

const PaymentStatus = {
  CREATED: 1n,
  ACCEPTED: 2n,
  REJECTED: 3n,
};

function participant(signer, legalName, externalRef, overrides = {}) {
  return {
    wallet: signer.address,
    legalName,
    addressLine1: `${legalName} address`,
    addressLine2: "",
    bic: `${externalRef}BIC`,
    lei: `${externalRef}LEI`,
    externalRef,
    ...overrides,
  };
}

function approver(signer, roleName, overrides = {}) {
  return {
    wallet: signer.address,
    userHash: ethers.id(`user:${roleName}:${signer.address}`),
    roleName,
    externalRef: `external-${roleName}`,
    ...overrides,
  };
}

function documentInput(id, overrides = {}) {
  return {
    documentId: id,
    documentHash: ethers.keccak256(ethers.toUtf8Bytes(id)),
    ...overrides,
  };
}

function bankAccount(overrides = {}) {
  return {
    swiftAddress: "HSBCHKHHHKH",
    bankAccountHolderName: "Topaz Main Contractor Ltd",
    bankAccountNumberRef: "acct-ref-001",
    bankName: "HSBC Hong Kong",
    registeredAddress: "1 Queen's Road Central",
    currency: "HKD",
    ...overrides,
  };
}

function accountInfo(overrides = {}) {
  return {
    accountName: "Topaz Developer Operating Account",
    accountNumber: "dev-acct-001",
    addressLine1: "8 Finance Street",
    addressLine2: "",
    bic: "HSBCHKHHHKH",
    ultimateName: "Topaz Developer Ltd",
    ...overrides,
  };
}

function paymentOrderInput(overrides = {}) {
  return {
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
    ...overrides,
  };
}

function contactInput(overrides = {}) {
  return {
    party: "Developer",
    contactType: "owner",
    name: "Topaz Developer Ltd",
    contactPerson: "Ada Wong",
    contactEmail: "ada@example.com",
    contactNumber: "+852-5555-0101",
    location: "Hong Kong",
    domain: "topaz.example",
    accountName: "Developer Operating Account",
    ...overrides,
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
    otherDeveloper,
    otherContractor,
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
    otherDeveloper,
    otherContractor,
    payment,
    lifecycle,
    contacts,
    developerParticipant: participant(developer, "Topaz Developer Ltd", "DEV"),
    otherDeveloperParticipant: participant(otherDeveloper, "Other Developer Ltd", "OTHER-DEV"),
    contractorParticipant: participant(contractor, "Topaz Main Contractor Ltd", "CON"),
    otherContractorParticipant: participant(otherContractor, "Other Contractor Ltd", "OTHER-CON"),
    claimApprovers: [
      approver(claimApproverOne, "claim-approver-one"),
      approver(claimApproverTwo, "claim-approver-two"),
    ],
    paymentApprovers: [
      approver(paymentApproverOne, "payment-approver-one"),
      approver(paymentApproverTwo, "payment-approver-two"),
    ],
  };
}

function createProjectInput(fixture, overrides = {}) {
  return {
    externalProjectId: "TOPAZ-001",
    name: "Topaz Commercial Tower",
    developer: fixture.developerParticipant,
    mainContractors: [fixture.contractorParticipant],
    claimApprovers: fixture.claimApprovers,
    paymentApprovers: fixture.paymentApprovers,
    bankAccountRefs: ["dev-bank-ref-001"],
    ...overrides,
  };
}

function updateProjectInput(fixture, projectId, overrides = {}) {
  return {
    projectId,
    externalProjectId: "TOPAZ-001-UPDATED",
    name: "Topaz Commercial Tower Updated",
    mainContractors: [fixture.contractorParticipant],
    claimApprovers: fixture.claimApprovers,
    paymentApprovers: fixture.paymentApprovers,
    bankAccountRefs: ["dev-bank-ref-002"],
    ...overrides,
  };
}

async function createProject(fixture, overrides = {}) {
  const input = createProjectInput(fixture, overrides);
  await fixture.lifecycle.connect(fixture.projectOfficer).createProject(input);
  return input;
}

async function submitClaim(fixture, projectId = 1, overrides = {}) {
  const input = {
    projectId,
    descriptionRef: "ipfs://claim-001",
    documents: [documentInput("claim-doc-001")],
    ...overrides,
  };
  await fixture.lifecycle.connect(fixture.contractor).submitClaim(input);
  return input;
}

async function approveClaim(fixture, claimId = 1) {
  await fixture.lifecycle.connect(fixture.claimApproverOne).claimApproverApprove(claimId);
  await fixture.lifecycle.connect(fixture.claimApproverTwo).claimApproverApprove(claimId);
  await fixture.lifecycle.connect(fixture.projectOfficer).projectOfficerApproveClaim(claimId);
}

async function submitApprovedClaim(fixture) {
  await createProject(fixture);
  await submitClaim(fixture);
  await approveClaim(fixture);
}

async function submitInvoice(fixture, claimId = 1, overrides = {}) {
  const input = {
    claimId,
    amountMinor: 1_250_000,
    bankAccount: bankAccount(),
    documents: [documentInput("invoice-doc-001")],
    ...overrides,
  };
  await fixture.lifecycle.connect(fixture.contractor).submitInvoice(input);
  return input;
}

async function submitApprovedInvoice(fixture) {
  await submitApprovedClaim(fixture);
  await submitInvoice(fixture);
  await fixture.lifecycle.connect(fixture.projectOfficer).projectOfficerApproveInvoice(1);
  await fixture.lifecycle.connect(fixture.finance).financeApproveInvoice(1);
}

async function createPaymentOrder(fixture, overrides = {}) {
  await fixture.lifecycle.connect(fixture.finance).createPaymentOrder(paymentOrderInput(overrides));
}

describe("Topaz branch coverage", function () {
  it("covers contact validation, indexes, updates, duplicates and deactivate branches", async function () {
    const { contacts, outsider } = await deployFixture();
    const input = contactInput();

    await expect(contacts.connect(outsider).upsertContact(input))
      .to.be.revertedWithCustomError(contacts, "UnauthorizedCaller")
      .withArgs(outsider.address);

    for (const [badInput, reason] of [
      [contactInput({ party: "" }), "party is required"],
      [contactInput({ contactType: "" }), "contactType is required"],
      [contactInput({ name: "" }), "name is required"],
      [contactInput({ accountName: "" }), "accountName is required"],
    ]) {
      await expect(contacts.upsertContact(badInput))
        .to.be.revertedWithCustomError(contacts, "InvalidInput")
        .withArgs(reason);
    }

    await contacts.batchUpsertContacts([
      input,
      contactInput({ party: "Developer", contactType: "billing", accountName: "Billing Account" }),
    ]);
    expect(await contacts.getFirstActiveContactIdByParty("developer")).to.equal(1n);
    expect(await contacts.getActiveContactCountByParty("DEVELOPER")).to.equal(2n);
    expect(await contacts.getActiveContactCountByType("OWNER")).to.equal(1n);
    expect(await contacts.getActiveContactIdByPartyAt("Developer", 1)).to.equal(2n);
    expect(await contacts.getActiveContactIdByTypeAt("billing", 0)).to.equal(2n);

    await expect(contacts.upsertContact(contactInput({ name: "Updated Name" })))
      .to.emit(contacts, "ContactUpserted")
      .withArgs(1n, input.party, input.accountName, input.contactType, false, true);

    await expect(contacts.upsertContact(contactInput({ party: "Other", accountName: input.accountName })))
      .to.be.revertedWithCustomError(contacts, "DuplicateAccountName")
      .withArgs(input.accountName);
    await expect(contacts.getContactSummary(999)).to.be.revertedWithCustomError(contacts, "UnknownContact").withArgs(999n);
    await expect(contacts.getActiveContactIdByPartyAt("Developer", 99))
      .to.be.revertedWithCustomError(contacts, "InvalidInput")
      .withArgs("party contact index is out of bounds");
    await expect(contacts.getActiveContactIdByTypeAt("billing", 99))
      .to.be.revertedWithCustomError(contacts, "InvalidInput")
      .withArgs("type contact index is out of bounds");

    await expect(contacts.deactivateContact("Missing", "Missing Account"))
      .to.be.revertedWithCustomError(contacts, "UnknownPartyAccount")
      .withArgs("Missing", "Missing Account");
    await contacts.deactivateContact(input.party, input.accountName);
    expect(await contacts.getContactIdByPartyAccount(input.party, input.accountName)).to.equal(0n);
    expect(await contacts.getContactIdByAccount(input.accountName)).to.equal(0n);
    await expect(contacts.deactivateContact(input.party, input.accountName))
      .to.be.revertedWithCustomError(contacts, "InvalidState")
      .withArgs("contact is already inactive");
  });

  it("covers project validation, updates, deletion and approver removal branches", async function () {
    const fixture = await deployFixture();
    const { lifecycle, projectOfficer, finance, contractor, outsider } = fixture;

    await expect(ethers.deployContract("TopazLifecycle", [fixture.admin.address, ethers.ZeroAddress]))
      .to.be.revertedWithCustomError(lifecycle, "InvalidInput")
      .withArgs("payment contract is required");

    const manyContractors = Array.from({ length: 16 }, (_, index) =>
      participant(fixture.otherContractor, `Contractor ${index}`, `CON-${index}`),
    );
    const manyPaymentApprovers = Array.from({ length: 16 }, (_, index) =>
      approver(fixture.paymentApproverOne, `payment-${index}`, { userHash: ethers.id(`payment-${index}`) }),
    );

    for (const [badInput, reason] of [
      [createProjectInput(fixture, { externalProjectId: "" }), "externalProjectId is required"],
      [createProjectInput(fixture, { name: "" }), "name is required"],
      [createProjectInput(fixture, { mainContractors: [] }), "at least one main contractor is required"],
      [createProjectInput(fixture, { mainContractors: manyContractors }), "main contractors cannot exceed 15"],
      [
        createProjectInput(fixture, {
          mainContractors: [
            fixture.contractorParticipant,
            participant(fixture.otherContractor, "Duplicate Contractor", fixture.contractorParticipant.externalRef),
          ],
        }),
        "duplicate contractor identity",
      ],
      [
        createProjectInput(fixture, { developer: participant(fixture.developer, "", "DEV") }),
        "participant legalName is required",
      ],
      [
        createProjectInput(fixture, {
          mainContractors: [participant(fixture.contractor, "Contractor", "CON", { wallet: ethers.ZeroAddress })],
        }),
        "participant wallet is required",
      ],
      [
        createProjectInput(fixture, {
          claimApprovers: [approver(fixture.claimApproverOne, "bad", { userHash: ethers.ZeroHash })],
        }),
        "approver wallet and userHash are required",
      ],
      [
        createProjectInput(fixture, {
          claimApprovers: [
            fixture.claimApprovers[0],
            approver(fixture.claimApproverTwo, "duplicate", { userHash: fixture.claimApprovers[0].userHash }),
          ],
        }),
        "duplicate approver userHash",
      ],
      [createProjectInput(fixture, { paymentApprovers: [] }), "at least one payment approver is required"],
      [
        createProjectInput(fixture, { paymentApprovers: manyPaymentApprovers }),
        "payment approvers cannot exceed 15",
      ],
    ]) {
      await expect(lifecycle.connect(projectOfficer).createProject(badInput))
        .to.be.revertedWithCustomError(lifecycle, "InvalidInput")
        .withArgs(reason);
    }

    await createProject(fixture);
    await createProject(fixture, { externalProjectId: "TOPAZ-OTHER" });

    await expect(lifecycle.updateProject(updateProjectInput(fixture, 999)))
      .to.be.revertedWithCustomError(lifecycle, "AccessControlUnauthorizedAccount");
    await expect(lifecycle.connect(projectOfficer).updateProject(updateProjectInput(fixture, 999)))
      .to.be.revertedWithCustomError(lifecycle, "UnknownProject")
      .withArgs(999n);
    await expect(lifecycle.connect(projectOfficer).updateProject(updateProjectInput(fixture, 1, { externalProjectId: "TOPAZ-OTHER" })))
      .to.be.revertedWithCustomError(lifecycle, "DuplicateProjectId")
      .withArgs("TOPAZ-OTHER");
    await expect(lifecycle.connect(projectOfficer).updateProject(updateProjectInput(fixture, 1)))
      .to.emit(lifecycle, "ProjectUpdated")
      .withArgs(1n, "TOPAZ-001-UPDATED");
    expect((await lifecycle.getProjectSummary(1))[2]).to.equal(ProjectStatus.UPDATED);

    await submitClaim(fixture, 1);
    await expect(
      lifecycle.connect(projectOfficer).updateProject(
        updateProjectInput(fixture, 1, { mainContractors: [fixture.otherContractorParticipant] }),
      ),
    )
      .to.be.revertedWithCustomError(lifecycle, "InvalidInput")
      .withArgs("cannot remove contractor with existing claims");

    await expect(lifecycle.connect(outsider).removeProjectApprover(1, fixture.claimApprovers[0].userHash))
      .to.be.revertedWithCustomError(lifecycle, "UnauthorizedCaller")
      .withArgs(outsider.address);
    await expect(lifecycle.removeProjectApprover(1, ethers.ZeroHash))
      .to.be.revertedWithCustomError(lifecycle, "InvalidInput")
      .withArgs("approver hash is required");
    await expect(lifecycle.removeProjectApprover(1, ethers.id("missing")))
      .to.be.revertedWithCustomError(lifecycle, "InvalidApprover")
      .withArgs(ethers.id("missing"));
    await expect(lifecycle.removeProjectApprover(1, fixture.claimApprovers[1].userHash))
      .to.emit(lifecycle, "ProjectApproverRemoved")
      .withArgs(1n, fixture.claimApprovers[1].userHash);
    expect((await lifecycle.getClaimApprover(1, 1)).status).to.equal(ApprovalStatus.INVALID);

    await createProject(fixture, {
      externalProjectId: "SINGLE-PAYMENT-APPROVER",
      paymentApprovers: [fixture.paymentApprovers[0]],
    });
    await expect(lifecycle.removeProjectApprover(3, fixture.paymentApprovers[0].userHash))
      .to.be.revertedWithCustomError(lifecycle, "InvalidInput")
      .withArgs("at least one payment approver is required");

    await lifecycle.connect(finance).updateProjectBankAccounts(2, ["bank-a", "bank-b"]);
    await lifecycle.connect(projectOfficer).requestProjectDeletion(2);
    expect((await lifecycle.getProjectSummary(2))[2]).to.equal(ProjectStatus.PENDING_DELETE);
    await expect(lifecycle.connect(outsider).deleteProject(2))
      .to.be.revertedWithCustomError(lifecycle, "InvalidActor")
      .withArgs(outsider.address);
    await lifecycle.connect(contractor).deleteProject(2);
    expect((await lifecycle.getProjectSummary(2))[2]).to.equal(ProjectStatus.DELETED);
    await expect(lifecycle.connect(projectOfficer).requestProjectDeletion(2))
      .to.be.revertedWithCustomError(lifecycle, "InvalidState")
      .withArgs("project is not active");
  });

  it("covers claim validation, rejection, resubmission, discard and delete branches", async function () {
    const fixture = await deployFixture();
    const { lifecycle, contractor, outsider, claimApproverOne, projectOfficer } = fixture;
    await createProject(fixture);

    for (const [badInput, reason] of [
      [{ projectId: 1, descriptionRef: "", documents: [documentInput("claim-a")] }, "claim descriptionRef is required"],
      [{ projectId: 1, descriptionRef: "ipfs://claim", documents: [] }, "claim documents are required"],
      [
        { projectId: 1, descriptionRef: "ipfs://claim", documents: [documentInput("", {})] },
        "documentId is required",
      ],
      [
        { projectId: 1, descriptionRef: "ipfs://claim", documents: [documentInput("claim-a", { documentHash: ethers.ZeroHash })] },
        "documentHash is required",
      ],
      [
        {
          projectId: 1,
          descriptionRef: "ipfs://claim",
          documents: [documentInput("dup"), documentInput("dup")],
        },
        "duplicate documentId",
      ],
    ]) {
      await expect(lifecycle.connect(contractor).submitClaim(badInput))
        .to.be.revertedWithCustomError(lifecycle, "InvalidInput")
        .withArgs(reason);
    }

    await expect(lifecycle.connect(contractor).submitClaim({ projectId: 999, descriptionRef: "x", documents: [documentInput("x")] }))
      .to.be.revertedWithCustomError(lifecycle, "UnknownProject")
      .withArgs(999n);
    await expect(lifecycle.connect(outsider).submitClaim({ projectId: 1, descriptionRef: "x", documents: [documentInput("x")] }))
      .to.be.revertedWithCustomError(lifecycle, "InvalidActor")
      .withArgs(outsider.address);

    await submitClaim(fixture);
    await expect(lifecycle.connect(outsider).updateClaim({ claimId: 1, descriptionRef: "x", documents: [documentInput("x")] }))
      .to.be.revertedWithCustomError(lifecycle, "InvalidActor")
      .withArgs(outsider.address);
    await lifecycle.connect(contractor).updateClaim({ claimId: 1, descriptionRef: "ipfs://updated", documents: [documentInput("claim-updated")] });
    expect((await lifecycle.getClaimDocument(1, 0)).documentId).to.equal("claim-updated");

    await expect(lifecycle.connect(outsider).claimApproverApprove(1))
      .to.be.revertedWithCustomError(lifecycle, "InvalidActor")
      .withArgs(outsider.address);
    await expect(lifecycle.connect(claimApproverOne).claimApproverReject(1, ethers.ZeroHash, "comment"))
      .to.be.revertedWithCustomError(lifecycle, "InvalidInput")
      .withArgs("authorHash is required");
    await expect(lifecycle.connect(claimApproverOne).claimApproverReject(1, ethers.id("author"), ""))
      .to.be.revertedWithCustomError(lifecycle, "InvalidInput")
      .withArgs("commentRef is required");
    await lifecycle.connect(claimApproverOne).claimApproverReject(1, ethers.id("author"), "ipfs://comment");
    expect((await lifecycle.getClaimSummary(1))[2]).to.equal(ClaimStatus.CA_REJECTED);

    await expect(lifecycle.connect(contractor).resubmitClaim({ claimId: 999, descriptionRef: "x", documents: [documentInput("x")] }))
      .to.be.revertedWithCustomError(lifecycle, "UnknownClaim")
      .withArgs(999n);
    await lifecycle.connect(contractor).resubmitClaim({ claimId: 1, descriptionRef: "ipfs://resubmitted", documents: [documentInput("resubmitted")] });
    expect((await lifecycle.getClaimSummary(1))[2]).to.equal(ClaimStatus.RESUBMITTED);

    await expect(lifecycle.connect(projectOfficer).projectOfficerApproveClaim(1))
      .to.be.revertedWithCustomError(lifecycle, "InvalidState")
      .withArgs("claim is not fully CA approved");
    await lifecycle.connect(fixture.claimApproverOne).claimApproverApprove(1);
    expect((await lifecycle.getClaimSummary(1))[2]).to.equal(ClaimStatus.PARTIAL_CA_APPROVED);
    await lifecycle.connect(fixture.claimApproverTwo).claimApproverApprove(1);
    await expect(lifecycle.connect(projectOfficer).projectOfficerRejectClaim(1, ethers.id("po"), ""))
      .to.be.revertedWithCustomError(lifecycle, "InvalidInput")
      .withArgs("commentRef is required");
    await lifecycle.connect(projectOfficer).projectOfficerRejectClaim(1, ethers.id("po"), "ipfs://po-reject");
    expect((await lifecycle.getClaimSummary(1))[2]).to.equal(ClaimStatus.PO_REJECTED);
    await lifecycle.connect(contractor).updateClaim({ claimId: 1, descriptionRef: "ipfs://after-po", documents: [documentInput("after-po")] });
    expect((await lifecycle.getClaimSummary(1))[2]).to.equal(ClaimStatus.SUBMITTED);

    await submitClaim(fixture, 1, { descriptionRef: "ipfs://discard", documents: [documentInput("discard")] });
    await lifecycle.connect(contractor).discardClaim(2);
    expect((await lifecycle.getClaimSummary(2))[2]).to.equal(ClaimStatus.DISCARDED);

    await submitClaim(fixture, 1, { descriptionRef: "ipfs://delete", documents: [documentInput("delete")] });
    await lifecycle.connect(contractor).deleteClaim(3);
    expect((await lifecycle.getClaimSummary(3))[2]).to.equal(ClaimStatus.DELETED);

    await createProject(fixture, { externalProjectId: "NO-CLAIM-APPROVERS", claimApprovers: [] });
    await lifecycle.connect(contractor).submitClaim({ projectId: 2, descriptionRef: "ipfs://auto", documents: [documentInput("auto")] });
    expect((await lifecycle.getClaimSummary(4))[2]).to.equal(ClaimStatus.ALL_CA_APPROVED);
  });

  it("covers invoice and payment order validation, rejection, resubmission and bank reference branches", async function () {
    const fixture = await deployFixture();
    const { lifecycle, contractor, outsider, projectOfficer, finance, paymentApproverOne, paymentApproverTwo } = fixture;
    await submitApprovedClaim(fixture);

    await expect(lifecycle.connect(outsider).submitInvoice({ claimId: 1, amountMinor: 1, bankAccount: bankAccount(), documents: [documentInput("i")] }))
      .to.be.revertedWithCustomError(lifecycle, "InvalidActor")
      .withArgs(outsider.address);
    await expect(lifecycle.connect(contractor).submitInvoice({ claimId: 999, amountMinor: 1, bankAccount: bankAccount(), documents: [documentInput("i")] }))
      .to.be.revertedWithCustomError(lifecycle, "UnknownClaim")
      .withArgs(999n);

    for (const [badBank, reason] of [
      [bankAccount({ swiftAddress: "" }), "swiftAddress is required"],
      [bankAccount({ bankAccountHolderName: "" }), "bankAccountHolderName is required"],
      [bankAccount({ bankAccountNumberRef: "" }), "bankAccountNumberRef is required"],
      [bankAccount({ bankName: "" }), "bankName is required"],
      [bankAccount({ registeredAddress: "" }), "registeredAddress is required"],
      [bankAccount({ currency: "" }), "currency is required"],
    ]) {
      await expect(lifecycle.connect(contractor).submitInvoice({ claimId: 1, amountMinor: 1, bankAccount: badBank, documents: [documentInput("i")] }))
        .to.be.revertedWithCustomError(lifecycle, "InvalidInput")
        .withArgs(reason);
    }

    await submitInvoice(fixture);
    await lifecycle.connect(contractor).updateInvoice({ invoiceId: 1, amountMinor: 2_000_000, bankAccount: bankAccount({ currency: "USD" }), documents: [documentInput("invoice-updated")] });
    expect((await lifecycle.getInvoiceDocument(1, 0))[0]).to.equal("invoice-updated");

    await expect(lifecycle.connect(projectOfficer).projectOfficerRejectInvoice(1, ethers.ZeroHash, "comment"))
      .to.be.revertedWithCustomError(lifecycle, "InvalidInput")
      .withArgs("authorHash is required");
    await lifecycle.connect(projectOfficer).projectOfficerApproveInvoice(1);
    await expect(lifecycle.connect(contractor).updateInvoice({ invoiceId: 1, amountMinor: 3, bankAccount: bankAccount(), documents: [documentInput("late")] }))
      .to.be.revertedWithCustomError(lifecycle, "InvalidState")
      .withArgs("invoice can only be updated before approval");
    await expect(lifecycle.connect(projectOfficer).projectOfficerApproveInvoice(1))
      .to.be.revertedWithCustomError(lifecycle, "InvalidState")
      .withArgs("invoice is not awaiting project officer action");
    await lifecycle.connect(finance).financeRejectInvoice(1, ethers.id("finance"), "ipfs://finance-reject");
    expect((await lifecycle.getInvoiceSummary(1))[1]).to.equal(InvoiceStatus.FINANCE_DEPARTMENT_REJECTED);

    await submitInvoice(fixture, 1, { documents: [documentInput("invoice-2")] });
    await lifecycle.connect(contractor).discardInvoice(2);
    expect((await lifecycle.getInvoiceSummary(2))[1]).to.equal(InvoiceStatus.DISCARDED);

    await submitInvoice(fixture, 1, { documents: [documentInput("invoice-3")] });
    await lifecycle.connect(contractor).deleteInvoice(3);
    expect((await lifecycle.getInvoiceSummary(3))[1]).to.equal(InvoiceStatus.DELETED);

    await submitInvoice(fixture, 1, { documents: [documentInput("invoice-4")] });
    await lifecycle.connect(projectOfficer).projectOfficerApproveInvoice(4);
    await lifecycle.connect(finance).financeApproveInvoice(4);

    for (const [badInput, reason] of [
      [paymentOrderInput({ invoiceId: 999 }), "unknown"],
      [paymentOrderInput({ invoiceId: 1 }), "invoice must be Finance approved before payment"],
      [paymentOrderInput({ invoiceId: 4, customerRefNumber: "" }), "customer reference is required"],
      [paymentOrderInput({ invoiceId: 4, chargeBearer: "" }), "chargeBearer is required"],
      [paymentOrderInput({ invoiceId: 4, purposeCode: "" }), "purposeCode is required"],
      [paymentOrderInput({ invoiceId: 4, paymentType: "" }), "paymentType is required"],
      [paymentOrderInput({ invoiceId: 4, fromAccount: accountInfo({ accountName: "" }) }), "accountName is required"],
      [paymentOrderInput({ invoiceId: 4, fromAccount: accountInfo({ accountNumber: "" }) }), "accountNumber is required"],
    ]) {
      const expectation = expect(lifecycle.connect(finance).createPaymentOrder(badInput));
      if (reason === "unknown") {
        await expectation.to.be.revertedWithCustomError(lifecycle, "UnknownInvoice").withArgs(999n);
      } else if (reason === "invoice must be Finance approved before payment") {
        await expectation.to.be.revertedWithCustomError(lifecycle, "InvalidState").withArgs(reason);
      } else {
        await expectation.to.be.revertedWithCustomError(lifecycle, "InvalidInput").withArgs(reason);
      }
    }

    await createPaymentOrder(fixture, { invoiceId: 4 });
    await expect(lifecycle.connect(paymentApproverTwo).approvePaymentOrder(1))
      .to.be.revertedWithCustomError(lifecycle, "InvalidApproverTurn")
      .withArgs(paymentApproverOne.address, paymentApproverTwo.address);
    await expect(lifecycle.connect(paymentApproverOne).rejectPaymentOrder(1, ethers.id("reject"), ""))
      .to.be.revertedWithCustomError(lifecycle, "InvalidInput")
      .withArgs("commentRef is required");
    await lifecycle.connect(paymentApproverOne).rejectPaymentOrder(1, ethers.id("reject"), "ipfs://reject");
    expect((await lifecycle.getPaymentOrderSummary(1))[1]).to.equal(PaymentOrderStatus.REJECTED);

    await expect(lifecycle.connect(finance).resubmitPaymentOrder(paymentOrderInput({ paymentOrderId: 999 })))
      .to.be.revertedWithCustomError(lifecycle, "UnknownPaymentOrder")
      .withArgs(999n);
    await lifecycle.connect(finance).resubmitPaymentOrder({
      paymentOrderId: 1,
      fromAccount: accountInfo(),
      customerRefNumber: "CUST-REF-002",
      chargeBearer: "SHA",
      remittanceInformation: ["Invoice 004"],
      purposeCode: "GDDS",
      valueDate: 1_735_689_600,
      bankInformation: ["priority"],
      paymentType: "FPS",
      preparerRef: "finance-user-002",
    });
    expect((await lifecycle.getPaymentOrderSummary(1))[1]).to.equal(PaymentOrderStatus.RESUBMIT);
    await lifecycle.connect(paymentApproverOne).approvePaymentOrder(1);
    expect((await lifecycle.getPaymentOrderSummary(1))[1]).to.equal(PaymentOrderStatus.PARTIAL_APPROVED);
    await lifecycle.connect(paymentApproverTwo).approvePaymentOrder(1);
    expect((await lifecycle.getPaymentOrderSummary(1))[1]).to.equal(PaymentOrderStatus.ALL_APPROVED);
    expect(await lifecycle.getPaymentOrderPaymentId(1)).to.equal(1n);
    await expect(lifecycle.connect(paymentApproverTwo).approvePaymentOrder(1))
      .to.be.revertedWithCustomError(lifecycle, "InvalidState")
      .withArgs("payment order is not awaiting approval");
    await expect(lifecycle.connect(finance).recordBankPaymentReference(1, ""))
      .to.be.revertedWithCustomError(lifecycle, "InvalidInput")
      .withArgs("bankPaymentRef is required");
    await lifecycle.connect(finance).recordBankPaymentReference(1, "BANK-REF-001");
    expect((await lifecycle.getPaymentOrderSummary(1))[6]).to.equal("BANK-REF-001");
  });

  it("covers direct payment contract validation, state and receipt branches", async function () {
    const fixture = await deployFixture();
    const { payment, admin, paymentOperator } = fixture;
    await payment.grantRole(await payment.LIFECYCLE_ROLE(), admin.address);

    const request = {
      paymentOrderId: 1,
      invoiceId: 1,
      payer: fixture.developerParticipant,
      payee: fixture.contractorParticipant,
      fromAccount: accountInfo(),
      toAccount: accountInfo({ accountName: "Contractor Account", accountNumber: "con-acct-001" }),
      customerRefNumber: "CUST-REF-001",
      instructedAmountMinor: 1_250_000,
      instructedCurrency: "HKD",
      chargeBearer: "SHA",
      remittanceInformation: ["Invoice 001"],
      purposeCode: "GDDS",
      valueDate: 1_735_689_600,
      bankInformation: ["priority"],
      paymentType: "FPS",
      preparerRef: "finance-user-001",
    };

    for (const [badRequest, reason] of [
      [{ ...request, paymentOrderId: 0 }, "paymentOrderId is required"],
      [{ ...request, invoiceId: 0 }, "invoiceId is required"],
      [{ ...request, payer: { ...request.payer, wallet: ethers.ZeroAddress } }, "payer wallet is required"],
      [{ ...request, payee: { ...request.payee, wallet: ethers.ZeroAddress } }, "payee wallet is required"],
      [{ ...request, instructedAmountMinor: 0 }, "instructedAmountMinor must be positive"],
      [{ ...request, customerRefNumber: "" }, "customerRefNumber is required"],
      [{ ...request, instructedCurrency: "" }, "instructedCurrency is required"],
    ]) {
      await expect(payment.createPayment(badRequest))
        .to.be.revertedWithCustomError(payment, "InvalidInput")
        .withArgs(reason);
    }

    await payment.createPayment(request);
    await expect(payment.createPayment(request))
      .to.be.revertedWithCustomError(payment, "DuplicatePayment")
      .withArgs(1n);
    await expect(payment.getPaymentSummary(999)).to.be.revertedWithCustomError(payment, "UnknownPayment").withArgs(999n);

    for (const [badAcceptance, reason] of [
      [{ paymentId: 0, settlementBankRef: "SETTLED", settlementDate: 1 }, "paymentId is required"],
      [{ paymentId: 1, settlementBankRef: "", settlementDate: 1 }, "settlementBankRef is required"],
      [{ paymentId: 1, settlementBankRef: "SETTLED", settlementDate: 0 }, "settlementDate is required"],
    ]) {
      await expect(payment.connect(paymentOperator).acceptPayment(badAcceptance))
        .to.be.revertedWithCustomError(payment, "InvalidInput")
        .withArgs(reason);
    }
    await payment.connect(paymentOperator).acceptPayment({ paymentId: 1, settlementBankRef: "SETTLED", settlementDate: 1 });
    expect((await payment.getPaymentSummary(1))[2]).to.equal(PaymentStatus.ACCEPTED);
    await expect(payment.connect(paymentOperator).acceptPayment({ paymentId: 1, settlementBankRef: "SETTLED-2", settlementDate: 2 }))
      .to.be.revertedWithCustomError(payment, "InvalidState")
      .withArgs("payment is not awaiting acceptance");

    await payment.createPayment({ ...request, paymentOrderId: 2 });
    for (const [badRejection, reason] of [
      [{ paymentId: 0, rejectCode: "R01", rejectReason: "bad", rejectDate: 1 }, "paymentId is required"],
      [{ paymentId: 2, rejectCode: "", rejectReason: "bad", rejectDate: 1 }, "rejectCode is required"],
      [{ paymentId: 2, rejectCode: "R01", rejectReason: "", rejectDate: 1 }, "rejectReason is required"],
      [{ paymentId: 2, rejectCode: "R01", rejectReason: "bad", rejectDate: 0 }, "rejectDate is required"],
    ]) {
      await expect(payment.connect(paymentOperator).rejectPayment(badRejection))
        .to.be.revertedWithCustomError(payment, "InvalidInput")
        .withArgs(reason);
    }
    await payment.connect(paymentOperator).rejectPayment({ paymentId: 2, rejectCode: "R01", rejectReason: "bad", rejectDate: 2 });
    expect((await payment.getPaymentSummary(2))[2]).to.equal(PaymentStatus.REJECTED);

    const receipt = {
      paymentId: 1,
      transactionRefNum: "TXN-001",
      relatedReference: "CUST-REF-001",
      orderingCustomer: "Topaz Developer Ltd",
      orderingInstitution: "HSBC Hong Kong",
      remittanceInfo: "Invoice 001",
      valueDate: "2025-01-02",
    };
    for (const [badReceipt, reason] of [
      [{ ...receipt, paymentId: 0 }, "paymentId is required"],
      [{ ...receipt, transactionRefNum: "" }, "transactionRefNum is required"],
      [{ ...receipt, relatedReference: "" }, "relatedReference is required"],
      [{ ...receipt, orderingCustomer: "" }, "orderingCustomer is required"],
      [{ ...receipt, orderingInstitution: "" }, "orderingInstitution is required"],
      [{ ...receipt, valueDate: "" }, "valueDate is required"],
    ]) {
      await expect(payment.connect(paymentOperator).createPaymentReceipt(badReceipt))
        .to.be.revertedWithCustomError(payment, "InvalidInput")
        .withArgs(reason);
    }
    await payment.createPayment({ ...request, paymentOrderId: 3 });
    await expect(payment.connect(paymentOperator).createPaymentReceipt({ ...receipt, paymentId: 3 }))
      .to.be.revertedWithCustomError(payment, "InvalidState")
      .withArgs("payment must be accepted or rejected before receipt creation");
    await expect(payment.getPaymentReceiptSummary(999))
      .to.be.revertedWithCustomError(payment, "UnknownPaymentReceipt")
      .withArgs(999n);
    await payment.connect(paymentOperator).createPaymentReceipt(receipt);
    expect(await payment.getPaymentReceiptIdByPaymentId(1)).to.equal(1n);
    await expect(payment.connect(paymentOperator).createPaymentReceipt(receipt))
      .to.be.revertedWithCustomError(payment, "DuplicatePaymentReceipt")
      .withArgs(1n);
  });
});
