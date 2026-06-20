// SPDX-License-Identifier: MIT
pragma solidity ^0.8.24;

library TopazTypes {
    enum ApprovalStatus {
        PENDING,
        APPROVED,
        REJECTED,
        INVALID
    }

    enum ProjectStatus {
        NONE,
        CREATED,
        UPDATED,
        PENDING_DELETE,
        DELETED
    }

    enum ClaimStatus {
        NONE,
        SUBMITTED,
        RESUBMITTED,
        PARTIAL_CA_APPROVED,
        ALL_CA_APPROVED,
        APPROVED,
        CA_REJECTED,
        PO_REJECTED,
        DISCARDED,
        DELETED
    }

    enum InvoiceStatus {
        NONE,
        SUBMITTED,
        PROJECT_OFFICER_APPROVED,
        PROJECT_OFFICER_REJECTED,
        FINANCE_DEPARTMENT_APPROVED,
        FINANCE_DEPARTMENT_REJECTED,
        DISCARDED,
        DELETED
    }

    enum PaymentOrderStatus {
        NONE,
        CREATED,
        PARTIAL_APPROVED,
        ALL_APPROVED,
        REJECTED,
        RESUBMIT
    }

    struct Participant {
        address wallet;
        string legalName;
        string addressLine1;
        string addressLine2;
        string bic;
        string lei;
        string externalRef;
    }

    struct ApproverConfig {
        address wallet;
        bytes32 userHash;
        string roleName;
        string externalRef;
    }

    struct ApprovalSlot {
        address wallet;
        bytes32 userHash;
        string roleName;
        string externalRef;
        ApprovalStatus status;
        uint64 actionTimestamp;
    }

    struct CommentRecord {
        bytes32 authorHash;
        string commentRef;
        uint64 timestamp;
    }

    struct DocumentInput {
        string documentId;
        bytes32 documentHash;
    }

    struct DocumentRecord {
        string documentId;
        bytes32 documentHash;
    }

    struct BankAccountDetails {
        string swiftAddress;
        string bankAccountHolderName;
        string bankAccountNumberRef;
        string bankName;
        string registeredAddress;
        string currency;
    }

    struct AccountInfo {
        string accountName;
        string accountNumber;
        string addressLine1;
        string addressLine2;
        string bic;
        string ultimateName;
    }

    struct ContactInput {
        string party;
        string contactType;
        string name;
        string contactPerson;
        string contactEmail;
        string contactNumber;
        string location;
        string domain;
        string accountName;
    }

    struct CreateProjectInput {
        string externalProjectId;
        string name;
        Participant developer;
        Participant[] mainContractors;
        ApproverConfig[] claimApprovers;
        ApproverConfig[] paymentApprovers;
        string[] bankAccountRefs;
    }

    struct UpdateProjectInput {
        uint256 projectId;
        string externalProjectId;
        string name;
        Participant[] mainContractors;
        ApproverConfig[] claimApprovers;
        ApproverConfig[] paymentApprovers;
        string[] bankAccountRefs;
    }

    struct SubmitClaimInput {
        uint256 projectId;
        string descriptionRef;
        DocumentInput[] documents;
    }

    struct UpdateClaimInput {
        uint256 claimId;
        string descriptionRef;
        DocumentInput[] documents;
    }

    struct SubmitInvoiceInput {
        uint256 claimId;
        uint256 amountMinor;
        BankAccountDetails bankAccount;
        DocumentInput[] documents;
    }

    struct UpdateInvoiceInput {
        uint256 invoiceId;
        uint256 amountMinor;
        BankAccountDetails bankAccount;
        DocumentInput[] documents;
    }

    struct CreatePaymentOrderInput {
        uint256 invoiceId;
        AccountInfo fromAccount;
        string customerRefNumber;
        string chargeBearer;
        string[] remittanceInformation;
        string purposeCode;
        uint64 valueDate;
        string[] bankInformation;
        string paymentType;
        string preparerRef;
    }

    struct ResubmitPaymentOrderInput {
        uint256 paymentOrderId;
        AccountInfo fromAccount;
        string customerRefNumber;
        string chargeBearer;
        string[] remittanceInformation;
        string purposeCode;
        uint64 valueDate;
        string[] bankInformation;
        string paymentType;
        string preparerRef;
    }

    struct PaymentRequest {
        uint256 paymentOrderId;
        uint256 invoiceId;
        Participant payer;
        Participant payee;
        AccountInfo fromAccount;
        AccountInfo toAccount;
        string customerRefNumber;
        uint256 instructedAmountMinor;
        string instructedCurrency;
        string chargeBearer;
        string[] remittanceInformation;
        string purposeCode;
        uint64 valueDate;
        string[] bankInformation;
        string paymentType;
        string preparerRef;
    }

    struct PaymentAcceptance {
        uint256 paymentId;
        string settlementBankRef;
        uint64 settlementDate;
    }

    struct PaymentRejection {
        uint256 paymentId;
        string rejectCode;
        string rejectReason;
        uint64 rejectDate;
    }

    struct PaymentReceiptRequest {
        uint256 paymentId;
        string transactionRefNum;
        string relatedReference;
        string orderingCustomer;
        string orderingInstitution;
        string remittanceInfo;
        string valueDate;
    }
}

