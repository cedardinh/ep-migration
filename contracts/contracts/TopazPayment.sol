// SPDX-License-Identifier: MIT
pragma solidity ^0.8.24;

import {AccessControl} from "@openzeppelin/contracts/access/AccessControl.sol";
import {ITopazPayment} from "./ITopazPayment.sol";
import {TopazTypes} from "./TopazTypes.sol";

contract TopazPayment is AccessControl, ITopazPayment {
    bytes32 public constant LIFECYCLE_ROLE = keccak256("LIFECYCLE_ROLE");
    bytes32 public constant PAYMENT_OPERATOR_ROLE = keccak256("PAYMENT_OPERATOR_ROLE");

    enum PaymentStatus {
        NONE,
        CREATED,
        ACCEPTED,
        REJECTED
    }

    struct PaymentRecord {
        uint256 paymentId;
        uint256 paymentOrderId;
        uint256 invoiceId;
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
        string preparerRef;
        string settlementBankRef;
        uint64 settlementDate;
        string rejectCode;
        string rejectReason;
        uint64 rejectDate;
        PaymentStatus status;
        uint256 paymentReceiptId;
        uint64 createdAt;
        uint64 updatedAt;
        bool exists;
    }

    struct PaymentReceiptRecord {
        uint256 paymentReceiptId;
        uint256 paymentId;
        string transactionRefNum;
        string relatedReference;
        string orderingCustomer;
        string orderingInstitution;
        string remittanceInfo;
        string valueDate;
        uint64 createdAt;
        bool exists;
    }

    error DuplicatePayment(uint256 paymentOrderId);
    error UnknownPayment(uint256 paymentId);
    error DuplicatePaymentReceipt(uint256 paymentId);
    error UnknownPaymentReceipt(uint256 paymentReceiptId);
    error InvalidInput(string reason);
    error InvalidState(string reason);

    uint256 private _nextPaymentId = 1;
    uint256 private _nextPaymentReceiptId = 1;
    mapping(uint256 => PaymentRecord) private _payments;
    mapping(uint256 => uint256) private _paymentIdByPaymentOrderId;
    mapping(uint256 => PaymentReceiptRecord) private _paymentReceipts;
    mapping(uint256 => uint256) private _paymentReceiptIdByPaymentId;

    event PaymentCreated(
        uint256 indexed paymentId,
        uint256 indexed paymentOrderId,
        uint256 indexed invoiceId,
        string customerRefNumber,
        uint256 instructedAmountMinor,
        string instructedCurrency
    );
    event PaymentAccepted(uint256 indexed paymentId, uint256 indexed paymentOrderId, string settlementBankRef);
    event PaymentRejected(uint256 indexed paymentId, uint256 indexed paymentOrderId, string rejectCode, string rejectReason);
    event PaymentReceiptCreated(
        uint256 indexed paymentReceiptId,
        uint256 indexed paymentId,
        uint256 indexed paymentOrderId,
        string transactionRefNum
    );

    constructor(address admin) {
        _grantRole(DEFAULT_ADMIN_ROLE, admin);
    }

    function createPayment(TopazTypes.PaymentRequest calldata request)
        external
        onlyRole(LIFECYCLE_ROLE)
        returns (uint256 paymentId)
    {
        _validateRequest(request);
        if (_paymentIdByPaymentOrderId[request.paymentOrderId] != 0) {
            revert DuplicatePayment(request.paymentOrderId);
        }

        paymentId = _nextPaymentId++;
        PaymentRecord storage payment = _payments[paymentId];
        payment.exists = true;
        payment.paymentId = paymentId;
        payment.paymentOrderId = request.paymentOrderId;
        payment.invoiceId = request.invoiceId;
        payment.customerRefNumber = request.customerRefNumber;
        payment.instructedAmountMinor = request.instructedAmountMinor;
        payment.instructedCurrency = request.instructedCurrency;
        payment.chargeBearer = request.chargeBearer;
        payment.purposeCode = request.purposeCode;
        payment.valueDate = request.valueDate;
        payment.paymentType = request.paymentType;
        payment.preparerRef = request.preparerRef;
        payment.status = PaymentStatus.CREATED;
        payment.createdAt = _timestamp();
        payment.updatedAt = payment.createdAt;

        _copyParticipant(payment.payer, request.payer);
        _copyParticipant(payment.payee, request.payee);
        _copyAccountInfo(payment.fromAccount, request.fromAccount);
        _copyAccountInfo(payment.toAccount, request.toAccount);
        _replaceStringArray(payment.remittanceInformation, request.remittanceInformation);
        _replaceStringArray(payment.bankInformation, request.bankInformation);

        _paymentIdByPaymentOrderId[request.paymentOrderId] = paymentId;

        emit PaymentCreated(
            paymentId,
            request.paymentOrderId,
            request.invoiceId,
            request.customerRefNumber,
            request.instructedAmountMinor,
            request.instructedCurrency
        );
    }

    function acceptPayment(TopazTypes.PaymentAcceptance calldata acceptance)
        external
        onlyRole(PAYMENT_OPERATOR_ROLE)
    {
        _validateAcceptance(acceptance);

        PaymentRecord storage payment = _requirePayment(acceptance.paymentId);
        if (payment.status != PaymentStatus.CREATED) {
            revert InvalidState("payment is not awaiting acceptance");
        }

        payment.settlementBankRef = acceptance.settlementBankRef;
        payment.settlementDate = acceptance.settlementDate;
        payment.status = PaymentStatus.ACCEPTED;
        payment.updatedAt = _timestamp();

        emit PaymentAccepted(acceptance.paymentId, payment.paymentOrderId, acceptance.settlementBankRef);
    }

    function rejectPayment(TopazTypes.PaymentRejection calldata rejection)
        external
        onlyRole(PAYMENT_OPERATOR_ROLE)
    {
        _validateRejection(rejection);

        PaymentRecord storage payment = _requirePayment(rejection.paymentId);
        if (payment.status != PaymentStatus.CREATED) {
            revert InvalidState("payment is not awaiting rejection");
        }

        payment.rejectCode = rejection.rejectCode;
        payment.rejectReason = rejection.rejectReason;
        payment.rejectDate = rejection.rejectDate;
        payment.status = PaymentStatus.REJECTED;
        payment.updatedAt = _timestamp();

        emit PaymentRejected(rejection.paymentId, payment.paymentOrderId, rejection.rejectCode, rejection.rejectReason);
    }

    function createPaymentReceipt(TopazTypes.PaymentReceiptRequest calldata request)
        external
        onlyRole(PAYMENT_OPERATOR_ROLE)
        returns (uint256 paymentReceiptId)
    {
        _validateReceiptRequest(request);

        PaymentRecord storage payment = _requirePayment(request.paymentId);
        if (payment.status != PaymentStatus.ACCEPTED && payment.status != PaymentStatus.REJECTED) {
            revert InvalidState("payment must be accepted or rejected before receipt creation");
        }
        if (_paymentReceiptIdByPaymentId[request.paymentId] != 0) {
            revert DuplicatePaymentReceipt(request.paymentId);
        }

        paymentReceiptId = _nextPaymentReceiptId++;
        PaymentReceiptRecord storage receipt = _paymentReceipts[paymentReceiptId];
        receipt.exists = true;
        receipt.paymentReceiptId = paymentReceiptId;
        receipt.paymentId = request.paymentId;
        receipt.transactionRefNum = request.transactionRefNum;
        receipt.relatedReference = request.relatedReference;
        receipt.orderingCustomer = request.orderingCustomer;
        receipt.orderingInstitution = request.orderingInstitution;
        receipt.remittanceInfo = request.remittanceInfo;
        receipt.valueDate = request.valueDate;
        receipt.createdAt = _timestamp();

        payment.paymentReceiptId = paymentReceiptId;
        payment.updatedAt = receipt.createdAt;
        _paymentReceiptIdByPaymentId[request.paymentId] = paymentReceiptId;

        emit PaymentReceiptCreated(paymentReceiptId, request.paymentId, payment.paymentOrderId, request.transactionRefNum);
    }

    function getPaymentIdByPaymentOrderId(uint256 paymentOrderId) external view returns (uint256) {
        return _paymentIdByPaymentOrderId[paymentOrderId];
    }

    function getPaymentSummary(uint256 paymentId)
        external
        view
        returns (
            uint256 paymentOrderId,
            uint256 invoiceId,
            PaymentStatus status,
            string memory customerRefNumber,
            uint256 instructedAmountMinor,
            string memory instructedCurrency,
            string memory settlementBankRef,
            string memory rejectCode,
            string memory rejectReason,
            uint256 paymentReceiptId
        )
    {
        PaymentRecord storage payment = _requirePayment(paymentId);
        return (
            payment.paymentOrderId,
            payment.invoiceId,
            payment.status,
            payment.customerRefNumber,
            payment.instructedAmountMinor,
            payment.instructedCurrency,
            payment.settlementBankRef,
            payment.rejectCode,
            payment.rejectReason,
            payment.paymentReceiptId
        );
    }

    function getPaymentReceiptIdByPaymentId(uint256 paymentId) external view returns (uint256) {
        return _paymentReceiptIdByPaymentId[paymentId];
    }

    function getPaymentReceiptSummary(uint256 paymentReceiptId)
        external
        view
        returns (
            uint256 paymentId,
            string memory transactionRefNum,
            string memory relatedReference,
            string memory orderingCustomer,
            string memory orderingInstitution,
            string memory remittanceInfo,
            string memory valueDate
        )
    {
        PaymentReceiptRecord storage receipt = _requirePaymentReceipt(paymentReceiptId);
        return (
            receipt.paymentId,
            receipt.transactionRefNum,
            receipt.relatedReference,
            receipt.orderingCustomer,
            receipt.orderingInstitution,
            receipt.remittanceInfo,
            receipt.valueDate
        );
    }

    function _requirePayment(uint256 paymentId)
        internal
        view
        returns (PaymentRecord storage payment)
    {
        payment = _payments[paymentId];
        if (!payment.exists) {
            revert UnknownPayment(paymentId);
        }
    }

    function _requirePaymentReceipt(uint256 paymentReceiptId)
        internal
        view
        returns (PaymentReceiptRecord storage receipt)
    {
        receipt = _paymentReceipts[paymentReceiptId];
        if (!receipt.exists) {
            revert UnknownPaymentReceipt(paymentReceiptId);
        }
    }

    function _validateRequest(TopazTypes.PaymentRequest calldata request) internal pure {
        if (request.paymentOrderId == 0) {
            revert InvalidInput("paymentOrderId is required");
        }
        if (request.invoiceId == 0) {
            revert InvalidInput("invoiceId is required");
        }
        if (request.payer.wallet == address(0)) {
            revert InvalidInput("payer wallet is required");
        }
        if (request.payee.wallet == address(0)) {
            revert InvalidInput("payee wallet is required");
        }
        if (request.instructedAmountMinor == 0) {
            revert InvalidInput("instructedAmountMinor must be positive");
        }
        if (bytes(request.customerRefNumber).length == 0) {
            revert InvalidInput("customerRefNumber is required");
        }
        if (bytes(request.instructedCurrency).length == 0) {
            revert InvalidInput("instructedCurrency is required");
        }
    }

    function _validateAcceptance(TopazTypes.PaymentAcceptance calldata acceptance) internal pure {
        if (acceptance.paymentId == 0) {
            revert InvalidInput("paymentId is required");
        }
        if (bytes(acceptance.settlementBankRef).length == 0) {
            revert InvalidInput("settlementBankRef is required");
        }
        if (acceptance.settlementDate == 0) {
            revert InvalidInput("settlementDate is required");
        }
    }

    function _validateRejection(TopazTypes.PaymentRejection calldata rejection) internal pure {
        if (rejection.paymentId == 0) {
            revert InvalidInput("paymentId is required");
        }
        if (bytes(rejection.rejectCode).length == 0) {
            revert InvalidInput("rejectCode is required");
        }
        if (bytes(rejection.rejectReason).length == 0) {
            revert InvalidInput("rejectReason is required");
        }
        if (rejection.rejectDate == 0) {
            revert InvalidInput("rejectDate is required");
        }
    }

    function _validateReceiptRequest(TopazTypes.PaymentReceiptRequest calldata request) internal pure {
        if (request.paymentId == 0) {
            revert InvalidInput("paymentId is required");
        }
        if (bytes(request.transactionRefNum).length == 0) {
            revert InvalidInput("transactionRefNum is required");
        }
        if (bytes(request.relatedReference).length == 0) {
            revert InvalidInput("relatedReference is required");
        }
        if (bytes(request.orderingCustomer).length == 0) {
            revert InvalidInput("orderingCustomer is required");
        }
        if (bytes(request.orderingInstitution).length == 0) {
            revert InvalidInput("orderingInstitution is required");
        }
        if (bytes(request.valueDate).length == 0) {
            revert InvalidInput("valueDate is required");
        }
    }

    function _copyParticipant(TopazTypes.Participant storage target, TopazTypes.Participant calldata source) internal {
        target.wallet = source.wallet;
        target.legalName = source.legalName;
        target.addressLine1 = source.addressLine1;
        target.addressLine2 = source.addressLine2;
        target.bic = source.bic;
        target.lei = source.lei;
        target.externalRef = source.externalRef;
    }

    function _copyAccountInfo(TopazTypes.AccountInfo storage target, TopazTypes.AccountInfo calldata source) internal {
        target.accountName = source.accountName;
        target.accountNumber = source.accountNumber;
        target.addressLine1 = source.addressLine1;
        target.addressLine2 = source.addressLine2;
        target.bic = source.bic;
        target.ultimateName = source.ultimateName;
    }

    function _replaceStringArray(string[] storage target, string[] calldata source) internal {
        _clearStringArray(target);
        for (uint256 i = 0; i < source.length; i++) {
            target.push(source[i]);
        }
    }

    function _clearStringArray(string[] storage target) internal {
        while (target.length > 0) {
            target.pop();
        }
    }

    function _timestamp() internal view returns (uint64) {
        return uint64(block.timestamp);
    }
}
