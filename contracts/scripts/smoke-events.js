const hre = require("hardhat");
const fs = require("fs");
const path = require("path");
const { readConfig, deploymentOutputFile } = require("./config");

const { ethers } = hre;

const HANDLERS = {
  "lifecycle.ProjectCreated": "onLifecycleProjectCreated",
  "lifecycle.ProjectStatusChanged": "onLifecycleProjectStatusChanged",
  "lifecycle.ProjectUpdated": "onLifecycleProjectUpdated",
  "lifecycle.ProjectApproverRemoved": "onLifecycleProjectApproverRemoved",
  "lifecycle.ClaimCreated": "onLifecycleClaimCreated",
  "lifecycle.ClaimDocumentsUpdated": "onLifecycleClaimDocumentsUpdated",
  "lifecycle.ClaimStatusChanged": "onLifecycleClaimStatusChanged",
  "lifecycle.InvoiceCreated": "onLifecycleInvoiceCreated",
  "lifecycle.InvoiceDocumentsUpdated": "onLifecycleInvoiceDocumentsUpdated",
  "lifecycle.InvoiceStatusChanged": "onLifecycleInvoiceStatusChanged",
  "lifecycle.PaymentOrderCreated": "onLifecyclePaymentOrderCreated",
  "lifecycle.PaymentOrderStatusChanged": "onLifecyclePaymentOrderStatusChanged",
  "lifecycle.PaymentCreatedForOrder": "onLifecyclePaymentCreatedForOrder",
  "lifecycle.BankPaymentRequested": "onLifecycleBankPaymentRequested",
  "lifecycle.BankPaymentReferenceRecorded": "onLifecycleBankPaymentReferenceRecorded",
  "lifecycle.RoleAdminChanged": "onLifecycleRoleAdminChanged",
  "lifecycle.RoleGranted": "onLifecycleRoleGranted",
  "lifecycle.RoleRevoked": "onLifecycleRoleRevoked",
  "payment.PaymentCreated": "onPaymentPaymentCreated",
  "payment.PaymentAccepted": "onPaymentPaymentAccepted",
  "payment.PaymentRejected": "onPaymentPaymentRejected",
  "payment.PaymentReceiptCreated": "onPaymentPaymentReceiptCreated",
  "payment.RoleAdminChanged": "onPaymentRoleAdminChanged",
  "payment.RoleGranted": "onPaymentRoleGranted",
  "payment.RoleRevoked": "onPaymentRoleRevoked",
  "contacts.ContactUpserted": "onContactsContactUpserted",
  "contacts.ContactDeactivated": "onContactsContactDeactivated",
  "contacts.RoleAdminChanged": "onContactsRoleAdminChanged",
  "contacts.RoleGranted": "onContactsRoleGranted",
  "contacts.RoleRevoked": "onContactsRoleRevoked",
};

const TARGETS = [
  ["lifecycle", "ProjectCreated"],
  ["lifecycle", "ProjectStatusChanged"],
  ["lifecycle", "ProjectUpdated"],
  ["lifecycle", "ProjectApproverRemoved"],
  ["lifecycle", "ClaimCreated"],
  ["lifecycle", "ClaimDocumentsUpdated"],
  ["lifecycle", "ClaimStatusChanged"],
  ["lifecycle", "InvoiceCreated"],
  ["lifecycle", "InvoiceDocumentsUpdated"],
  ["lifecycle", "InvoiceStatusChanged"],
  ["lifecycle", "PaymentOrderCreated"],
  ["lifecycle", "PaymentOrderStatusChanged"],
  ["lifecycle", "PaymentCreatedForOrder"],
  ["lifecycle", "BankPaymentRequested"],
  ["lifecycle", "BankPaymentReferenceRecorded"],
  ["lifecycle", "RoleAdminChanged"],
  ["lifecycle", "RoleGranted"],
  ["lifecycle", "RoleRevoked"],
  ["payment", "PaymentCreated"],
  ["payment", "PaymentAccepted"],
  ["payment", "PaymentRejected"],
  ["payment", "PaymentReceiptCreated"],
  ["payment", "RoleAdminChanged"],
  ["payment", "RoleGranted"],
  ["payment", "RoleRevoked"],
  ["contacts", "ContactUpserted"],
  ["contacts", "ContactDeactivated"],
  ["contacts", "RoleAdminChanged"],
  ["contacts", "RoleGranted"],
  ["contacts", "RoleRevoked"],
];

const report = new Map(
  TARGETS.map(([contract, event]) => [
    key(contract, event),
    {
      contract,
      event,
      handler: HANDLERS[key(contract, event)],
      emitted: false,
      workflow: false,
      txHash: "",
      error: "",
    },
  ])
);

let contractByName = {};

function key(contract, event) {
  return `${contract}.${event}`;
}

function txOptions() {
  return { gasLimit: 8_000_000 };
}

function participant(address, legalName, externalRef) {
  return {
    wallet: address,
    legalName,
    addressLine1: `${legalName} address line 1`,
    addressLine2: "",
    bic: `${externalRef}BIC`,
    lei: `${externalRef}LEI`,
    externalRef,
  };
}

function approver(address, roleName, seed) {
  return {
    wallet: address,
    userHash: ethers.id(`smoke:${seed}:${roleName}`),
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

function bankAccount(seed) {
  return {
    swiftAddress: "HSBCHKHHHKH",
    bankAccountHolderName: "Topaz Main Contractor Ltd",
    bankAccountNumberRef: `acct-ref-${seed}`,
    bankName: "HSBC Hong Kong",
    registeredAddress: "1 Queen's Road Central",
    currency: "HKD",
  };
}

function accountInfo(seed) {
  return {
    accountName: `Topaz Developer Operating Account ${seed}`,
    accountNumber: `dev-acct-${seed}`,
    addressLine1: "8 Finance Street",
    addressLine2: "",
    bic: "HSBCHKHHHKH",
    ultimateName: "Topaz Developer Ltd",
  };
}

function contactInput(seed) {
  return {
    party: `Developer ${seed}`,
    contactType: "owner",
    name: "Topaz Developer Ltd",
    contactPerson: "Ada Wong",
    contactEmail: `ada-${seed}@example.com`,
    contactNumber: "+852-5555-0101",
    location: "Hong Kong",
    domain: "topaz.example",
    accountName: `Developer Operating Account ${seed}`,
  };
}

function parseWorkflowLines(logFile) {
  if (!logFile || !fs.existsSync(logFile)) return [];
  const lines = fs.readFileSync(logFile, "utf8").split(/\r?\n/);
  const events = [];
  for (const line of lines) {
    const trimmed = line.trim();
    if (!trimmed.startsWith("{") || !trimmed.includes("\"handler\"")) continue;
    try {
      events.push(JSON.parse(trimmed));
    } catch (_) {
      // Ignore regular log lines.
    }
  }
  return events;
}

async function waitForWorkflow(logFile, contract, event, handler, txHash) {
  if (!logFile) return false;
  const deadline = Date.now() + 30000;
  while (Date.now() < deadline) {
    const seen = parseWorkflowLines(logFile).some((entry) => {
      return entry.contract === contract
        && entry.event === event
        && entry.handler === handler
        && String(entry.txHash).toLowerCase() === txHash.toLowerCase();
    });
    if (seen) return true;
    await sleep(500);
  }
  return false;
}

async function waitForListenerStart(logFile) {
  if (!logFile) return;
  const deadline = Date.now() + 60000;
  while (Date.now() < deadline) {
    if (fs.existsSync(logFile) && fs.readFileSync(logFile, "utf8").includes("Topaz contract event listener started")) {
      return;
    }
    await sleep(500);
  }
  throw new Error(`Listener did not start within 60s. log=${logFile}`);
}

function sleep(ms) {
  return new Promise((resolve) => setTimeout(resolve, ms));
}

function emittedInReceipt(receipt, contractName, eventName) {
  const contract = contractByName[contractName];
  if (!contract) throw new Error(`No contract registered for ${contractName}`);
  const expectedAddress = contract.target.toLowerCase();
  return receipt.logs.some((log) => {
    if (String(log.address).toLowerCase() !== expectedAddress) return false;
    try {
      return contract.interface.parseLog(log).name === eventName;
    } catch (_) {
      return false;
    }
  });
}

function getEventArgs(receipt, contractName, eventName) {
  const contract = contractByName[contractName];
  if (!contract) throw new Error(`No contract registered for ${contractName}`);
  const expectedAddress = contract.target.toLowerCase();
  for (const log of receipt.logs) {
    if (String(log.address).toLowerCase() !== expectedAddress) continue;
    try {
      const parsed = contract.interface.parseLog(log);
      if (parsed.name === eventName) return parsed.args;
    } catch (_) {
      // Ignore logs for other contracts.
    }
  }
  throw new Error(`${contractName}.${eventName} not found in tx ${receipt.hash}`);
}

async function markFromReceipt(label, txPromise, expectations) {
  const tx = await txPromise;
  const receipt = await tx.wait();
  if (receipt.status !== 1) {
    throw new Error(`${label} failed tx=${receipt.hash}`);
  }

  for (const [contract, event] of expectations) {
    const row = report.get(key(contract, event));
    if (!row) throw new Error(`Unknown report target ${contract}.${event}`);
    if (emittedInReceipt(receipt, contract, event)) {
      row.emitted = true;
      if (!row.txHash) row.txHash = receipt.hash;
      row.error = "";
    } else if (!row.error) {
      row.error = `${label} tx did not emit ${contract}.${event}`;
    }
  }
  return receipt;
}

async function verifyWorkflow(logFile) {
  for (const row of report.values()) {
    if (!row.emitted || !row.txHash) continue;
    row.workflow = await waitForWorkflow(logFile, row.contract, row.event, row.handler, row.txHash);
    if (!row.workflow) {
      row.error = `workflow log missing for ${row.handler} tx=${row.txHash}`;
    }
  }
}

function writeReport(outputFile) {
  const rows = Array.from(report.values());
  const lines = [
    "| Contract | Event | Handler | Chain emitted | Workflow received | Tx | Notes |",
    "|---|---|---|---|---|---|---|",
  ];
  for (const row of rows) {
    lines.push(`| ${row.contract} | ${row.event} | ${row.handler} | ${row.emitted ? "DONE" : "MISSING"} | ${row.workflow ? "DONE" : "MISSING"} | ${row.txHash || "-"} | ${row.error || ""} |`);
  }
  const body = `${lines.join("\n")}\n`;
  console.log(body);
  if (outputFile) {
    fs.mkdirSync(path.dirname(outputFile), { recursive: true });
    fs.writeFileSync(outputFile, body);
  }
}

async function main() {
  const logFile = process.env.LISTENER_LOG_FILE;
  const reportFile = process.env.SMOKE_REPORT_FILE;
  await waitForListenerStart(logFile);

  const contractsConfig = readConfig().value;
  const network = await ethers.provider.getNetwork();
  const deploymentFile = deploymentOutputFile(contractsConfig, hre.network.name, Number(network.chainId));
  const deployment = JSON.parse(fs.readFileSync(deploymentFile, "utf8"));
  const addresses = deployment.contracts || {};

  const [deployer] = await ethers.getSigners();
  const lifecycle = await ethers.getContractAt("TopazLifecycle", addresses.topazLifecycle, deployer);
  const payment = await ethers.getContractAt("TopazPayment", addresses.topazPayment, deployer);
  const contacts = await ethers.getContractAt("TopazContacts", addresses.topazContacts, deployer);
  contractByName = { lifecycle, payment, contacts };
  const seed = `${Date.now()}`;
  const tempLifecycle = ethers.Wallet.createRandom().address;
  const tempPayment = ethers.Wallet.createRandom().address;
  const tempContacts = ethers.Wallet.createRandom().address;

  await markFromReceipt(
    "lifecycle set role admin",
    lifecycle.setRoleAdmin(await lifecycle.FINANCE_ROLE(), await lifecycle.DEFAULT_ADMIN_ROLE(), txOptions()),
    [["lifecycle", "RoleAdminChanged"]]
  );
  await markFromReceipt(
    "lifecycle grant project officer",
    lifecycle.grantRole(await lifecycle.PROJECT_OFFICER_ROLE(), deployer.address, txOptions()),
    [["lifecycle", "RoleGranted"]]
  );
  await markFromReceipt(
    "lifecycle grant finance",
    lifecycle.grantRole(await lifecycle.FINANCE_ROLE(), deployer.address, txOptions()),
    [["lifecycle", "RoleGranted"]]
  );
  await markFromReceipt(
    "lifecycle grant temp finance",
    lifecycle.grantRole(await lifecycle.FINANCE_ROLE(), tempLifecycle, txOptions()),
    [["lifecycle", "RoleGranted"]]
  );
  await markFromReceipt(
    "lifecycle revoke temp finance",
    lifecycle.revokeRole(await lifecycle.FINANCE_ROLE(), tempLifecycle, txOptions()),
    [["lifecycle", "RoleRevoked"]]
  );
  await markFromReceipt(
    "payment grant operator",
    payment.grantRole(await payment.PAYMENT_OPERATOR_ROLE(), deployer.address, txOptions()),
    [["payment", "RoleGranted"]]
  );
  await markFromReceipt(
    "payment set role admin",
    payment.setRoleAdmin(await payment.PAYMENT_OPERATOR_ROLE(), await payment.DEFAULT_ADMIN_ROLE(), txOptions()),
    [["payment", "RoleAdminChanged"]]
  );
  await markFromReceipt(
    "payment grant temp operator",
    payment.grantRole(await payment.PAYMENT_OPERATOR_ROLE(), tempPayment, txOptions()),
    [["payment", "RoleGranted"]]
  );
  await markFromReceipt(
    "payment revoke temp operator",
    payment.revokeRole(await payment.PAYMENT_OPERATOR_ROLE(), tempPayment, txOptions()),
    [["payment", "RoleRevoked"]]
  );
  await markFromReceipt(
    "contacts grant temp admin",
    contacts.grantRole(await contacts.ADMIN_ROLE(), tempContacts, txOptions()),
    [["contacts", "RoleGranted"]]
  );
  await markFromReceipt(
    "contacts set role admin",
    contacts.setRoleAdmin(await contacts.ADMIN_ROLE(), await contacts.DEFAULT_ADMIN_ROLE(), txOptions()),
    [["contacts", "RoleAdminChanged"]]
  );
  await markFromReceipt(
    "contacts revoke temp admin",
    contacts.revokeRole(await contacts.ADMIN_ROLE(), tempContacts, txOptions()),
    [["contacts", "RoleRevoked"]]
  );

  const contact = contactInput(seed);
  await markFromReceipt(
    "contact upsert",
    contacts.upsertContact(contact, txOptions()),
    [["contacts", "ContactUpserted"]]
  );
  await markFromReceipt(
    "contact deactivate",
    contacts.deactivateContact(contact.party, contact.accountName, txOptions()),
    [["contacts", "ContactDeactivated"]]
  );

  const claimApproverOne = approver(deployer.address, "claim-approver-one", seed);
  const claimApproverTwo = approver(deployer.address, "claim-approver-two", seed);
  const paymentApproverOne = approver(deployer.address, "payment-approver-one", seed);
  const paymentApproverTwo = approver(deployer.address, "payment-approver-two", seed);
  const createProjectInput = {
    externalProjectId: `TOPAZ-SMOKE-${seed}`,
    name: "Topaz Event Smoke Project",
    developer: participant(deployer.address, "Topaz Developer Ltd", "DEV"),
    mainContractors: [participant(deployer.address, "Topaz Main Contractor Ltd", "CON")],
    claimApprovers: [claimApproverOne, claimApproverTwo],
    paymentApprovers: [paymentApproverOne, paymentApproverTwo],
    bankAccountRefs: [`dev-bank-ref-${seed}`],
  };
  const projectReceipt = await markFromReceipt(
    "project create",
    lifecycle.createProject(createProjectInput, txOptions()),
    [["lifecycle", "ProjectCreated"], ["lifecycle", "ProjectStatusChanged"]]
  );
  const projectId = getEventArgs(projectReceipt, "lifecycle", "ProjectCreated").projectId;

  await markFromReceipt(
    "project approver remove",
    lifecycle.removeProjectApprover(projectId, claimApproverOne.userHash, txOptions()),
    [["lifecycle", "ProjectApproverRemoved"], ["lifecycle", "ProjectUpdated"], ["lifecycle", "ProjectStatusChanged"]]
  );

  const claimReceipt = await markFromReceipt(
    "claim submit",
    lifecycle.submitClaim({
      projectId,
      descriptionRef: `ipfs://claim-${seed}`,
      documents: [documentInput(`claim-doc-${seed}`)],
    }, txOptions()),
    [["lifecycle", "ClaimCreated"], ["lifecycle", "ClaimDocumentsUpdated"], ["lifecycle", "ClaimStatusChanged"]]
  );
  const claimId = getEventArgs(claimReceipt, "lifecycle", "ClaimCreated").claimId;

  await markFromReceipt(
    "claim approver approve",
    lifecycle.claimApproverApprove(claimId, txOptions()),
    [["lifecycle", "ClaimStatusChanged"]]
  );
  await markFromReceipt(
    "project officer approve claim",
    lifecycle.projectOfficerApproveClaim(claimId, txOptions()),
    [["lifecycle", "ClaimStatusChanged"]]
  );

  await createInvoicePaymentFlow({
    lifecycle,
    payment,
    claimId,
    invoiceSeed: `${seed}-accepted`,
    paymentAction: "accept",
  });
  await createInvoicePaymentFlow({
    lifecycle,
    payment,
    claimId,
    invoiceSeed: `${seed}-rejected`,
    paymentAction: "reject",
  });

  await verifyWorkflow(logFile);
  writeReport(reportFile);

  const failed = Array.from(report.values()).filter((row) => !row.emitted || !row.workflow);
  if (failed.length > 0) {
    process.exitCode = 1;
  }
}

async function createInvoicePaymentFlow({ lifecycle, payment, claimId, invoiceSeed, paymentAction }) {
  const invoiceReceipt = await markFromReceipt(
    `invoice submit ${invoiceSeed}`,
    lifecycle.submitInvoice({
      claimId,
      amountMinor: paymentAction === "accept" ? 1250000 : 1260000,
      bankAccount: bankAccount(invoiceSeed),
      documents: [documentInput(`invoice-doc-${invoiceSeed}`)],
    }, txOptions()),
    [["lifecycle", "InvoiceCreated"], ["lifecycle", "InvoiceDocumentsUpdated"], ["lifecycle", "InvoiceStatusChanged"]]
  );
  const invoiceId = getEventArgs(invoiceReceipt, "lifecycle", "InvoiceCreated").invoiceId;

  await markFromReceipt(
    `project officer approve invoice ${invoiceSeed}`,
    lifecycle.projectOfficerApproveInvoice(invoiceId, txOptions()),
    [["lifecycle", "InvoiceStatusChanged"]]
  );
  await markFromReceipt(
    `finance approve invoice ${invoiceSeed}`,
    lifecycle.financeApproveInvoice(invoiceId, txOptions()),
    [["lifecycle", "InvoiceStatusChanged"]]
  );
  const paymentOrderReceipt = await markFromReceipt(
    `payment order create ${invoiceSeed}`,
    lifecycle.createPaymentOrder({
      invoiceId,
      fromAccount: accountInfo(invoiceSeed),
      customerRefNumber: `CUST-${invoiceSeed}`,
      chargeBearer: "SHA",
      remittanceInformation: [`Invoice ${invoiceSeed}`],
      purposeCode: "GDDS",
      valueDate: 1735689600,
      bankInformation: ["priority"],
      paymentType: "FPS",
      preparerRef: `finance-${invoiceSeed}`,
    }, txOptions()),
    [["lifecycle", "PaymentOrderCreated"], ["lifecycle", "PaymentOrderStatusChanged"]]
  );
  const paymentOrderId = getEventArgs(paymentOrderReceipt, "lifecycle", "PaymentOrderCreated").paymentOrderId;

  await markFromReceipt(
    `payment order first approve ${invoiceSeed}`,
    lifecycle.approvePaymentOrder(paymentOrderId, txOptions()),
    [["lifecycle", "PaymentOrderStatusChanged"]]
  );
  const finalApprovalReceipt = await markFromReceipt(
    `payment order final approve ${invoiceSeed}`,
    lifecycle.approvePaymentOrder(paymentOrderId, txOptions()),
    [
      ["lifecycle", "PaymentOrderStatusChanged"],
      ["lifecycle", "PaymentCreatedForOrder"],
      ["lifecycle", "BankPaymentRequested"],
      ["payment", "PaymentCreated"],
    ]
  );
  const paymentId = getEventArgs(finalApprovalReceipt, "lifecycle", "PaymentCreatedForOrder").paymentId;

  if (paymentAction === "accept") {
    await markFromReceipt(
      `bank payment reference ${invoiceSeed}`,
      lifecycle.recordBankPaymentReference(paymentOrderId, `BANK-PAY-${invoiceSeed}`, txOptions()),
      [["lifecycle", "BankPaymentReferenceRecorded"]]
    );
    await markFromReceipt(
      `payment accept ${invoiceSeed}`,
      payment.acceptPayment({
        paymentId,
        settlementBankRef: `SETTLED-${invoiceSeed}`,
        settlementDate: 1735776000,
      }, txOptions()),
      [["payment", "PaymentAccepted"]]
    );
    await markFromReceipt(
      `payment receipt ${invoiceSeed}`,
      payment.createPaymentReceipt({
        paymentId,
        transactionRefNum: `TXN-${invoiceSeed}`,
        relatedReference: `CUST-${invoiceSeed}`,
        orderingCustomer: "Topaz Developer Ltd",
        orderingInstitution: "HSBC Hong Kong",
        remittanceInfo: `Invoice ${invoiceSeed}`,
        valueDate: "2025-01-02",
      }, txOptions()),
      [["payment", "PaymentReceiptCreated"]]
    );
  } else {
    await markFromReceipt(
      `payment reject ${invoiceSeed}`,
      payment.rejectPayment({
        paymentId,
        rejectCode: "RJCT",
        rejectReason: `Smoke rejection ${invoiceSeed}`,
        rejectDate: 1735776000,
      }, txOptions()),
      [["payment", "PaymentRejected"]]
    );
  }
}

main().catch((error) => {
  console.error(error);
  writeReport(process.env.SMOKE_REPORT_FILE);
  process.exitCode = 1;
});
