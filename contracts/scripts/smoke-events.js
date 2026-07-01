const hre = require("hardhat");
const fs = require("fs");
const path = require("path");
const { readConfig, deploymentOutputFile } = require("./config");

const { ethers } = hre;

const ProjectStatus = {
  CREATED: 1,
  UPDATED: 2,
};

const ClaimStatus = {
  SUBMITTED: 1,
  ALL_CA_APPROVED: 4,
  APPROVED: 5,
};

const InvoiceStatus = {
  SUBMITTED: 1,
  PROJECT_OFFICER_APPROVED: 2,
  FINANCE_DEPARTMENT_APPROVED: 4,
};

const PaymentOrderStatus = {
  CREATED: 1,
  PARTIAL_APPROVED: 2,
  ALL_APPROVED: 3,
};

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
      paramChecks: [],
      error: "",
    },
  ])
);

let contractByName = {};

function key(contract, event) {
  return `${contract}.${event}`;
}

function present() {
  return { __smokeMatcher: "present" };
}

function isPresentMatcher(value) {
  return Boolean(value && typeof value === "object" && value.__smokeMatcher === "present");
}

function expectEvent(contract, event, expected = {}) {
  return { contract, event, expected };
}

function txOptions() {
  return { gasLimit: 8_000_000 };
}

async function ensureRole(contract, role, grantee) {
  if (await contract.hasRole(role, grantee)) return;
  const tx = await contract.grantRole(role, grantee, txOptions());
  const receipt = await tx.wait();
  if (receipt.status !== 1) {
    throw new Error(`Failed to grant role ${role} to ${grantee}`);
  }
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
    email: `${roleName}@example.com`,
    firstName: roleName,
    lastName: "Approver",
    userProfileName: `${roleName}-profile`,
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

function contactInput(seed, wallet) {
  return {
    wallet,
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
    if (!trimmed.includes("\"handler\"")) continue;
    const start = trimmed.indexOf("{");
    if (start < 0) continue;
    try {
      events.push(JSON.parse(trimmed.slice(start)));
    } catch (_) {
      // Ignore regular log lines.
    }
  }
  return events;
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

function getEventMatches(receipt, contractName, eventName) {
  const contract = contractByName[contractName];
  if (!contract) throw new Error(`No contract registered for ${contractName}`);
  const expectedAddress = contract.target.toLowerCase();
  const matches = [];
  for (const log of receipt.logs) {
    if (String(log.address).toLowerCase() !== expectedAddress) continue;
    try {
      const parsed = contract.interface.parseLog(log);
      if (parsed.name === eventName) {
        matches.push({ parsed, args: plainEventArgs(parsed) });
      }
    } catch (_) {
      // Ignore logs for other contracts.
    }
  }
  return matches;
}

function getEventArgs(receipt, contractName, eventName) {
  const match = getEventMatches(receipt, contractName, eventName)[0];
  if (match) return match.parsed.args;
  throw new Error(`${contractName}.${eventName} not found in tx ${receipt.hash}`);
}

function plainEventArgs(parsed) {
  const args = {};
  parsed.fragment.inputs.forEach((input, index) => {
    args[input.name || String(index)] = parsed.args[index];
  });
  return args;
}

function normalizeComparable(value) {
  if (isPresentMatcher(value)) return "<present>";
  if (typeof value === "bigint") return value.toString();
  if (typeof value === "number" && Number.isInteger(value)) return value.toString();
  if (typeof value === "string") {
    return /^0x[0-9a-fA-F]+$/.test(value) ? value.toLowerCase() : value;
  }
  if (Array.isArray(value)) return value.map(normalizeComparable);
  if (value && typeof value === "object") {
    return Object.keys(value).sort().reduce((acc, field) => {
      acc[field] = normalizeComparable(value[field]);
      return acc;
    }, {});
  }
  return value;
}

function displayValue(value) {
  if (isPresentMatcher(value)) return "<present>";
  return normalizeComparable(value);
}

function stringify(value) {
  return JSON.stringify(value, (_, item) => {
    if (isPresentMatcher(item)) return "<present>";
    if (typeof item === "bigint") return item.toString();
    return item;
  });
}

function compareParams(expected, actual) {
  const details = [];
  for (const [field, expectedValue] of Object.entries(expected || {})) {
    const hasActual = actual && Object.prototype.hasOwnProperty.call(actual, field);
    const actualValue = hasActual ? actual[field] : undefined;
    const matched = isPresentMatcher(expectedValue)
      ? hasActual && actualValue !== undefined && actualValue !== null && actualValue !== ""
      : JSON.stringify(normalizeComparable(expectedValue)) === JSON.stringify(normalizeComparable(actualValue));
    details.push({
      field,
      expected: displayValue(expectedValue),
      actual: displayValue(actualValue),
      matched,
    });
  }
  return {
    matched: details.every((detail) => detail.matched),
    details,
  };
}

function normalizeExpectation(expectation) {
  if (Array.isArray(expectation)) {
    return expectEvent(expectation[0], expectation[1], {});
  }
  return expectation;
}

async function markFromReceipt(label, txPromise, expectations) {
  const tx = await txPromise;
  const receipt = await tx.wait();
  if (receipt.status !== 1) {
    throw new Error(`${label} failed tx=${receipt.hash}`);
  }

  const used = new Map();
  for (const rawExpectation of expectations.map(normalizeExpectation)) {
    const { contract, event, expected } = rawExpectation;
    const row = report.get(key(contract, event));
    if (!row) throw new Error(`Unknown report target ${contract}.${event}`);
    const matches = getEventMatches(receipt, contract, event);
    const matchKey = key(contract, event);
    const matchIndex = used.get(matchKey) || 0;
    const match = matches[matchIndex];
    used.set(matchKey, matchIndex + 1);

    if (match) {
      const comparison = compareParams(expected, match.args);
      const check = {
        label,
        contract,
        event,
        txHash: receipt.hash,
        found: true,
        expected,
        actual: match.args,
        expectedMatched: comparison.matched,
        expectedDetails: comparison.details,
        workflow: false,
        workflowParamMatched: false,
        workflowDetails: [],
        workflowParams: {},
      };
      row.paramChecks.push(check);
      row.emitted = true;
      if (!row.txHash) row.txHash = receipt.hash;
      if (!comparison.matched && !row.error) {
        row.error = `${label} params mismatch for ${contract}.${event}`;
      }
      printParamCheck(check);
    } else {
      const check = {
        label,
        contract,
        event,
        txHash: receipt.hash,
        found: false,
        expected,
        actual: {},
        expectedMatched: false,
        expectedDetails: [],
        workflow: false,
        workflowParamMatched: false,
        workflowDetails: [],
        workflowParams: {},
      };
      row.paramChecks.push(check);
      if (!row.error) row.error = `${label} tx did not emit ${contract}.${event}`;
      printParamCheck(check);
    }
  }
  return receipt;
}

function printParamCheck(check) {
  console.log(
    `[event-param] ${check.label} | ${check.contract}.${check.event} | result=${check.found ? "FOUND" : "MISSING"} | expectedParams=${check.expectedMatched ? "PASS" : "FAIL"} | tx=${check.txHash}`
  );
  console.log(`  expected: ${stringify(check.expected)}`);
  console.log(`  emitted:  ${stringify(check.actual)}`);
  if (check.expectedDetails.length > 0) {
    const failed = check.expectedDetails.filter((detail) => !detail.matched);
    if (failed.length > 0) {
      console.log(`  mismatch: ${stringify(failed)}`);
    }
  }
}

async function verifyWorkflow(logFile) {
  if (!logFile) return;
  const deadline = Date.now() + 30000;
  while (Date.now() < deadline) {
    const entries = parseWorkflowLines(logFile);
    applyWorkflowEntries(entries);
    if (allChecks().filter((check) => check.found).every((check) => check.workflow)) {
      return;
    }
    await sleep(500);
  }
  applyWorkflowEntries(parseWorkflowLines(logFile));
}

function applyWorkflowEntries(entries) {
  for (const row of report.values()) {
    for (const check of row.paramChecks) {
      if (!check.found || check.workflow) continue;
      const entry = entries.find((candidate) => {
        return candidate.contract === check.contract
          && candidate.event === check.event
          && candidate.handler === row.handler
          && String(candidate.txHash).toLowerCase() === check.txHash.toLowerCase();
      });
      if (!entry) continue;

      check.workflow = true;
      check.workflowParams = entry.params || {};
      const workflowComparison = compareParams(check.actual, check.workflowParams);
      check.workflowParamMatched = workflowComparison.matched;
      check.workflowDetails = workflowComparison.details;
      if (!workflowComparison.matched && !row.error) {
        row.error = `workflow params mismatch for ${row.handler} tx=${check.txHash}`;
      }
      console.log(
        `[workflow-param] ${check.label} | ${check.contract}.${check.event} | workflow=FOUND | workflowParams=${check.workflowParamMatched ? "PASS" : "FAIL"} | tx=${check.txHash}`
      );
      console.log(`  workflow: ${stringify(check.workflowParams)}`);
    }
    row.workflow = row.paramChecks.filter((check) => check.found).every((check) => check.workflow);
    if (row.paramChecks.some((check) => check.found && !check.workflow) && !row.error) {
      row.error = `workflow log missing for ${row.handler}`;
    }
  }
}

function allChecks() {
  return Array.from(report.values()).flatMap((row) => row.paramChecks);
}

function rowStatus(row, predicate) {
  if (row.paramChecks.length === 0) return "MISSING";
  return row.paramChecks.every(predicate) ? "PASS" : "FAIL";
}

function rowNotes(row) {
  const failed = row.paramChecks.filter((check) => {
    return !check.found || !check.expectedMatched || !check.workflow || !check.workflowParamMatched;
  });
  if (failed.length === 0) return "";

  const notes = [];
  if (row.error) notes.push(row.error);
  notes.push(`${failed.length} failed check(s)`);
  return notes.join("; ");
}

function writeReport(outputFile) {
  const rows = Array.from(report.values());
  const lines = [
    "| Contract | Event | Handler | Chain emitted | Expected params | Workflow received | Workflow params | Tx | Notes |",
    "|---|---|---|---|---|---|---|---|---|",
  ];
  for (const row of rows) {
    const tx = row.paramChecks.map((check) => check.txHash).filter(Boolean)[0] || "-";
    lines.push(`| ${row.contract} | ${row.event} | ${row.handler} | ${rowStatus(row, (check) => check.found)} | ${rowStatus(row, (check) => check.expectedMatched)} | ${rowStatus(row, (check) => check.workflow)} | ${rowStatus(row, (check) => check.workflowParamMatched)} | ${tx} | ${rowNotes(row)} |`);
  }

  lines.push("");
  lines.push("## Parameter details");
  for (const check of allChecks()) {
    lines.push("");
    lines.push(`- ${check.label} ${check.contract}.${check.event} tx=${check.txHash}`);
    lines.push(`  - emitted=${check.found ? "FOUND" : "MISSING"} expectedParams=${check.expectedMatched ? "PASS" : "FAIL"} workflow=${check.workflow ? "FOUND" : "MISSING"} workflowParams=${check.workflowParamMatched ? "PASS" : "FAIL"}`);
    lines.push(`  - expected: \`${stringify(check.expected)}\``);
    lines.push(`  - emitted: \`${stringify(check.actual)}\``);
    if (check.workflow) {
      lines.push(`  - workflow: \`${stringify(check.workflowParams)}\``);
    }
    const expectedMismatches = check.expectedDetails.filter((detail) => !detail.matched);
    const workflowMismatches = check.workflowDetails.filter((detail) => !detail.matched);
    if (expectedMismatches.length > 0) {
      lines.push(`  - expected mismatches: \`${stringify(expectedMismatches)}\``);
    }
    if (workflowMismatches.length > 0) {
      lines.push(`  - workflow mismatches: \`${stringify(workflowMismatches)}\``);
    }
  }
  const body = `${lines.join("\n")}\n`;
  console.log(body);
  if (outputFile) {
    fs.mkdirSync(path.dirname(outputFile), { recursive: true });
    fs.writeFileSync(outputFile, body);
  }
  writeJsonReport(process.env.SMOKE_JSON_REPORT_FILE);
}

function writeJsonReport(outputFile) {
  if (!outputFile) return;
  const body = JSON.stringify(
    { rows: Array.from(report.values()) },
    (_, item) => {
      if (isPresentMatcher(item)) return "<present>";
      if (typeof item === "bigint") return item.toString();
      return item;
    },
    2
  );
  fs.mkdirSync(path.dirname(outputFile), { recursive: true });
  fs.writeFileSync(outputFile, `${body}\n`);
}

function loadJsonReport(inputFile) {
  const payload = JSON.parse(fs.readFileSync(inputFile, "utf8"));
  for (const row of payload.rows || []) {
    for (const check of row.paramChecks || []) {
      check.workflow = false;
      check.workflowParamMatched = false;
      check.workflowDetails = [];
      check.workflowParams = {};
    }
    row.workflow = false;
    row.error = "";
    report.set(key(row.contract, row.event), row);
  }
}

async function main() {
  const logFile = process.env.LISTENER_LOG_FILE;
  const reportFile = process.env.SMOKE_REPORT_FILE;
  await waitForListenerStart(logFile);

  if (process.env.SMOKE_EXPECTED_JSON_FILE) {
    loadJsonReport(process.env.SMOKE_EXPECTED_JSON_FILE);
    await verifyWorkflow(logFile);
    writeReport(reportFile);
    const failedRows = Array.from(report.values()).filter((row) => {
      return row.paramChecks.length === 0
        || row.paramChecks.some((check) => !check.found || !check.expectedMatched || !check.workflow || !check.workflowParamMatched);
    });
    if (failedRows.length > 0) {
      process.exitCode = 1;
    }
    return;
  }

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
  const tempLifecycleProjectOfficer = ethers.Wallet.createRandom().address;
  const tempLifecycleFinance = ethers.Wallet.createRandom().address;
  const tempLifecycleRevokedFinance = ethers.Wallet.createRandom().address;
  const tempPaymentOperator = ethers.Wallet.createRandom().address;
  const tempPaymentRevokedOperator = ethers.Wallet.createRandom().address;
  const tempContacts = ethers.Wallet.createRandom().address;
  const lifecycleDefaultAdminRole = await lifecycle.DEFAULT_ADMIN_ROLE();
  const lifecycleProjectOfficerRole = await lifecycle.PROJECT_OFFICER_ROLE();
  const lifecycleFinanceRole = await lifecycle.FINANCE_ROLE();
  const paymentDefaultAdminRole = await payment.DEFAULT_ADMIN_ROLE();
  const paymentOperatorRole = await payment.PAYMENT_OPERATOR_ROLE();
  const contactsDefaultAdminRole = await contacts.DEFAULT_ADMIN_ROLE();
  const contactsAdminRole = await contacts.ADMIN_ROLE();

  await ensureRole(lifecycle, lifecycleProjectOfficerRole, deployer.address);
  await ensureRole(lifecycle, lifecycleFinanceRole, deployer.address);
  await ensureRole(payment, paymentOperatorRole, deployer.address);

  await markFromReceipt(
    "lifecycle set role admin",
    lifecycle.setRoleAdmin(lifecycleFinanceRole, lifecycleDefaultAdminRole, txOptions()),
    [
      expectEvent("lifecycle", "RoleAdminChanged", {
        role: lifecycleFinanceRole,
        previousAdminRole: lifecycleDefaultAdminRole,
        newAdminRole: lifecycleDefaultAdminRole,
      }),
    ]
  );
  await markFromReceipt(
    "lifecycle grant project officer",
    lifecycle.grantRole(lifecycleProjectOfficerRole, tempLifecycleProjectOfficer, txOptions()),
    [
      expectEvent("lifecycle", "RoleGranted", {
        role: lifecycleProjectOfficerRole,
        account: tempLifecycleProjectOfficer,
        sender: deployer.address,
      }),
    ]
  );
  await markFromReceipt(
    "lifecycle grant finance",
    lifecycle.grantRole(lifecycleFinanceRole, tempLifecycleFinance, txOptions()),
    [
      expectEvent("lifecycle", "RoleGranted", {
        role: lifecycleFinanceRole,
        account: tempLifecycleFinance,
        sender: deployer.address,
      }),
    ]
  );
  await markFromReceipt(
    "lifecycle grant temp finance",
    lifecycle.grantRole(lifecycleFinanceRole, tempLifecycleRevokedFinance, txOptions()),
    [
      expectEvent("lifecycle", "RoleGranted", {
        role: lifecycleFinanceRole,
        account: tempLifecycleRevokedFinance,
        sender: deployer.address,
      }),
    ]
  );
  await markFromReceipt(
    "lifecycle revoke temp finance",
    lifecycle.revokeRole(lifecycleFinanceRole, tempLifecycleRevokedFinance, txOptions()),
    [
      expectEvent("lifecycle", "RoleRevoked", {
        role: lifecycleFinanceRole,
        account: tempLifecycleRevokedFinance,
        sender: deployer.address,
      }),
    ]
  );
  await markFromReceipt(
    "payment grant operator",
    payment.grantRole(paymentOperatorRole, tempPaymentOperator, txOptions()),
    [
      expectEvent("payment", "RoleGranted", {
        role: paymentOperatorRole,
        account: tempPaymentOperator,
        sender: deployer.address,
      }),
    ]
  );
  await markFromReceipt(
    "payment set role admin",
    payment.setRoleAdmin(paymentOperatorRole, paymentDefaultAdminRole, txOptions()),
    [
      expectEvent("payment", "RoleAdminChanged", {
        role: paymentOperatorRole,
        previousAdminRole: paymentDefaultAdminRole,
        newAdminRole: paymentDefaultAdminRole,
      }),
    ]
  );
  await markFromReceipt(
    "payment grant temp operator",
    payment.grantRole(paymentOperatorRole, tempPaymentRevokedOperator, txOptions()),
    [
      expectEvent("payment", "RoleGranted", {
        role: paymentOperatorRole,
        account: tempPaymentRevokedOperator,
        sender: deployer.address,
      }),
    ]
  );
  await markFromReceipt(
    "payment revoke temp operator",
    payment.revokeRole(paymentOperatorRole, tempPaymentRevokedOperator, txOptions()),
    [
      expectEvent("payment", "RoleRevoked", {
        role: paymentOperatorRole,
        account: tempPaymentRevokedOperator,
        sender: deployer.address,
      }),
    ]
  );
  await markFromReceipt(
    "contacts grant temp admin",
    contacts.grantRole(contactsAdminRole, tempContacts, txOptions()),
    [
      expectEvent("contacts", "RoleGranted", {
        role: contactsAdminRole,
        account: tempContacts,
        sender: deployer.address,
      }),
    ]
  );
  await markFromReceipt(
    "contacts set role admin",
    contacts.setRoleAdmin(contactsAdminRole, contactsDefaultAdminRole, txOptions()),
    [
      expectEvent("contacts", "RoleAdminChanged", {
        role: contactsAdminRole,
        previousAdminRole: contactsDefaultAdminRole,
        newAdminRole: contactsDefaultAdminRole,
      }),
    ]
  );
  await markFromReceipt(
    "contacts revoke temp admin",
    contacts.revokeRole(contactsAdminRole, tempContacts, txOptions()),
    [
      expectEvent("contacts", "RoleRevoked", {
        role: contactsAdminRole,
        account: tempContacts,
        sender: deployer.address,
      }),
    ]
  );

  const contact = contactInput(seed, deployer.address);
  const contactReceipt = await markFromReceipt(
    "contact upsert",
    contacts.upsertContact(contact, txOptions()),
    [
      expectEvent("contacts", "ContactUpserted", {
        contactId: present(),
        wallet: contact.wallet,
        party: contact.party,
        accountName: contact.accountName,
        contactType: contact.contactType,
        created: true,
        active: true,
      }),
    ]
  );
  const contactId = getEventArgs(contactReceipt, "contacts", "ContactUpserted").contactId;
  await markFromReceipt(
    "contact deactivate",
    contacts.deactivateContact(contact.party, contact.accountName, txOptions()),
    [
      expectEvent("contacts", "ContactDeactivated", {
        contactId,
        wallet: contact.wallet,
        party: contact.party,
        accountName: contact.accountName,
      }),
    ]
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
    [
      expectEvent("lifecycle", "ProjectCreated", {
        projectId: present(),
        externalProjectId: createProjectInput.externalProjectId,
        developerWallet: createProjectInput.developer.wallet,
      }),
      expectEvent("lifecycle", "ProjectStatusChanged", {
        projectId: present(),
        status: ProjectStatus.CREATED,
      }),
    ]
  );
  const projectId = getEventArgs(projectReceipt, "lifecycle", "ProjectCreated").projectId;

  await markFromReceipt(
    "project approver remove",
    lifecycle.removeProjectApprover(projectId, claimApproverOne.userHash, txOptions()),
    [
      expectEvent("lifecycle", "ProjectApproverRemoved", {
        projectId,
        userHash: claimApproverOne.userHash,
      }),
      expectEvent("lifecycle", "ProjectUpdated", {
        projectId,
        externalProjectId: createProjectInput.externalProjectId,
      }),
      expectEvent("lifecycle", "ProjectStatusChanged", {
        projectId,
        status: ProjectStatus.UPDATED,
      }),
    ]
  );

  const claimReceipt = await markFromReceipt(
    "claim submit",
    lifecycle.submitClaim({
      projectId,
      descriptionRef: `ipfs://claim-${seed}`,
      documents: [documentInput(`claim-doc-${seed}`)],
    }, txOptions()),
    [
      expectEvent("lifecycle", "ClaimCreated", {
        claimId: present(),
        projectId,
        contractorWallet: deployer.address,
        status: ClaimStatus.SUBMITTED,
      }),
      expectEvent("lifecycle", "ClaimDocumentsUpdated", {
        claimId: present(),
        documentCount: 1,
      }),
      expectEvent("lifecycle", "ClaimStatusChanged", {
        claimId: present(),
        status: ClaimStatus.SUBMITTED,
      }),
    ]
  );
  const claimId = getEventArgs(claimReceipt, "lifecycle", "ClaimCreated").claimId;

  await markFromReceipt(
    "claim approver approve",
    lifecycle.claimApproverApprove(claimId, txOptions()),
    [
      expectEvent("lifecycle", "ClaimStatusChanged", {
        claimId,
        status: ClaimStatus.ALL_CA_APPROVED,
      }),
    ]
  );
  await markFromReceipt(
    "project officer approve claim",
    lifecycle.projectOfficerApproveClaim(claimId, txOptions()),
    [
      expectEvent("lifecycle", "ClaimStatusChanged", {
        claimId,
        status: ClaimStatus.APPROVED,
      }),
    ]
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

  const failedRows = Array.from(report.values()).filter((row) => {
    return row.paramChecks.length === 0
      || row.paramChecks.some((check) => !check.found || !check.expectedMatched || !check.workflow || !check.workflowParamMatched);
  });
  if (failedRows.length > 0) {
    process.exitCode = 1;
  }
}

async function createInvoicePaymentFlow({ lifecycle, payment, claimId, invoiceSeed, paymentAction }) {
  const amountMinor = paymentAction === "accept" ? 1250000 : 1260000;
  const invoiceBankAccount = bankAccount(invoiceSeed);
  const customerRefNumber = `CUST-${invoiceSeed}`;
  const invoiceReceipt = await markFromReceipt(
    `invoice submit ${invoiceSeed}`,
    lifecycle.submitInvoice({
      claimId,
      amountMinor,
      bankAccount: invoiceBankAccount,
      documents: [documentInput(`invoice-doc-${invoiceSeed}`)],
    }, txOptions()),
    [
      expectEvent("lifecycle", "InvoiceCreated", {
        invoiceId: present(),
        claimId,
        status: InvoiceStatus.SUBMITTED,
      }),
      expectEvent("lifecycle", "InvoiceDocumentsUpdated", {
        invoiceId: present(),
        documentCount: 1,
      }),
      expectEvent("lifecycle", "InvoiceStatusChanged", {
        invoiceId: present(),
        status: InvoiceStatus.SUBMITTED,
      }),
    ]
  );
  const invoiceId = getEventArgs(invoiceReceipt, "lifecycle", "InvoiceCreated").invoiceId;

  await markFromReceipt(
    `project officer approve invoice ${invoiceSeed}`,
    lifecycle.projectOfficerApproveInvoice(invoiceId, txOptions()),
    [
      expectEvent("lifecycle", "InvoiceStatusChanged", {
        invoiceId,
        status: InvoiceStatus.PROJECT_OFFICER_APPROVED,
      }),
    ]
  );
  await markFromReceipt(
    `finance approve invoice ${invoiceSeed}`,
    lifecycle.financeApproveInvoice(invoiceId, txOptions()),
    [
      expectEvent("lifecycle", "InvoiceStatusChanged", {
        invoiceId,
        status: InvoiceStatus.FINANCE_DEPARTMENT_APPROVED,
      }),
    ]
  );
  const paymentOrderReceipt = await markFromReceipt(
    `payment order create ${invoiceSeed}`,
    lifecycle.createPaymentOrder({
      invoiceId,
      fromAccount: accountInfo(invoiceSeed),
      customerRefNumber,
      chargeBearer: "SHA",
      remittanceInformation: [`Invoice ${invoiceSeed}`],
      purposeCode: "GDDS",
      valueDate: 1735689600,
      bankInformation: ["priority"],
      paymentType: "FPS",
      preparerRef: `finance-${invoiceSeed}`,
    }, txOptions()),
    [
      expectEvent("lifecycle", "PaymentOrderCreated", {
        paymentOrderId: present(),
        invoiceId,
        status: PaymentOrderStatus.CREATED,
      }),
      expectEvent("lifecycle", "PaymentOrderStatusChanged", {
        paymentOrderId: present(),
        status: PaymentOrderStatus.CREATED,
      }),
    ]
  );
  const paymentOrderId = getEventArgs(paymentOrderReceipt, "lifecycle", "PaymentOrderCreated").paymentOrderId;

  await markFromReceipt(
    `payment order first approve ${invoiceSeed}`,
    lifecycle.approvePaymentOrder(paymentOrderId, txOptions()),
    [
      expectEvent("lifecycle", "PaymentOrderStatusChanged", {
        paymentOrderId,
        status: PaymentOrderStatus.PARTIAL_APPROVED,
      }),
    ]
  );
  const finalApprovalReceipt = await markFromReceipt(
    `payment order final approve ${invoiceSeed}`,
    lifecycle.approvePaymentOrder(paymentOrderId, txOptions()),
    [
      expectEvent("lifecycle", "PaymentOrderStatusChanged", {
        paymentOrderId,
        status: PaymentOrderStatus.ALL_APPROVED,
      }),
      expectEvent("lifecycle", "PaymentCreatedForOrder", {
        paymentOrderId,
        paymentId: present(),
        invoiceId,
      }),
      expectEvent("lifecycle", "BankPaymentRequested", {
        paymentOrderId,
        invoiceId,
        customerRefNumber,
      }),
      expectEvent("payment", "PaymentCreated", {
        paymentId: present(),
        paymentOrderId,
        invoiceId,
        customerRefNumber,
        instructedAmountMinor: amountMinor,
        instructedCurrency: invoiceBankAccount.currency,
      }),
    ]
  );
  const paymentId = getEventArgs(finalApprovalReceipt, "lifecycle", "PaymentCreatedForOrder").paymentId;

  if (paymentAction === "accept") {
    await markFromReceipt(
      `bank payment reference ${invoiceSeed}`,
      lifecycle.recordBankPaymentReference(paymentOrderId, `BANK-PAY-${invoiceSeed}`, txOptions()),
      [
        expectEvent("lifecycle", "BankPaymentReferenceRecorded", {
          paymentOrderId,
          bankPaymentRef: `BANK-PAY-${invoiceSeed}`,
        }),
      ]
    );
    await markFromReceipt(
      `payment accept ${invoiceSeed}`,
      payment.acceptPayment({
        paymentId,
        settlementBankRef: `SETTLED-${invoiceSeed}`,
        settlementDate: 1735776000,
      }, txOptions()),
      [
        expectEvent("payment", "PaymentAccepted", {
          paymentId,
          paymentOrderId,
          settlementBankRef: `SETTLED-${invoiceSeed}`,
        }),
      ]
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
      [
        expectEvent("payment", "PaymentReceiptCreated", {
          paymentReceiptId: present(),
          paymentId,
          paymentOrderId,
          transactionRefNum: `TXN-${invoiceSeed}`,
        }),
      ]
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
      [
        expectEvent("payment", "PaymentRejected", {
          paymentId,
          paymentOrderId,
          rejectCode: "RJCT",
          rejectReason: `Smoke rejection ${invoiceSeed}`,
        }),
      ]
    );
  }
}

main().catch((error) => {
  console.error(error);
  writeReport(process.env.SMOKE_REPORT_FILE);
  process.exitCode = 1;
});
