// SPDX-License-Identifier: MIT
pragma solidity ^0.8.24;

import {TopazAccessControl} from "./TopazAccessControl.sol";
import {ITopazPayment} from "./ITopazPayment.sol";
import {TopazTypes} from "./TopazTypes.sol";

contract TopazLifecycle is TopazAccessControl {
    struct ProjectRecord {
        string externalProjectId;
        string name;
        TopazTypes.ProjectStatus status;
        TopazTypes.Participant developer;
        TopazTypes.Participant[] mainContractors;
        TopazTypes.ApproverConfig[] claimApprovers;
        TopazTypes.ApproverConfig[] paymentApprovers;
        string[] bankAccountRefs;
        uint64 createdAt;
        uint64 updatedAt;
        bool exists;
    }

    struct ClaimRecord {
        uint256 projectId;
        string descriptionRef;
        TopazTypes.ClaimStatus status;
        TopazTypes.Participant contractor;
        TopazTypes.Participant developer;
        TopazTypes.DocumentRecord[] documents;
        TopazTypes.ApprovalSlot[] approvers;
        uint256[] invoiceIds;
        TopazTypes.CommentRecord[] comments;
        uint64 createdAt;
        uint64 updatedAt;
        bool exists;
    }

    struct InvoiceRecord {
        uint256 claimId;
        TopazTypes.InvoiceStatus status;
        TopazTypes.BankAccountDetails bankAccount;
        uint256 amountMinor;
        string currency;
        TopazTypes.DocumentRecord[] documents;
        TopazTypes.Participant mainContractor;
        TopazTypes.Participant developer;
        uint256[] paymentOrderIds;
        TopazTypes.CommentRecord[] comments;
        uint64 createdAt;
        uint64 updatedAt;
        bool exists;
    }

    struct PaymentOrderRecord {
        uint256 invoiceId;
        TopazTypes.PaymentOrderStatus status;
        TopazTypes.Participant payer;
        TopazTypes.Participant payee;
        TopazTypes.AccountInfo fromAccount;
        TopazTypes.AccountInfo toAccount;
        string customerRefNumber;
        uint256 instructedAmountMinor;
        string instructedCurrency;
        string chargeBearer;
        string[] remittanceInformation;
        string purposeCode;
        uint64 valueDate;
        string[] bankInformation;
        string paymentType;
        TopazTypes.ApprovalSlot[] approvers;
        string preparerRef;
        uint256 paymentId;
        string bankPaymentRef;
        TopazTypes.CommentRecord[] comments;
        uint64 createdAt;
        uint64 updatedAt;
        bool exists;
    }

    error DuplicateProjectId(string externalProjectId);
    error UnknownProject(uint256 projectId);
    error UnknownClaim(uint256 claimId);
    error UnknownInvoice(uint256 invoiceId);
    error UnknownPaymentOrder(uint256 paymentOrderId);
    error InvalidState(string reason);
    error InvalidActor(address actor);
    error InvalidInput(string reason);
    error InvalidApprover(bytes32 userHash);
    error InvalidApproverTurn(address expected, address actual);

    uint256 private _nextProjectId = 1;
    uint256 private _nextClaimId = 1;
    uint256 private _nextInvoiceId = 1;
    uint256 private _nextPaymentOrderId = 1;

    mapping(bytes32 => uint256) private _projectIdByExternalIdHash;
    mapping(uint256 => ProjectRecord) private _projects;
    mapping(uint256 => ClaimRecord) private _claims;
    mapping(uint256 => InvoiceRecord) private _invoices;
    mapping(uint256 => PaymentOrderRecord) private _paymentOrders;
    mapping(uint256 => uint256[]) private _projectClaimIds;

    event ProjectCreated(uint256 indexed projectId, string externalProjectId, address indexed developerWallet);
    event ProjectUpdated(uint256 indexed projectId, string externalProjectId);
    event ProjectStatusChanged(uint256 indexed projectId, TopazTypes.ProjectStatus status);
    event ProjectApproverRemoved(uint256 indexed projectId, bytes32 indexed userHash);

    event ClaimCreated(uint256 indexed claimId, uint256 indexed projectId, address indexed contractorWallet, TopazTypes.ClaimStatus status);
    event ClaimStatusChanged(uint256 indexed claimId, TopazTypes.ClaimStatus status);
    event ClaimDocumentsUpdated(uint256 indexed claimId, uint256 documentCount);

    event InvoiceCreated(uint256 indexed invoiceId, uint256 indexed claimId, TopazTypes.InvoiceStatus status);
    event InvoiceStatusChanged(uint256 indexed invoiceId, TopazTypes.InvoiceStatus status);
    event InvoiceDocumentsUpdated(uint256 indexed invoiceId, uint256 documentCount);

    event PaymentOrderCreated(uint256 indexed paymentOrderId, uint256 indexed invoiceId, TopazTypes.PaymentOrderStatus status);
    event PaymentOrderStatusChanged(uint256 indexed paymentOrderId, TopazTypes.PaymentOrderStatus status);
    event PaymentCreatedForOrder(uint256 indexed paymentOrderId, uint256 indexed paymentId, uint256 indexed invoiceId);
    event BankPaymentRequested(uint256 indexed paymentOrderId, uint256 indexed invoiceId, string customerRefNumber);
    event BankPaymentReferenceRecorded(uint256 indexed paymentOrderId, string bankPaymentRef);

    ITopazPayment private immutable _payment;

    constructor(address admin, address paymentContract) TopazAccessControl(admin) {
        if (paymentContract == address(0)) {
            revert InvalidInput("payment contract is required");
        }

        _payment = ITopazPayment(paymentContract);
    }

    function createProject(TopazTypes.CreateProjectInput calldata input)
        external
        onlyRole(PROJECT_OFFICER_ROLE)
        returns (uint256 projectId)
    {
        _validateProjectInput(
            input.externalProjectId,
            input.name,
            input.mainContractors,
            input.claimApprovers,
            input.paymentApprovers
        );
        _validateParticipant(input.developer);

        bytes32 projectKey = _projectExternalIdKey(input.developer, input.externalProjectId);
        if (_projectIdByExternalIdHash[projectKey] != 0) {
            revert DuplicateProjectId(input.externalProjectId);
        }

        projectId = _nextProjectId++;
        ProjectRecord storage project = _projects[projectId];
        project.exists = true;
        project.externalProjectId = input.externalProjectId;
        project.name = input.name;
        project.status = TopazTypes.ProjectStatus.CREATED;
        project.createdAt = _timestamp();
        project.updatedAt = project.createdAt;

        _setParticipantFromCalldata(project.developer, input.developer);
        _replaceProjectParticipants(project.mainContractors, input.mainContractors);
        _replaceProjectApproverConfigs(project.claimApprovers, input.claimApprovers);
        _replaceProjectApproverConfigs(project.paymentApprovers, input.paymentApprovers);
        _replaceStringArray(project.bankAccountRefs, input.bankAccountRefs);

        _projectIdByExternalIdHash[projectKey] = projectId;

        emit ProjectCreated(projectId, input.externalProjectId, input.developer.wallet);
        emit ProjectStatusChanged(projectId, project.status);
    }

    function updateProject(TopazTypes.UpdateProjectInput calldata input)
        external
        onlyRole(PROJECT_OFFICER_ROLE)
    {
        ProjectRecord storage project = _requireProject(input.projectId);

        _validateProjectInput(
            input.externalProjectId,
            input.name,
            input.mainContractors,
            input.claimApprovers,
            input.paymentApprovers
        );
        _validateClaimedContractorsRemainAssigned(input.projectId, input.mainContractors);

        bytes32 oldKey = _projectExternalIdKey(project.developer, project.externalProjectId);
        bytes32 newKey = _projectExternalIdKey(project.developer, input.externalProjectId);
        if (oldKey != newKey) {
            if (_projectIdByExternalIdHash[newKey] != 0) {
                revert DuplicateProjectId(input.externalProjectId);
            }
            delete _projectIdByExternalIdHash[oldKey];
            _projectIdByExternalIdHash[newKey] = input.projectId;
        }

        project.externalProjectId = input.externalProjectId;
        project.name = input.name;
        project.status = TopazTypes.ProjectStatus.UPDATED;
        project.updatedAt = _timestamp();
        _replaceProjectParticipants(project.mainContractors, input.mainContractors);
        _replaceProjectApproverConfigs(project.claimApprovers, input.claimApprovers);
        _replaceProjectApproverConfigs(project.paymentApprovers, input.paymentApprovers);
        _replaceStringArray(project.bankAccountRefs, input.bankAccountRefs);
        _cascadeProjectApproverChanges(input.projectId);

        emit ProjectUpdated(input.projectId, input.externalProjectId);
        emit ProjectStatusChanged(input.projectId, project.status);
    }

    function removeProjectApprover(uint256 projectId, bytes32 userHash)
        external
        onlyAdminOrSuperAdmin
    {
        if (userHash == bytes32(0)) {
            revert InvalidInput("approver hash is required");
        }

        ProjectRecord storage project = _requireProject(projectId);
        bool removedClaimApprover = _removeApproverConfig(project.claimApprovers, userHash);
        bool removedPaymentApprover = _removeApproverConfig(project.paymentApprovers, userHash);

        if (!removedClaimApprover && !removedPaymentApprover) {
            revert InvalidApprover(userHash);
        }
        if (project.paymentApprovers.length == 0) {
            revert InvalidInput("at least one payment approver is required");
        }

        project.status = TopazTypes.ProjectStatus.UPDATED;
        project.updatedAt = _timestamp();
        _cascadeProjectApproverChanges(projectId);

        emit ProjectApproverRemoved(projectId, userHash);
        emit ProjectUpdated(projectId, project.externalProjectId);
        emit ProjectStatusChanged(projectId, project.status);
    }

    function updateProjectBankAccounts(uint256 projectId, string[] calldata bankAccountRefs)
        external
        onlyRole(FINANCE_ROLE)
    {
        ProjectRecord storage project = _requireProject(projectId);
        project.status = TopazTypes.ProjectStatus.UPDATED;
        project.updatedAt = _timestamp();
        _replaceStringArray(project.bankAccountRefs, bankAccountRefs);

        emit ProjectUpdated(projectId, project.externalProjectId);
        emit ProjectStatusChanged(projectId, project.status);
    }

    function requestProjectDeletion(uint256 projectId)
        external
        onlyRole(PROJECT_OFFICER_ROLE)
    {
        ProjectRecord storage project = _requireProject(projectId);
        if (project.status != TopazTypes.ProjectStatus.CREATED && project.status != TopazTypes.ProjectStatus.UPDATED) {
            revert InvalidState("project is not active");
        }

        project.status = TopazTypes.ProjectStatus.PENDING_DELETE;
        project.updatedAt = _timestamp();

        emit ProjectStatusChanged(projectId, project.status);
    }

    function deleteProject(uint256 projectId) external {
        ProjectRecord storage project = _requireProject(projectId);
        if (project.status != TopazTypes.ProjectStatus.PENDING_DELETE) {
            revert InvalidState("project must be pending delete");
        }
        if (!_isProjectContractor(project, msg.sender)) {
            revert InvalidActor(msg.sender);
        }

        project.status = TopazTypes.ProjectStatus.DELETED;
        project.updatedAt = _timestamp();

        emit ProjectStatusChanged(projectId, project.status);
    }

    function submitClaim(TopazTypes.SubmitClaimInput calldata input)
        external
        returns (uint256 claimId)
    {
        ProjectRecord storage project = _requireProject(input.projectId);
        if (project.status != TopazTypes.ProjectStatus.CREATED && project.status != TopazTypes.ProjectStatus.UPDATED) {
            revert InvalidState("project is not open for claims");
        }
        if (bytes(input.descriptionRef).length == 0) {
            revert InvalidInput("claim descriptionRef is required");
        }
        _validateDocuments(input.documents, "claim documents are required");

        uint256 contractorIndex = _findProjectContractor(project, msg.sender);
        if (contractorIndex == type(uint256).max) {
            revert InvalidActor(msg.sender);
        }

        claimId = _nextClaimId++;
        ClaimRecord storage claim = _claims[claimId];
        claim.exists = true;
        claim.projectId = input.projectId;
        claim.descriptionRef = input.descriptionRef;
        claim.status = project.claimApprovers.length == 0
            ? TopazTypes.ClaimStatus.ALL_CA_APPROVED
            : TopazTypes.ClaimStatus.SUBMITTED;
        claim.createdAt = _timestamp();
        claim.updatedAt = claim.createdAt;
        _copyParticipantStorageToStorage(claim.contractor, project.mainContractors[contractorIndex]);
        _copyParticipantStorageToStorage(claim.developer, project.developer);
        _replaceDocumentArray(claim.documents, input.documents);
        _resetClaimApproversFromProject(claim, project);

        _projectClaimIds[input.projectId].push(claimId);

        emit ClaimCreated(claimId, input.projectId, claim.contractor.wallet, claim.status);
        emit ClaimDocumentsUpdated(claimId, claim.documents.length);
        emit ClaimStatusChanged(claimId, claim.status);
    }

    function updateClaim(TopazTypes.UpdateClaimInput calldata input)
        external
    {
        ClaimRecord storage claim = _requireClaim(input.claimId);
        _requireContractor(claim.contractor.wallet);
        if (
            claim.status != TopazTypes.ClaimStatus.SUBMITTED && claim.status != TopazTypes.ClaimStatus.RESUBMITTED
                && claim.status != TopazTypes.ClaimStatus.CA_REJECTED
                && claim.status != TopazTypes.ClaimStatus.PO_REJECTED
        ) {
            revert InvalidState("claim cannot be updated in its current status");
        }
        if (bytes(input.descriptionRef).length == 0) {
            revert InvalidInput("claim descriptionRef is required");
        }
        _validateDocuments(input.documents, "claim documents are required");

        claim.descriptionRef = input.descriptionRef;
        _replaceDocumentArray(claim.documents, input.documents);
        if (
            claim.status == TopazTypes.ClaimStatus.CA_REJECTED
                || claim.status == TopazTypes.ClaimStatus.PO_REJECTED
        ) {
            claim.status = TopazTypes.ClaimStatus.SUBMITTED;
            emit ClaimStatusChanged(input.claimId, claim.status);
        }
        claim.updatedAt = _timestamp();

        emit ClaimDocumentsUpdated(input.claimId, claim.documents.length);
    }

    function resubmitClaim(TopazTypes.UpdateClaimInput calldata input)
        external
    {
        ClaimRecord storage claim = _requireClaim(input.claimId);
        _requireContractor(claim.contractor.wallet);
        if (claim.status != TopazTypes.ClaimStatus.CA_REJECTED && claim.status != TopazTypes.ClaimStatus.PO_REJECTED) {
            revert InvalidState("claim is not rejected");
        }
        if (bytes(input.descriptionRef).length == 0) {
            revert InvalidInput("claim descriptionRef is required");
        }
        _validateDocuments(input.documents, "claim documents are required");

        ProjectRecord storage project = _requireProject(claim.projectId);
        claim.descriptionRef = input.descriptionRef;
        _replaceDocumentArray(claim.documents, input.documents);
        _resetClaimApproversFromProject(claim, project);
        claim.status = project.claimApprovers.length == 0
            ? TopazTypes.ClaimStatus.ALL_CA_APPROVED
            : TopazTypes.ClaimStatus.RESUBMITTED;
        claim.updatedAt = _timestamp();

        emit ClaimDocumentsUpdated(input.claimId, claim.documents.length);
        emit ClaimStatusChanged(input.claimId, claim.status);
    }

    function discardClaim(uint256 claimId)
        external
    {
        ClaimRecord storage claim = _requireClaim(claimId);
        _requireContractor(claim.contractor.wallet);
        if (claim.status != TopazTypes.ClaimStatus.SUBMITTED && claim.status != TopazTypes.ClaimStatus.RESUBMITTED) {
            revert InvalidState("claim can only be discarded before approval");
        }

        claim.status = TopazTypes.ClaimStatus.DISCARDED;
        claim.updatedAt = _timestamp();

        emit ClaimStatusChanged(claimId, claim.status);
    }

    function deleteClaim(uint256 claimId)
        external
    {
        ClaimRecord storage claim = _requireClaim(claimId);
        _requireContractor(claim.contractor.wallet);
        if (claim.status != TopazTypes.ClaimStatus.SUBMITTED && claim.status != TopazTypes.ClaimStatus.RESUBMITTED) {
            revert InvalidState("claim can only be deleted before approval");
        }

        claim.status = TopazTypes.ClaimStatus.DELETED;
        claim.updatedAt = _timestamp();

        emit ClaimStatusChanged(claimId, claim.status);
    }

    function claimApproverApprove(uint256 claimId)
        external
    {
        ClaimRecord storage claim = _requireClaim(claimId);
        if (
            claim.status != TopazTypes.ClaimStatus.SUBMITTED && claim.status != TopazTypes.ClaimStatus.RESUBMITTED
                && claim.status != TopazTypes.ClaimStatus.PARTIAL_CA_APPROVED
        ) {
            revert InvalidState("claim is not awaiting claim approver action");
        }

        uint256 approverIndex = _findPendingClaimApprover(claim, msg.sender);
        if (approverIndex == type(uint256).max) {
            revert InvalidActor(msg.sender);
        }
        TopazTypes.ApprovalSlot storage slot = claim.approvers[approverIndex];
        slot.status = TopazTypes.ApprovalStatus.APPROVED;
        slot.actionTimestamp = _timestamp();
        claim.status = _deriveClaimApprovalStatus(claim.approvers);
        claim.updatedAt = _timestamp();

        emit ClaimStatusChanged(claimId, claim.status);
    }

    function claimApproverReject(uint256 claimId, bytes32 authorHash, string calldata commentRef)
        external
    {
        ClaimRecord storage claim = _requireClaim(claimId);
        if (
            claim.status != TopazTypes.ClaimStatus.SUBMITTED && claim.status != TopazTypes.ClaimStatus.RESUBMITTED
                && claim.status != TopazTypes.ClaimStatus.PARTIAL_CA_APPROVED
        ) {
            revert InvalidState("claim is not awaiting claim approver action");
        }

        uint256 approverIndex = _findPendingClaimApprover(claim, msg.sender);
        if (approverIndex == type(uint256).max) {
            revert InvalidActor(msg.sender);
        }
        TopazTypes.ApprovalSlot storage slot = claim.approvers[approverIndex];
        slot.status = TopazTypes.ApprovalStatus.REJECTED;
        slot.actionTimestamp = _timestamp();
        _addComment(claim.comments, authorHash, commentRef);
        claim.status = TopazTypes.ClaimStatus.CA_REJECTED;
        claim.updatedAt = _timestamp();

        emit ClaimStatusChanged(claimId, claim.status);
    }

    function projectOfficerApproveClaim(uint256 claimId)
        external
        onlyRole(PROJECT_OFFICER_ROLE)
    {
        ClaimRecord storage claim = _requireClaim(claimId);
        if (claim.status != TopazTypes.ClaimStatus.ALL_CA_APPROVED) {
            revert InvalidState("claim is not fully CA approved");
        }

        claim.status = TopazTypes.ClaimStatus.APPROVED;
        claim.updatedAt = _timestamp();

        emit ClaimStatusChanged(claimId, claim.status);
    }

    function projectOfficerRejectClaim(uint256 claimId, bytes32 authorHash, string calldata commentRef)
        external
        onlyRole(PROJECT_OFFICER_ROLE)
    {
        ClaimRecord storage claim = _requireClaim(claimId);
        if (
            claim.status != TopazTypes.ClaimStatus.SUBMITTED && claim.status != TopazTypes.ClaimStatus.RESUBMITTED
                && claim.status != TopazTypes.ClaimStatus.PARTIAL_CA_APPROVED
                && claim.status != TopazTypes.ClaimStatus.ALL_CA_APPROVED
        ) {
            revert InvalidState("claim is not at project officer stage");
        }
        if (bytes(commentRef).length == 0) {
            revert InvalidInput("commentRef is required");
        }

        _addComment(claim.comments, authorHash, commentRef);
        claim.status = TopazTypes.ClaimStatus.PO_REJECTED;
        claim.updatedAt = _timestamp();

        emit ClaimStatusChanged(claimId, claim.status);
    }

    function submitInvoice(TopazTypes.SubmitInvoiceInput calldata input)
        external
        returns (uint256 invoiceId)
    {
        ClaimRecord storage claim = _requireClaim(input.claimId);
        _requireContractor(claim.contractor.wallet);
        if (claim.status != TopazTypes.ClaimStatus.APPROVED) {
            revert InvalidState("claim must be approved before invoicing");
        }
        _validateBankAccount(input.bankAccount);
        _validateDocuments(input.documents, "at least one invoice document is required");

        invoiceId = _nextInvoiceId++;
        InvoiceRecord storage invoice = _invoices[invoiceId];
        invoice.exists = true;
        invoice.claimId = input.claimId;
        invoice.status = TopazTypes.InvoiceStatus.SUBMITTED;
        invoice.amountMinor = input.amountMinor;
        invoice.currency = input.bankAccount.currency;
        invoice.createdAt = _timestamp();
        invoice.updatedAt = invoice.createdAt;
        _setBankAccountFromCalldata(invoice.bankAccount, input.bankAccount);
        _copyParticipantStorageToStorage(invoice.mainContractor, claim.contractor);
        _copyParticipantStorageToStorage(invoice.developer, claim.developer);
        _replaceDocumentArray(invoice.documents, input.documents);
        claim.invoiceIds.push(invoiceId);

        emit InvoiceCreated(invoiceId, input.claimId, invoice.status);
        emit InvoiceDocumentsUpdated(invoiceId, invoice.documents.length);
        emit InvoiceStatusChanged(invoiceId, invoice.status);
    }

    function updateInvoice(TopazTypes.UpdateInvoiceInput calldata input)
        external
    {
        InvoiceRecord storage invoice = _requireInvoice(input.invoiceId);
        ClaimRecord storage claim = _requireClaim(invoice.claimId);
        _requireContractor(claim.contractor.wallet);
        if (invoice.status != TopazTypes.InvoiceStatus.SUBMITTED) {
            revert InvalidState("invoice can only be updated before approval");
        }
        _validateBankAccount(input.bankAccount);
        _validateDocuments(input.documents, "at least one invoice document is required");

        invoice.amountMinor = input.amountMinor;
        invoice.currency = input.bankAccount.currency;
        invoice.updatedAt = _timestamp();
        _setBankAccountFromCalldata(invoice.bankAccount, input.bankAccount);
        _replaceDocumentArray(invoice.documents, input.documents);

        emit InvoiceDocumentsUpdated(input.invoiceId, invoice.documents.length);
    }

    function discardInvoice(uint256 invoiceId)
        external
    {
        InvoiceRecord storage invoice = _requireInvoice(invoiceId);
        ClaimRecord storage claim = _requireClaim(invoice.claimId);
        _requireContractor(claim.contractor.wallet);
        if (invoice.status != TopazTypes.InvoiceStatus.SUBMITTED) {
            revert InvalidState("invoice can only be discarded before approval");
        }

        invoice.status = TopazTypes.InvoiceStatus.DISCARDED;
        invoice.updatedAt = _timestamp();

        emit InvoiceStatusChanged(invoiceId, invoice.status);
    }

    function deleteInvoice(uint256 invoiceId)
        external
    {
        InvoiceRecord storage invoice = _requireInvoice(invoiceId);
        ClaimRecord storage claim = _requireClaim(invoice.claimId);
        _requireContractor(claim.contractor.wallet);
        if (invoice.status != TopazTypes.InvoiceStatus.SUBMITTED) {
            revert InvalidState("invoice can only be deleted before approval");
        }

        invoice.status = TopazTypes.InvoiceStatus.DELETED;
        invoice.updatedAt = _timestamp();

        emit InvoiceStatusChanged(invoiceId, invoice.status);
    }

    function projectOfficerApproveInvoice(uint256 invoiceId)
        external
        onlyRole(PROJECT_OFFICER_ROLE)
    {
        InvoiceRecord storage invoice = _requireInvoice(invoiceId);
        if (invoice.status != TopazTypes.InvoiceStatus.SUBMITTED) {
            revert InvalidState("invoice is not awaiting project officer action");
        }

        invoice.status = TopazTypes.InvoiceStatus.PROJECT_OFFICER_APPROVED;
        invoice.updatedAt = _timestamp();

        emit InvoiceStatusChanged(invoiceId, invoice.status);
    }

    function projectOfficerRejectInvoice(uint256 invoiceId, bytes32 authorHash, string calldata commentRef)
        external
        onlyRole(PROJECT_OFFICER_ROLE)
    {
        InvoiceRecord storage invoice = _requireInvoice(invoiceId);
        if (invoice.status != TopazTypes.InvoiceStatus.SUBMITTED) {
            revert InvalidState("invoice is not awaiting project officer action");
        }

        invoice.status = TopazTypes.InvoiceStatus.PROJECT_OFFICER_REJECTED;
        invoice.updatedAt = _timestamp();
        _addComment(invoice.comments, authorHash, commentRef);

        emit InvoiceStatusChanged(invoiceId, invoice.status);
    }

    function financeApproveInvoice(uint256 invoiceId)
        external
        onlyRole(FINANCE_ROLE)
    {
        InvoiceRecord storage invoice = _requireInvoice(invoiceId);
        if (invoice.status != TopazTypes.InvoiceStatus.PROJECT_OFFICER_APPROVED) {
            revert InvalidState("invoice is not awaiting finance action");
        }

        invoice.status = TopazTypes.InvoiceStatus.FINANCE_DEPARTMENT_APPROVED;
        invoice.updatedAt = _timestamp();

        emit InvoiceStatusChanged(invoiceId, invoice.status);
    }

    function financeRejectInvoice(uint256 invoiceId, bytes32 authorHash, string calldata commentRef)
        external
        onlyRole(FINANCE_ROLE)
    {
        InvoiceRecord storage invoice = _requireInvoice(invoiceId);
        if (invoice.status != TopazTypes.InvoiceStatus.PROJECT_OFFICER_APPROVED) {
            revert InvalidState("invoice is not awaiting finance action");
        }

        invoice.status = TopazTypes.InvoiceStatus.FINANCE_DEPARTMENT_REJECTED;
        invoice.updatedAt = _timestamp();
        _addComment(invoice.comments, authorHash, commentRef);

        emit InvoiceStatusChanged(invoiceId, invoice.status);
    }

    function createPaymentOrder(TopazTypes.CreatePaymentOrderInput calldata input)
        external
        onlyRole(FINANCE_ROLE)
        returns (uint256 paymentOrderId)
    {
        InvoiceRecord storage invoice = _requireInvoice(input.invoiceId);
        if (invoice.status != TopazTypes.InvoiceStatus.FINANCE_DEPARTMENT_APPROVED) {
            revert InvalidState("invoice must be Finance approved before payment");
        }
        if (bytes(input.customerRefNumber).length == 0) {
            revert InvalidInput("customer reference is required");
        }
        if (bytes(input.chargeBearer).length == 0) {
            revert InvalidInput("chargeBearer is required");
        }
        if (bytes(input.purposeCode).length == 0) {
            revert InvalidInput("purposeCode is required");
        }
        if (bytes(input.paymentType).length == 0) {
            revert InvalidInput("paymentType is required");
        }
        _validateAccountInfo(input.fromAccount);

        ClaimRecord storage claim = _requireClaim(invoice.claimId);
        ProjectRecord storage project = _requireProject(claim.projectId);

        paymentOrderId = _nextPaymentOrderId++;
        PaymentOrderRecord storage paymentOrder = _paymentOrders[paymentOrderId];
        paymentOrder.exists = true;
        paymentOrder.invoiceId = input.invoiceId;
        paymentOrder.status = TopazTypes.PaymentOrderStatus.CREATED;
        paymentOrder.customerRefNumber = input.customerRefNumber;
        paymentOrder.instructedAmountMinor = invoice.amountMinor;
        paymentOrder.instructedCurrency = invoice.currency;
        paymentOrder.chargeBearer = input.chargeBearer;
        paymentOrder.purposeCode = input.purposeCode;
        paymentOrder.valueDate = input.valueDate;
        paymentOrder.paymentType = input.paymentType;
        paymentOrder.preparerRef = input.preparerRef;
        paymentOrder.createdAt = _timestamp();
        paymentOrder.updatedAt = paymentOrder.createdAt;
        _copyParticipantStorageToStorage(paymentOrder.payer, invoice.developer);
        _copyParticipantStorageToStorage(paymentOrder.payee, invoice.mainContractor);
        _setAccountInfoFromCalldata(paymentOrder.fromAccount, input.fromAccount);
        _deriveToAccountFromInvoice(paymentOrder.toAccount, invoice.bankAccount);
        _replaceStringArray(paymentOrder.remittanceInformation, input.remittanceInformation);
        _replaceStringArray(paymentOrder.bankInformation, input.bankInformation);
        _resetPaymentApproversFromProject(paymentOrder, project);
        invoice.paymentOrderIds.push(paymentOrderId);

        emit PaymentOrderCreated(paymentOrderId, input.invoiceId, paymentOrder.status);
        emit PaymentOrderStatusChanged(paymentOrderId, paymentOrder.status);
    }

    function approvePaymentOrder(uint256 paymentOrderId)
        external
    {
        PaymentOrderRecord storage paymentOrder = _requirePaymentOrder(paymentOrderId);
        if (!_isPaymentAwaitingApproval(paymentOrder.status)) {
            revert InvalidState("payment order is not awaiting approval");
        }

        uint256 approverIndex = _findFirstPendingApprover(paymentOrder.approvers);
        if (approverIndex == type(uint256).max) {
            revert InvalidState("payment order has no pending approver");
        }
        TopazTypes.ApprovalSlot storage slot = paymentOrder.approvers[approverIndex];
        if (slot.wallet != msg.sender) {
            revert InvalidApproverTurn(slot.wallet, msg.sender);
        }

        slot.status = TopazTypes.ApprovalStatus.APPROVED;
        slot.actionTimestamp = _timestamp();
        paymentOrder.status = _derivePaymentApprovalStatus(paymentOrder.approvers);
        paymentOrder.updatedAt = _timestamp();

        emit PaymentOrderStatusChanged(paymentOrderId, paymentOrder.status);

        if (paymentOrder.status == TopazTypes.PaymentOrderStatus.ALL_APPROVED) {
            _createPayment(paymentOrderId, paymentOrder);
        }
    }

    function rejectPaymentOrder(uint256 paymentOrderId, bytes32 authorHash, string calldata commentRef)
        external
    {
        PaymentOrderRecord storage paymentOrder = _requirePaymentOrder(paymentOrderId);
        if (!_isPaymentAwaitingApproval(paymentOrder.status)) {
            revert InvalidState("payment order is not awaiting approval");
        }
        if (bytes(commentRef).length == 0) {
            revert InvalidInput("commentRef is required");
        }

        uint256 approverIndex = _findFirstPendingApprover(paymentOrder.approvers);
        if (approverIndex == type(uint256).max) {
            revert InvalidState("payment order has no pending approver");
        }
        TopazTypes.ApprovalSlot storage slot = paymentOrder.approvers[approverIndex];
        if (slot.wallet != msg.sender) {
            revert InvalidApproverTurn(slot.wallet, msg.sender);
        }

        slot.status = TopazTypes.ApprovalStatus.REJECTED;
        slot.actionTimestamp = _timestamp();
        paymentOrder.status = TopazTypes.PaymentOrderStatus.REJECTED;
        paymentOrder.updatedAt = _timestamp();
        _addComment(paymentOrder.comments, authorHash, commentRef);

        emit PaymentOrderStatusChanged(paymentOrderId, paymentOrder.status);
    }

    function resubmitPaymentOrder(TopazTypes.ResubmitPaymentOrderInput calldata input)
        external
        onlyRole(FINANCE_ROLE)
    {
        PaymentOrderRecord storage paymentOrder = _requirePaymentOrder(input.paymentOrderId);
        if (paymentOrder.status != TopazTypes.PaymentOrderStatus.REJECTED) {
            revert InvalidState("payment order is not rejected");
        }
        if (bytes(input.customerRefNumber).length == 0) {
            revert InvalidInput("customer reference is required");
        }
        if (bytes(input.chargeBearer).length == 0) {
            revert InvalidInput("chargeBearer is required");
        }
        if (bytes(input.purposeCode).length == 0) {
            revert InvalidInput("purposeCode is required");
        }
        if (bytes(input.paymentType).length == 0) {
            revert InvalidInput("paymentType is required");
        }
        _validateAccountInfo(input.fromAccount);

        InvoiceRecord storage invoice = _requireInvoice(paymentOrder.invoiceId);
        ClaimRecord storage claim = _requireClaim(invoice.claimId);
        ProjectRecord storage project = _requireProject(claim.projectId);

        _setAccountInfoFromCalldata(paymentOrder.fromAccount, input.fromAccount);
        paymentOrder.customerRefNumber = input.customerRefNumber;
        paymentOrder.chargeBearer = input.chargeBearer;
        paymentOrder.purposeCode = input.purposeCode;
        paymentOrder.valueDate = input.valueDate;
        paymentOrder.paymentType = input.paymentType;
        paymentOrder.preparerRef = input.preparerRef;
        paymentOrder.paymentId = 0;
        paymentOrder.bankPaymentRef = "";
        _replaceStringArray(paymentOrder.remittanceInformation, input.remittanceInformation);
        _replaceStringArray(paymentOrder.bankInformation, input.bankInformation);
        _resetPaymentApproversFromProject(paymentOrder, project);
        paymentOrder.status = TopazTypes.PaymentOrderStatus.RESUBMIT;
        paymentOrder.updatedAt = _timestamp();

        emit PaymentOrderStatusChanged(input.paymentOrderId, paymentOrder.status);
    }

    function recordBankPaymentReference(uint256 paymentOrderId, string calldata bankPaymentRef)
        external
        onlyRole(FINANCE_ROLE)
    {
        PaymentOrderRecord storage paymentOrder = _requirePaymentOrder(paymentOrderId);
        if (paymentOrder.paymentId == 0) {
            revert InvalidState("payment has not been created");
        }
        if (bytes(bankPaymentRef).length == 0) {
            revert InvalidInput("bankPaymentRef is required");
        }

        paymentOrder.bankPaymentRef = bankPaymentRef;
        paymentOrder.updatedAt = _timestamp();

        emit BankPaymentReferenceRecorded(paymentOrderId, bankPaymentRef);
    }

    function getProjectSummary(uint256 projectId)
        external
        view
        returns (
            string memory externalProjectId,
            string memory name,
            TopazTypes.ProjectStatus status,
            TopazTypes.Participant memory developer,
            TopazTypes.Participant[] memory mainContractors,
            TopazTypes.ApproverConfig[] memory claimApprovers,
            TopazTypes.ApproverConfig[] memory paymentApprovers,
            string[] memory bankAccountRefs,
            uint64 createdAt,
            uint64 updatedAt,
            uint256 claimCount
        )
    {
        ProjectRecord storage project = _requireProject(projectId);
        return (
            project.externalProjectId,
            project.name,
            project.status,
            project.developer,
            project.mainContractors,
            project.claimApprovers,
            project.paymentApprovers,
            project.bankAccountRefs,
            project.createdAt,
            project.updatedAt,
            _projectClaimIds[projectId].length
        );
    }

    function getProjectClaimIds(uint256 projectId)
        external
        view
        returns (uint256[] memory)
    {
        _requireProject(projectId);
        return _projectClaimIds[projectId];
    }

    function getProjectPaymentApproverCount(uint256 projectId)
        external
        view
        returns (uint256)
    {
        ProjectRecord storage project = _requireProject(projectId);
        return project.paymentApprovers.length;
    }

    function getClaimSummary(uint256 claimId)
        external
        view
        returns (
            uint256 projectId,
            string memory descriptionRef,
            TopazTypes.ClaimStatus status,
            TopazTypes.Participant memory contractor,
            TopazTypes.Participant memory developer,
            uint256 documentCount,
            uint256 invoiceCount,
            uint64 createdAt,
            uint64 updatedAt
        )
    {
        ClaimRecord storage claim = _requireClaim(claimId);
        return (
            claim.projectId,
            claim.descriptionRef,
            claim.status,
            _participantToMemory(claim.contractor),
            _participantToMemory(claim.developer),
            claim.documents.length,
            claim.invoiceIds.length,
            claim.createdAt,
            claim.updatedAt
        );
    }

    function getClaimDocument(uint256 claimId, uint256 index)
        external
        view
        returns (TopazTypes.DocumentRecord memory)
    {
        ClaimRecord storage claim = _requireClaim(claimId);
        return claim.documents[index];
    }

    function getClaimApproverCount(uint256 claimId)
        external
        view
        returns (uint256)
    {
        ClaimRecord storage claim = _requireClaim(claimId);
        return claim.approvers.length;
    }

    function getClaimApprover(uint256 claimId, uint256 index)
        external
        view
        returns (TopazTypes.ApprovalSlot memory)
    {
        ClaimRecord storage claim = _requireClaim(claimId);
        return claim.approvers[index];
    }

    function getClaimInvoiceIds(uint256 claimId)
        external
        view
        returns (uint256[] memory)
    {
        ClaimRecord storage claim = _requireClaim(claimId);
        return claim.invoiceIds;
    }

    function getInvoiceSummary(uint256 invoiceId)
        external
        view
        returns (
            uint256 claimId,
            TopazTypes.InvoiceStatus status,
            TopazTypes.BankAccountDetails memory bankAccount,
            uint256 amountMinor,
            string memory currency,
            TopazTypes.Participant memory mainContractor,
            TopazTypes.Participant memory developer,
            uint256 paymentOrderCount,
            uint64 createdAt,
            uint64 updatedAt
        )
    {
        InvoiceRecord storage invoice = _requireInvoice(invoiceId);
        return (
            invoice.claimId,
            invoice.status,
            invoice.bankAccount,
            invoice.amountMinor,
            invoice.currency,
            _participantToMemory(invoice.mainContractor),
            _participantToMemory(invoice.developer),
            invoice.paymentOrderIds.length,
            invoice.createdAt,
            invoice.updatedAt
        );
    }

    function getInvoiceDocument(uint256 invoiceId, uint256 index)
        external
        view
        returns (string memory documentId, bytes32 documentHash)
    {
        InvoiceRecord storage invoice = _requireInvoice(invoiceId);
        TopazTypes.DocumentRecord storage document = invoice.documents[index];
        return (document.documentId, document.documentHash);
    }

    function getInvoicePaymentOrderIds(uint256 invoiceId)
        external
        view
        returns (uint256[] memory)
    {
        InvoiceRecord storage invoice = _requireInvoice(invoiceId);
        uint256[] memory ids = new uint256[](invoice.paymentOrderIds.length);
        for (uint256 i = 0; i < invoice.paymentOrderIds.length; i++) {
            ids[i] = invoice.paymentOrderIds[i];
        }
        return ids;
    }

    function getPaymentOrderSummary(uint256 paymentOrderId)
        external
        view
        returns (
            uint256 invoiceId,
            TopazTypes.PaymentOrderStatus status,
            string memory customerRefNumber,
            uint256 instructedAmountMinor,
            string memory instructedCurrency,
            uint256 paymentId,
            string memory bankPaymentRef,
            uint64 createdAt,
            uint64 updatedAt
        )
    {
        PaymentOrderRecord storage paymentOrder = _requirePaymentOrder(paymentOrderId);
        return (
            paymentOrder.invoiceId,
            paymentOrder.status,
            paymentOrder.customerRefNumber,
            paymentOrder.instructedAmountMinor,
            paymentOrder.instructedCurrency,
            paymentOrder.paymentId,
            paymentOrder.bankPaymentRef,
            paymentOrder.createdAt,
            paymentOrder.updatedAt
        );
    }

    function getPaymentOrderApproverCount(uint256 paymentOrderId)
        external
        view
        returns (uint256)
    {
        PaymentOrderRecord storage paymentOrder = _requirePaymentOrder(paymentOrderId);
        return paymentOrder.approvers.length;
    }

    function getPaymentOrderPaymentId(uint256 paymentOrderId)
        external
        view
        returns (uint256)
    {
        PaymentOrderRecord storage paymentOrder = _requirePaymentOrder(paymentOrderId);
        return paymentOrder.paymentId;
    }

    function getPaymentOrderApprover(uint256 paymentOrderId, uint256 index)
        external
        view
        returns (TopazTypes.ApprovalSlot memory)
    {
        PaymentOrderRecord storage paymentOrder = _requirePaymentOrder(paymentOrderId);
        return paymentOrder.approvers[index];
    }

    function _cascadeProjectApproverChanges(uint256 projectId) internal {
        ProjectRecord storage project = _requireProject(projectId);
        uint256[] storage claimIds = _projectClaimIds[projectId];
        for (uint256 i = 0; i < claimIds.length; i++) {
            ClaimRecord storage claim = _claims[claimIds[i]];
            if (_isClaimAwaitingClaimApproverAction(claim.status)) {
                bool claimChanged = false;
                for (uint256 j = 0; j < claim.approvers.length; j++) {
                    TopazTypes.ApprovalSlot storage slot = claim.approvers[j];
                    if (
                        slot.status == TopazTypes.ApprovalStatus.PENDING
                            && !_containsApprover(project.claimApprovers, slot.userHash)
                    ) {
                        slot.status = TopazTypes.ApprovalStatus.INVALID;
                        slot.actionTimestamp = _timestamp();
                        claimChanged = true;
                    }
                }
                if (claimChanged) {
                    claim.status = _recomputeClaimStatusAfterApproverInvalidation(claim.approvers, claim.status);
                    claim.updatedAt = _timestamp();
                    emit ClaimStatusChanged(claimIds[i], claim.status);
                }
            }

            for (uint256 j = 0; j < claim.invoiceIds.length; j++) {
                InvoiceRecord storage invoice = _invoices[claim.invoiceIds[j]];
                for (uint256 k = 0; k < invoice.paymentOrderIds.length; k++) {
                    PaymentOrderRecord storage paymentOrder = _paymentOrders[invoice.paymentOrderIds[k]];
                    if (paymentOrder.exists && _isPaymentAwaitingApproval(paymentOrder.status)) {
                        bool paymentChanged = false;
                        for (uint256 m = 0; m < paymentOrder.approvers.length; m++) {
                            TopazTypes.ApprovalSlot storage slot = paymentOrder.approvers[m];
                            if (
                                slot.status == TopazTypes.ApprovalStatus.PENDING
                                    && !_containsApprover(project.paymentApprovers, slot.userHash)
                            ) {
                                slot.status = TopazTypes.ApprovalStatus.INVALID;
                                slot.actionTimestamp = _timestamp();
                                paymentChanged = true;
                            }
                        }
                        if (paymentChanged) {
                            TopazTypes.PaymentOrderStatus previousStatus = paymentOrder.status;
                            paymentOrder.status =
                                _recomputePaymentStatusAfterApproverInvalidation(paymentOrder.approvers, paymentOrder.status);
                            paymentOrder.updatedAt = _timestamp();
                            if (
                                previousStatus != TopazTypes.PaymentOrderStatus.ALL_APPROVED
                                    && paymentOrder.status == TopazTypes.PaymentOrderStatus.ALL_APPROVED
                            ) {
                                _createPayment(invoice.paymentOrderIds[k], paymentOrder);
                            }
                            emit PaymentOrderStatusChanged(invoice.paymentOrderIds[k], paymentOrder.status);
                            if (
                                previousStatus != TopazTypes.PaymentOrderStatus.ALL_APPROVED
                                    && paymentOrder.status == TopazTypes.PaymentOrderStatus.ALL_APPROVED
                                    && bytes(paymentOrder.bankPaymentRef).length == 0
                            ) {
                                emit BankPaymentRequested(
                                    invoice.paymentOrderIds[k],
                                    paymentOrder.invoiceId,
                                    paymentOrder.customerRefNumber
                                );
                            }
                        }
                    }
                }
            }
        }
    }

    function _findProjectContractor(ProjectRecord storage project, address wallet) internal view returns (uint256) {
        for (uint256 i = 0; i < project.mainContractors.length; i++) {
            if (project.mainContractors[i].wallet == wallet) {
                return i;
            }
        }
        return type(uint256).max;
    }

    function _isProjectContractor(ProjectRecord storage project, address wallet) internal view returns (bool) {
        return _findProjectContractor(project, wallet) != type(uint256).max;
    }

    function _projectContractor(ProjectRecord storage project, address wallet)
        internal
        view
        returns (TopazTypes.Participant storage)
    {
        uint256 index = _findProjectContractor(project, wallet);
        if (index == type(uint256).max) {
            revert InvalidActor(wallet);
        }
        return project.mainContractors[index];
    }

    function _findPendingClaimApprover(ClaimRecord storage claim, address wallet) internal view returns (uint256) {
        for (uint256 i = 0; i < claim.approvers.length; i++) {
            if (claim.approvers[i].wallet == wallet && claim.approvers[i].status == TopazTypes.ApprovalStatus.PENDING) {
                return i;
            }
        }
        return type(uint256).max;
    }

    function _findFirstPendingApprover(TopazTypes.ApprovalSlot[] storage approvers) internal view returns (uint256) {
        for (uint256 i = 0; i < approvers.length; i++) {
            if (approvers[i].status == TopazTypes.ApprovalStatus.PENDING) {
                return i;
            }
        }
        return type(uint256).max;
    }

    function _isClaimAwaitingClaimApproverAction(TopazTypes.ClaimStatus status) internal pure returns (bool) {
        return (
            status == TopazTypes.ClaimStatus.SUBMITTED ||
                status == TopazTypes.ClaimStatus.RESUBMITTED ||
                status == TopazTypes.ClaimStatus.PARTIAL_CA_APPROVED
        );
    }

    function _recomputeClaimStatusAfterApproverInvalidation(
        TopazTypes.ApprovalSlot[] storage approvers,
        TopazTypes.ClaimStatus currentStatus
    ) internal view returns (TopazTypes.ClaimStatus) {
        if (!_isClaimAwaitingClaimApproverAction(currentStatus)) {
            return currentStatus;
        }
        for (uint256 i = 0; i < approvers.length; i++) {
            if (approvers[i].status == TopazTypes.ApprovalStatus.REJECTED) {
                return TopazTypes.ClaimStatus.CA_REJECTED;
            }
            if (approvers[i].status == TopazTypes.ApprovalStatus.PENDING) {
                return i == 0 ? TopazTypes.ClaimStatus.SUBMITTED : TopazTypes.ClaimStatus.PARTIAL_CA_APPROVED;
            }
        }
        return TopazTypes.ClaimStatus.ALL_CA_APPROVED;
    }

    function _recomputePaymentStatusAfterApproverInvalidation(
        TopazTypes.ApprovalSlot[] storage approvers,
        TopazTypes.PaymentOrderStatus currentStatus
    ) internal view returns (TopazTypes.PaymentOrderStatus) {
        if (!_isPaymentAwaitingApproval(currentStatus)) {
            return currentStatus;
        }
        for (uint256 i = 0; i < approvers.length; i++) {
            if (approvers[i].status == TopazTypes.ApprovalStatus.REJECTED) {
                return TopazTypes.PaymentOrderStatus.REJECTED;
            }
            if (approvers[i].status == TopazTypes.ApprovalStatus.PENDING) {
                return i == 0 ? TopazTypes.PaymentOrderStatus.CREATED : TopazTypes.PaymentOrderStatus.PARTIAL_APPROVED;
            }
        }
        return TopazTypes.PaymentOrderStatus.ALL_APPROVED;
    }

    function _deriveClaimApprovalStatus(TopazTypes.ApprovalSlot[] storage approvers)
        internal
        view
        returns (TopazTypes.ClaimStatus)
    {
        for (uint256 i = 0; i < approvers.length; i++) {
            if (approvers[i].status == TopazTypes.ApprovalStatus.REJECTED) {
                return TopazTypes.ClaimStatus.CA_REJECTED;
            }
            if (approvers[i].status == TopazTypes.ApprovalStatus.PENDING) {
                return i == 0 ? TopazTypes.ClaimStatus.SUBMITTED : TopazTypes.ClaimStatus.PARTIAL_CA_APPROVED;
            }
        }
        return TopazTypes.ClaimStatus.ALL_CA_APPROVED;
    }

    function _derivePaymentApprovalStatus(TopazTypes.ApprovalSlot[] storage approvers)
        internal
        view
        returns (TopazTypes.PaymentOrderStatus)
    {
        for (uint256 i = 0; i < approvers.length; i++) {
            if (approvers[i].status == TopazTypes.ApprovalStatus.REJECTED) {
                return TopazTypes.PaymentOrderStatus.REJECTED;
            }
            if (approvers[i].status == TopazTypes.ApprovalStatus.PENDING) {
                return i == 0 ? TopazTypes.PaymentOrderStatus.CREATED : TopazTypes.PaymentOrderStatus.PARTIAL_APPROVED;
            }
        }
        return TopazTypes.PaymentOrderStatus.ALL_APPROVED;
    }

    function _isPaymentAwaitingApproval(TopazTypes.PaymentOrderStatus status) internal pure returns (bool) {
        return (
            status == TopazTypes.PaymentOrderStatus.CREATED ||
                status == TopazTypes.PaymentOrderStatus.RESUBMIT ||
                status == TopazTypes.PaymentOrderStatus.PARTIAL_APPROVED
        );
    }

    function _containsApprover(TopazTypes.ApproverConfig[] storage approvers, bytes32 userHash)
        internal
        view
        returns (bool)
    {
        for (uint256 i = 0; i < approvers.length; i++) {
            if (approvers[i].userHash == userHash) {
                return true;
            }
        }
        return false;
    }

    // Match stored Claim parties against the updated Project contractor list by business identity.
    function _containsParticipantIdentity(
        TopazTypes.Participant[] calldata participants,
        TopazTypes.Participant storage participant
    ) internal view returns (bool) {
        bytes32 identity = _participantIdentityKey(participant);
        for (uint256 i = 0; i < participants.length; i++) {
            if (_participantIdentityKey(participants[i]) == identity) {
                return true;
            }
        }
        return false;
    }

    // Keep Corda project id uniqueness scoped to the developer business identity.
    function _projectExternalIdKey(TopazTypes.Participant calldata developer, string calldata externalProjectId)
        internal
        pure
        returns (bytes32)
    {
        return keccak256(abi.encodePacked(_participantIdentityKey(developer), externalProjectId));
    }

    function _projectExternalIdKey(TopazTypes.Participant storage developer, string storage externalProjectId)
        internal
        view
        returns (bytes32)
    {
        return keccak256(abi.encodePacked(_participantIdentityKey(developer), externalProjectId));
    }

    function _projectExternalIdKey(TopazTypes.Participant storage developer, string calldata externalProjectId)
        internal
        view
        returns (bytes32)
    {
        return keccak256(abi.encodePacked(_participantIdentityKey(developer), externalProjectId));
    }

    // Preserve Corda participant semantics by matching Project/Claim parties through externalRef/legalName, not the signer wallet.
    function _participantIdentityKey(TopazTypes.Participant calldata participant) internal pure returns (bytes32) {
        if (bytes(participant.externalRef).length != 0) {
            return keccak256(bytes(participant.externalRef));
        }
        return keccak256(bytes(participant.legalName));
    }

    function _participantIdentityKey(TopazTypes.Participant storage participant) internal view returns (bytes32) {
        if (bytes(participant.externalRef).length != 0) {
            return keccak256(bytes(participant.externalRef));
        }
        return keccak256(bytes(participant.legalName));
    }
    function _removeApproverConfig(TopazTypes.ApproverConfig[] storage approvers, bytes32 userHash)
        internal
        returns (bool)
    {
        for (uint256 i = 0; i < approvers.length; i++) {
            if (approvers[i].userHash == userHash) {
                for (uint256 j = i; j + 1 < approvers.length; j++) {
                    approvers[j] = approvers[j + 1];
                }
                approvers.pop();
                return true;
            }
        }
        return false;
    }
    function _resetClaimApproversFromProject(ClaimRecord storage claim, ProjectRecord storage project) internal {
        _clearApprovalSlots(claim.approvers);
        for (uint256 i = 0; i < project.claimApprovers.length; i++) {
            _setApprovalSlotFromConfig(claim.approvers, project.claimApprovers[i]);
        }
    }
    function _resetPaymentApproversFromProject(
        PaymentOrderRecord storage paymentOrder,
        ProjectRecord storage project
    ) internal {
        _clearApprovalSlots(paymentOrder.approvers);
        for (uint256 i = 0; i < project.paymentApprovers.length; i++) {
            _setApprovalSlotFromConfig(paymentOrder.approvers, project.paymentApprovers[i]);
        }
    }
    function _setApprovalSlotFromConfig(
        TopazTypes.ApprovalSlot[] storage target,
        TopazTypes.ApproverConfig storage config
    ) internal {
        target.push();
        TopazTypes.ApprovalSlot storage approver = target[target.length - 1];
        approver.wallet = config.wallet;
        approver.userHash = config.userHash;
        approver.email = config.email;
        approver.firstName = config.firstName;
        approver.lastName = config.lastName;
        approver.userProfileName = config.userProfileName;
        approver.roleName = config.roleName;
        approver.externalRef = config.externalRef;
        approver.status = TopazTypes.ApprovalStatus.PENDING;
        approver.actionTimestamp = 0;
    }
    function _deriveToAccountFromInvoice(
        TopazTypes.AccountInfo storage target,
        TopazTypes.BankAccountDetails storage bankAccount
    ) internal {
        target.accountName = bankAccount.bankAccountHolderName;
        target.accountNumber = bankAccount.bankAccountNumberRef;
        target.addressLine1 = bankAccount.registeredAddress;
        target.addressLine2 = "";
        target.bic = bankAccount.swiftAddress;
        target.ultimateName = bankAccount.bankName;
    }

    function _createPayment(uint256 paymentOrderId, PaymentOrderRecord storage paymentOrder) internal {
        TopazTypes.PaymentRequest memory request = TopazTypes.PaymentRequest({
            paymentOrderId: paymentOrderId,
            invoiceId: paymentOrder.invoiceId,
            payer: _participantToMemory(paymentOrder.payer),
            payee: _participantToMemory(paymentOrder.payee),
            fromAccount: _accountInfoToMemory(paymentOrder.fromAccount),
            toAccount: _accountInfoToMemory(paymentOrder.toAccount),
            customerRefNumber: paymentOrder.customerRefNumber,
            instructedAmountMinor: paymentOrder.instructedAmountMinor,
            instructedCurrency: paymentOrder.instructedCurrency,
            chargeBearer: paymentOrder.chargeBearer,
            remittanceInformation: _copyStringArrayToMemory(paymentOrder.remittanceInformation),
            purposeCode: paymentOrder.purposeCode,
            valueDate: paymentOrder.valueDate,
            bankInformation: _copyStringArrayToMemory(paymentOrder.bankInformation),
            paymentType: paymentOrder.paymentType,
            preparerRef: paymentOrder.preparerRef
        });

        uint256 paymentId = _payment.createPayment(request);
        paymentOrder.paymentId = paymentId;
        paymentOrder.updatedAt = _timestamp();

        emit PaymentCreatedForOrder(paymentOrderId, paymentId, paymentOrder.invoiceId);
        emit BankPaymentRequested(paymentOrderId, paymentOrder.invoiceId, paymentOrder.customerRefNumber);
    }

    function _addComment(
        TopazTypes.CommentRecord[] storage target,
        bytes32 authorHash,
        string calldata commentRef
    ) internal {
        if (authorHash == bytes32(0)) {
            revert InvalidInput("authorHash is required");
        }
        if (bytes(commentRef).length == 0) {
            revert InvalidInput("commentRef is required");
        }

        target.push();
        TopazTypes.CommentRecord storage comment = target[target.length - 1];
        comment.authorHash = authorHash;
        comment.commentRef = commentRef;
        comment.timestamp = _timestamp();
    }

    function _requireContractor(address expectedWallet) internal view {
        if (msg.sender != expectedWallet) {
            revert InvalidActor(msg.sender);
        }
    }

    function _requireProject(uint256 projectId) internal view returns (ProjectRecord storage project) {
        project = _projects[projectId];
        if (!project.exists) {
            revert UnknownProject(projectId);
        }
    }

    function _requireClaim(uint256 claimId) internal view returns (ClaimRecord storage claim) {
        claim = _claims[claimId];
        if (!claim.exists) {
            revert UnknownClaim(claimId);
        }
    }

    function _requireInvoice(uint256 invoiceId) internal view returns (InvoiceRecord storage invoice) {
        invoice = _invoices[invoiceId];
        if (!invoice.exists) {
            revert UnknownInvoice(invoiceId);
        }
    }

    function _requirePaymentOrder(uint256 paymentOrderId)
        internal
        view
        returns (PaymentOrderRecord storage paymentOrder)
    {
        paymentOrder = _paymentOrders[paymentOrderId];
        if (!paymentOrder.exists) {
            revert UnknownPaymentOrder(paymentOrderId);
        }
    }

    function _validateProjectInput(
        string calldata externalProjectId,
        string calldata name,
        TopazTypes.Participant[] calldata mainContractors,
        TopazTypes.ApproverConfig[] calldata claimApprovers,
        TopazTypes.ApproverConfig[] calldata paymentApprovers
    ) internal pure {
        if (bytes(externalProjectId).length == 0) {
            revert InvalidInput("externalProjectId is required");
        }
        if (bytes(name).length == 0) {
            revert InvalidInput("name is required");
        }
        if (mainContractors.length == 0) {
            revert InvalidInput("at least one main contractor is required");
        }
        if (mainContractors.length > 15) {
            revert InvalidInput("main contractors cannot exceed 15");
        }
        for (uint256 i = 0; i < mainContractors.length; i++) {
            _validateParticipant(mainContractors[i]);
            // A Project cannot list the same main contractor twice under the same business identity.
            for (uint256 j = i + 1; j < mainContractors.length; j++) {
                if (_participantIdentityKey(mainContractors[i]) == _participantIdentityKey(mainContractors[j])) {
                    revert InvalidInput("duplicate contractor identity");
                }
            }
        }
        _validateApproverConfigs(claimApprovers);
        if (paymentApprovers.length == 0) {
            revert InvalidInput("at least one payment approver is required");
        }
        if (paymentApprovers.length > 15) {
            revert InvalidInput("payment approvers cannot exceed 15");
        }
        _validateApproverConfigs(paymentApprovers);
    }

    // Prevent Project updates from orphaning Claims created by an assigned contractor.
    function _validateClaimedContractorsRemainAssigned(
        uint256 projectId,
        TopazTypes.Participant[] calldata mainContractors
    ) internal view {
        uint256[] storage claimIds = _projectClaimIds[projectId];
        for (uint256 i = 0; i < claimIds.length; i++) {
            ClaimRecord storage claim = _claims[claimIds[i]];
            if (!_containsParticipantIdentity(mainContractors, claim.contractor)) {
                revert InvalidInput("cannot remove contractor with existing claims");
            }
        }
    }

    // Preserve Corda approver lookup semantics by allowing each approver userHash only once.
    function _validateApproverConfigs(TopazTypes.ApproverConfig[] calldata approvers) internal pure {
        for (uint256 i = 0; i < approvers.length; i++) {
            if (approvers[i].wallet == address(0) || approvers[i].userHash == bytes32(0)) {
                revert InvalidInput("approver wallet and userHash are required");
            }
            for (uint256 j = i + 1; j < approvers.length; j++) {
                if (approvers[i].userHash == approvers[j].userHash) {
                    revert InvalidInput("duplicate approver userHash");
                }
            }
        }
    }

    function _validateParticipant(TopazTypes.Participant calldata participant) internal pure {
        if (participant.wallet == address(0)) {
            revert InvalidInput("participant wallet is required");
        }
        if (bytes(participant.legalName).length == 0) {
            revert InvalidInput("participant legalName is required");
        }
    }

    function _validateBankAccount(TopazTypes.BankAccountDetails calldata bankAccount) internal pure {
        if (bytes(bankAccount.swiftAddress).length == 0) {
            revert InvalidInput("swiftAddress is required");
        }
        if (bytes(bankAccount.bankAccountHolderName).length == 0) {
            revert InvalidInput("bankAccountHolderName is required");
        }
        if (bytes(bankAccount.bankAccountNumberRef).length == 0) {
            revert InvalidInput("bankAccountNumberRef is required");
        }
        if (bytes(bankAccount.bankName).length == 0) {
            revert InvalidInput("bankName is required");
        }
        if (bytes(bankAccount.registeredAddress).length == 0) {
            revert InvalidInput("registeredAddress is required");
        }
        if (bytes(bankAccount.currency).length == 0) {
            revert InvalidInput("currency is required");
        }
    }

    function _validateAccountInfo(TopazTypes.AccountInfo calldata accountInfo) internal pure {
        if (bytes(accountInfo.accountName).length == 0) {
            revert InvalidInput("accountName is required");
        }
        if (bytes(accountInfo.accountNumber).length == 0) {
            revert InvalidInput("accountNumber is required");
        }
    }

    function _validateDocuments(TopazTypes.DocumentInput[] calldata documents, string memory emptyArrayMessage)
        internal
        pure
    {
        if (documents.length == 0) {
            revert InvalidInput(emptyArrayMessage);
        }
        for (uint256 i = 0; i < documents.length; i++) {
            if (bytes(documents[i].documentId).length == 0) {
                revert InvalidInput("documentId is required");
            }
            if (documents[i].documentHash == bytes32(0)) {
                revert InvalidInput("documentHash is required");
            }
            for (uint256 j = i + 1; j < documents.length; j++) {
                if (keccak256(bytes(documents[i].documentId)) == keccak256(bytes(documents[j].documentId))) {
                    revert InvalidInput("duplicate documentId");
                }
            }
        }
    }

    function _replaceProjectParticipants(
        TopazTypes.Participant[] storage target,
        TopazTypes.Participant[] calldata source
    ) internal {
        _clearParticipants(target);
        for (uint256 i = 0; i < source.length; i++) {
            target.push();
            _setParticipantFromCalldata(target[target.length - 1], source[i]);
        }
    }

    function _replaceProjectApproverConfigs(
        TopazTypes.ApproverConfig[] storage target,
        TopazTypes.ApproverConfig[] calldata source
    ) internal {
        _clearApproverConfigs(target);
        for (uint256 i = 0; i < source.length; i++) {
            if (source[i].wallet == address(0) || source[i].userHash == bytes32(0)) {
                revert InvalidInput("approver wallet and userHash are required");
            }
            target.push();
            TopazTypes.ApproverConfig storage approver = target[target.length - 1];
            approver.wallet = source[i].wallet;
            approver.userHash = source[i].userHash;
            approver.email = source[i].email;
            approver.firstName = source[i].firstName;
            approver.lastName = source[i].lastName;
            approver.userProfileName = source[i].userProfileName;
            approver.roleName = source[i].roleName;
            approver.externalRef = source[i].externalRef;
        }
    }

    function _replaceStringArray(string[] storage target, string[] calldata source) internal {
        _clearStringArray(target);
        for (uint256 i = 0; i < source.length; i++) {
            target.push(source[i]);
        }
    }

    function _replaceDocumentArray(
        TopazTypes.DocumentRecord[] storage target,
        TopazTypes.DocumentInput[] calldata source
    ) internal {
        _clearDocumentRecords(target);
        for (uint256 i = 0; i < source.length; i++) {
            target.push();
            TopazTypes.DocumentRecord storage document = target[target.length - 1];
            document.documentId = source[i].documentId;
            document.documentHash = source[i].documentHash;
        }
    }

    function _clearParticipants(TopazTypes.Participant[] storage target) internal {
        while (target.length > 0) {
            target.pop();
        }
    }

    function _clearApproverConfigs(TopazTypes.ApproverConfig[] storage target) internal {
        while (target.length > 0) {
            target.pop();
        }
    }

    function _clearApprovalSlots(TopazTypes.ApprovalSlot[] storage target) internal {
        while (target.length > 0) {
            target.pop();
        }
    }

    function _clearStringArray(string[] storage target) internal {
        while (target.length > 0) {
            target.pop();
        }
    }

    function _clearDocumentRecords(TopazTypes.DocumentRecord[] storage target) internal {
        while (target.length > 0) {
            target.pop();
        }
    }

    function _setParticipantFromCalldata(
        TopazTypes.Participant storage target,
        TopazTypes.Participant calldata source
    ) internal {
        target.wallet = source.wallet;
        target.legalName = source.legalName;
        target.addressLine1 = source.addressLine1;
        target.addressLine2 = source.addressLine2;
        target.bic = source.bic;
        target.lei = source.lei;
        target.externalRef = source.externalRef;
    }

    function _participantToMemory(TopazTypes.Participant storage source)
        internal
        view
        returns (TopazTypes.Participant memory)
    {
        return TopazTypes.Participant({
            wallet: source.wallet,
            legalName: source.legalName,
            addressLine1: source.addressLine1,
            addressLine2: source.addressLine2,
            bic: source.bic,
            lei: source.lei,
            externalRef: source.externalRef
        });
    }

    function _copyParticipantStorageToStorage(
        TopazTypes.Participant storage target,
        TopazTypes.Participant storage source
    ) internal {
        target.wallet = source.wallet;
        target.legalName = source.legalName;
        target.addressLine1 = source.addressLine1;
        target.addressLine2 = source.addressLine2;
        target.bic = source.bic;
        target.lei = source.lei;
        target.externalRef = source.externalRef;
    }

    function _setBankAccountFromCalldata(
        TopazTypes.BankAccountDetails storage target,
        TopazTypes.BankAccountDetails calldata source
    ) internal {
        target.swiftAddress = source.swiftAddress;
        target.bankAccountHolderName = source.bankAccountHolderName;
        target.bankAccountNumberRef = source.bankAccountNumberRef;
        target.bankName = source.bankName;
        target.registeredAddress = source.registeredAddress;
        target.currency = source.currency;
    }

    function _setAccountInfoFromCalldata(
        TopazTypes.AccountInfo storage target,
        TopazTypes.AccountInfo calldata source
    ) internal {
        target.accountName = source.accountName;
        target.accountNumber = source.accountNumber;
        target.addressLine1 = source.addressLine1;
        target.addressLine2 = source.addressLine2;
        target.bic = source.bic;
        target.ultimateName = source.ultimateName;
    }

    function _accountInfoToMemory(TopazTypes.AccountInfo storage source)
        internal
        view
        returns (TopazTypes.AccountInfo memory)
    {
        return TopazTypes.AccountInfo({
            accountName: source.accountName,
            accountNumber: source.accountNumber,
            addressLine1: source.addressLine1,
            addressLine2: source.addressLine2,
            bic: source.bic,
            ultimateName: source.ultimateName
        });
    }

    function _copyStringArrayToMemory(string[] storage source) internal view returns (string[] memory target) {
        target = new string[](source.length);
        for (uint256 i = 0; i < source.length; i++) {
            target[i] = source[i];
        }
    }

    function _timestamp() internal view returns (uint64) {
        return uint64(block.timestamp);
    }
}
