package com.demo.server.epmigration.chain.generated;

import io.reactivex.Flowable;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;
import org.web3j.abi.EventEncoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.Bool;
import org.web3j.abi.datatypes.DynamicArray;
import org.web3j.abi.datatypes.DynamicStruct;
import org.web3j.abi.datatypes.Event;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.Type;
import org.web3j.abi.datatypes.Utf8String;
import org.web3j.abi.datatypes.generated.Bytes32;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.abi.datatypes.generated.Uint64;
import org.web3j.abi.datatypes.generated.Uint8;
import org.web3j.abi.datatypes.reflection.Parameterized;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameter;
import org.web3j.protocol.core.RemoteFunctionCall;
import org.web3j.protocol.core.methods.request.EthFilter;
import org.web3j.protocol.core.methods.response.BaseEventResponse;
import org.web3j.protocol.core.methods.response.Log;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.tuples.generated.Tuple10;
import org.web3j.tuples.generated.Tuple11;
import org.web3j.tuples.generated.Tuple2;
import org.web3j.tuples.generated.Tuple9;
import org.web3j.tx.Contract;
import org.web3j.tx.TransactionManager;
import org.web3j.tx.gas.ContractGasProvider;

/**
 * <p>Auto generated code.
 * <p><strong>Do not modify!</strong>
 * <p>Please use the <a href="https://docs.web3j.io/command_line.html">web3j command line tools</a>,
 * or the org.web3j.codegen.SolidityFunctionWrapperGenerator in the
 * <a href="https://github.com/web3j/web3j/tree/master/codegen">codegen module</a> to update.
 *
 * <p>Generated with web3j version 4.9.8.
 */
@SuppressWarnings("rawtypes")
public class TopazLifecycle extends Contract {
    public static final String BINARY = "Bin file was not provided";

    public static final String FUNC_ADMIN_ROLE = "ADMIN_ROLE";

    public static final String FUNC_DEFAULT_ADMIN_ROLE = "DEFAULT_ADMIN_ROLE";

    public static final String FUNC_FINANCE_ROLE = "FINANCE_ROLE";

    public static final String FUNC_PROJECT_OFFICER_ROLE = "PROJECT_OFFICER_ROLE";

    public static final String FUNC_SETTLEMENT_BANK_ROLE = "SETTLEMENT_BANK_ROLE";

    public static final String FUNC_SUPER_ADMIN_ROLE = "SUPER_ADMIN_ROLE";

    public static final String FUNC_APPROVEPAYMENTORDER = "approvePaymentOrder";

    public static final String FUNC_CLAIMAPPROVERAPPROVE = "claimApproverApprove";

    public static final String FUNC_CLAIMAPPROVERREJECT = "claimApproverReject";

    public static final String FUNC_CREATEPAYMENTORDER = "createPaymentOrder";

    public static final String FUNC_CREATEPROJECT = "createProject";

    public static final String FUNC_DELETECLAIM = "deleteClaim";

    public static final String FUNC_DELETEINVOICE = "deleteInvoice";

    public static final String FUNC_DELETEPROJECT = "deleteProject";

    public static final String FUNC_DISCARDCLAIM = "discardClaim";

    public static final String FUNC_DISCARDINVOICE = "discardInvoice";

    public static final String FUNC_FINANCEAPPROVEINVOICE = "financeApproveInvoice";

    public static final String FUNC_FINANCEREJECTINVOICE = "financeRejectInvoice";

    public static final String FUNC_GETCLAIMAPPROVER = "getClaimApprover";

    public static final String FUNC_GETCLAIMAPPROVERCOUNT = "getClaimApproverCount";

    public static final String FUNC_GETCLAIMDOCUMENT = "getClaimDocument";

    public static final String FUNC_GETCLAIMINVOICEIDS = "getClaimInvoiceIds";

    public static final String FUNC_GETCLAIMSUMMARY = "getClaimSummary";

    public static final String FUNC_GETINVOICEDOCUMENT = "getInvoiceDocument";

    public static final String FUNC_GETINVOICEPAYMENTORDERIDS = "getInvoicePaymentOrderIds";

    public static final String FUNC_GETINVOICESUMMARY = "getInvoiceSummary";

    public static final String FUNC_GETPAYMENTORDERAPPROVER = "getPaymentOrderApprover";

    public static final String FUNC_GETPAYMENTORDERAPPROVERCOUNT = "getPaymentOrderApproverCount";

    public static final String FUNC_GETPAYMENTORDERPAYMENTID = "getPaymentOrderPaymentId";

    public static final String FUNC_GETPAYMENTORDERSUMMARY = "getPaymentOrderSummary";

    public static final String FUNC_GETPROJECTCLAIMIDS = "getProjectClaimIds";

    public static final String FUNC_GETPROJECTPAYMENTAPPROVERCOUNT = "getProjectPaymentApproverCount";

    public static final String FUNC_GETPROJECTSUMMARY = "getProjectSummary";

    public static final String FUNC_GETROLEADMIN = "getRoleAdmin";

    public static final String FUNC_GRANTROLE = "grantRole";

    public static final String FUNC_HASROLE = "hasRole";

    public static final String FUNC_PROJECTOFFICERAPPROVECLAIM = "projectOfficerApproveClaim";

    public static final String FUNC_PROJECTOFFICERAPPROVEINVOICE = "projectOfficerApproveInvoice";

    public static final String FUNC_PROJECTOFFICERREJECTCLAIM = "projectOfficerRejectClaim";

    public static final String FUNC_PROJECTOFFICERREJECTINVOICE = "projectOfficerRejectInvoice";

    public static final String FUNC_RECORDBANKPAYMENTREFERENCE = "recordBankPaymentReference";

    public static final String FUNC_REJECTPAYMENTORDER = "rejectPaymentOrder";

    public static final String FUNC_REMOVEPROJECTAPPROVER = "removeProjectApprover";

    public static final String FUNC_RENOUNCEROLE = "renounceRole";

    public static final String FUNC_REQUESTPROJECTDELETION = "requestProjectDeletion";

    public static final String FUNC_RESUBMITCLAIM = "resubmitClaim";

    public static final String FUNC_RESUBMITPAYMENTORDER = "resubmitPaymentOrder";

    public static final String FUNC_REVOKEROLE = "revokeRole";

    public static final String FUNC_SUBMITCLAIM = "submitClaim";

    public static final String FUNC_SUBMITINVOICE = "submitInvoice";

    public static final String FUNC_SUPPORTSINTERFACE = "supportsInterface";

    public static final String FUNC_UPDATECLAIM = "updateClaim";

    public static final String FUNC_UPDATEINVOICE = "updateInvoice";

    public static final String FUNC_UPDATEPROJECT = "updateProject";

    public static final String FUNC_UPDATEPROJECTBANKACCOUNTS = "updateProjectBankAccounts";

    public static final Event BANKPAYMENTREFERENCERECORDED_EVENT = new Event("BankPaymentReferenceRecorded",
            Arrays.<TypeReference<?>>asList(new TypeReference<Uint256>(true) {}, new TypeReference<Utf8String>() {}));
    ;

    public static final Event BANKPAYMENTREQUESTED_EVENT = new Event("BankPaymentRequested",
            Arrays.<TypeReference<?>>asList(new TypeReference<Uint256>(true) {}, new TypeReference<Uint256>(true) {}, new TypeReference<Utf8String>() {}));
    ;

    public static final Event CLAIMCREATED_EVENT = new Event("ClaimCreated",
            Arrays.<TypeReference<?>>asList(new TypeReference<Uint256>(true) {}, new TypeReference<Uint256>(true) {}, new TypeReference<Address>(true) {}, new TypeReference<Uint8>() {}));
    ;

    public static final Event CLAIMDOCUMENTSUPDATED_EVENT = new Event("ClaimDocumentsUpdated",
            Arrays.<TypeReference<?>>asList(new TypeReference<Uint256>(true) {}, new TypeReference<Uint256>() {}));
    ;

    public static final Event CLAIMSTATUSCHANGED_EVENT = new Event("ClaimStatusChanged",
            Arrays.<TypeReference<?>>asList(new TypeReference<Uint256>(true) {}, new TypeReference<Uint8>() {}));
    ;

    public static final Event INVOICECREATED_EVENT = new Event("InvoiceCreated",
            Arrays.<TypeReference<?>>asList(new TypeReference<Uint256>(true) {}, new TypeReference<Uint256>(true) {}, new TypeReference<Uint8>() {}));
    ;

    public static final Event INVOICEDOCUMENTSUPDATED_EVENT = new Event("InvoiceDocumentsUpdated",
            Arrays.<TypeReference<?>>asList(new TypeReference<Uint256>(true) {}, new TypeReference<Uint256>() {}));
    ;

    public static final Event INVOICESTATUSCHANGED_EVENT = new Event("InvoiceStatusChanged",
            Arrays.<TypeReference<?>>asList(new TypeReference<Uint256>(true) {}, new TypeReference<Uint8>() {}));
    ;

    public static final Event PAYMENTCREATEDFORORDER_EVENT = new Event("PaymentCreatedForOrder",
            Arrays.<TypeReference<?>>asList(new TypeReference<Uint256>(true) {}, new TypeReference<Uint256>(true) {}, new TypeReference<Uint256>(true) {}));
    ;

    public static final Event PAYMENTORDERCREATED_EVENT = new Event("PaymentOrderCreated",
            Arrays.<TypeReference<?>>asList(new TypeReference<Uint256>(true) {}, new TypeReference<Uint256>(true) {}, new TypeReference<Uint8>() {}));
    ;

    public static final Event PAYMENTORDERSTATUSCHANGED_EVENT = new Event("PaymentOrderStatusChanged",
            Arrays.<TypeReference<?>>asList(new TypeReference<Uint256>(true) {}, new TypeReference<Uint8>() {}));
    ;

    public static final Event PROJECTAPPROVERREMOVED_EVENT = new Event("ProjectApproverRemoved",
            Arrays.<TypeReference<?>>asList(new TypeReference<Uint256>(true) {}, new TypeReference<Bytes32>(true) {}));
    ;

    public static final Event PROJECTCREATED_EVENT = new Event("ProjectCreated",
            Arrays.<TypeReference<?>>asList(new TypeReference<Uint256>(true) {}, new TypeReference<Utf8String>() {}, new TypeReference<Address>(true) {}));
    ;

    public static final Event PROJECTSTATUSCHANGED_EVENT = new Event("ProjectStatusChanged",
            Arrays.<TypeReference<?>>asList(new TypeReference<Uint256>(true) {}, new TypeReference<Uint8>() {}));
    ;

    public static final Event PROJECTUPDATED_EVENT = new Event("ProjectUpdated",
            Arrays.<TypeReference<?>>asList(new TypeReference<Uint256>(true) {}, new TypeReference<Utf8String>() {}));
    ;

    public static final Event ROLEADMINCHANGED_EVENT = new Event("RoleAdminChanged",
            Arrays.<TypeReference<?>>asList(new TypeReference<Bytes32>(true) {}, new TypeReference<Bytes32>(true) {}, new TypeReference<Bytes32>(true) {}));
    ;

    public static final Event ROLEGRANTED_EVENT = new Event("RoleGranted",
            Arrays.<TypeReference<?>>asList(new TypeReference<Bytes32>(true) {}, new TypeReference<Address>(true) {}, new TypeReference<Address>(true) {}));
    ;

    public static final Event ROLEREVOKED_EVENT = new Event("RoleRevoked",
            Arrays.<TypeReference<?>>asList(new TypeReference<Bytes32>(true) {}, new TypeReference<Address>(true) {}, new TypeReference<Address>(true) {}));
    ;

    @Deprecated
    protected TopazLifecycle(String contractAddress, Web3j web3j, Credentials credentials, BigInteger gasPrice, BigInteger gasLimit) {
        super(BINARY, contractAddress, web3j, credentials, gasPrice, gasLimit);
    }

    protected TopazLifecycle(String contractAddress, Web3j web3j, Credentials credentials, ContractGasProvider contractGasProvider) {
        super(BINARY, contractAddress, web3j, credentials, contractGasProvider);
    }

    @Deprecated
    protected TopazLifecycle(String contractAddress, Web3j web3j, TransactionManager transactionManager, BigInteger gasPrice, BigInteger gasLimit) {
        super(BINARY, contractAddress, web3j, transactionManager, gasPrice, gasLimit);
    }

    protected TopazLifecycle(String contractAddress, Web3j web3j, TransactionManager transactionManager, ContractGasProvider contractGasProvider) {
        super(BINARY, contractAddress, web3j, transactionManager, contractGasProvider);
    }

    public static List<BankPaymentReferenceRecordedEventResponse> getBankPaymentReferenceRecordedEvents(TransactionReceipt transactionReceipt) {
        List<Contract.EventValuesWithLog> valueList = staticExtractEventParametersWithLog(BANKPAYMENTREFERENCERECORDED_EVENT, transactionReceipt);
        ArrayList<BankPaymentReferenceRecordedEventResponse> responses = new ArrayList<BankPaymentReferenceRecordedEventResponse>(valueList.size());
        for (Contract.EventValuesWithLog eventValues : valueList) {
            BankPaymentReferenceRecordedEventResponse typedResponse = new BankPaymentReferenceRecordedEventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse.paymentOrderId = (BigInteger) eventValues.getIndexedValues().get(0).getValue();
            typedResponse.bankPaymentRef = (String) eventValues.getNonIndexedValues().get(0).getValue();
            responses.add(typedResponse);
        }
        return responses;
    }

    public static BankPaymentReferenceRecordedEventResponse getBankPaymentReferenceRecordedEventFromLog(Log log) {
        Contract.EventValuesWithLog eventValues = staticExtractEventParametersWithLog(BANKPAYMENTREFERENCERECORDED_EVENT, log);
        BankPaymentReferenceRecordedEventResponse typedResponse = new BankPaymentReferenceRecordedEventResponse();
        typedResponse.log = log;
        typedResponse.paymentOrderId = (BigInteger) eventValues.getIndexedValues().get(0).getValue();
        typedResponse.bankPaymentRef = (String) eventValues.getNonIndexedValues().get(0).getValue();
        return typedResponse;
    }

    public Flowable<BankPaymentReferenceRecordedEventResponse> bankPaymentReferenceRecordedEventFlowable(EthFilter filter) {
        return web3j.ethLogFlowable(filter).map(log -> getBankPaymentReferenceRecordedEventFromLog(log));
    }

    public Flowable<BankPaymentReferenceRecordedEventResponse> bankPaymentReferenceRecordedEventFlowable(DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(BANKPAYMENTREFERENCERECORDED_EVENT));
        return bankPaymentReferenceRecordedEventFlowable(filter);
    }

    public static List<BankPaymentRequestedEventResponse> getBankPaymentRequestedEvents(TransactionReceipt transactionReceipt) {
        List<Contract.EventValuesWithLog> valueList = staticExtractEventParametersWithLog(BANKPAYMENTREQUESTED_EVENT, transactionReceipt);
        ArrayList<BankPaymentRequestedEventResponse> responses = new ArrayList<BankPaymentRequestedEventResponse>(valueList.size());
        for (Contract.EventValuesWithLog eventValues : valueList) {
            BankPaymentRequestedEventResponse typedResponse = new BankPaymentRequestedEventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse.paymentOrderId = (BigInteger) eventValues.getIndexedValues().get(0).getValue();
            typedResponse.invoiceId = (BigInteger) eventValues.getIndexedValues().get(1).getValue();
            typedResponse.customerRefNumber = (String) eventValues.getNonIndexedValues().get(0).getValue();
            responses.add(typedResponse);
        }
        return responses;
    }

    public static BankPaymentRequestedEventResponse getBankPaymentRequestedEventFromLog(Log log) {
        Contract.EventValuesWithLog eventValues = staticExtractEventParametersWithLog(BANKPAYMENTREQUESTED_EVENT, log);
        BankPaymentRequestedEventResponse typedResponse = new BankPaymentRequestedEventResponse();
        typedResponse.log = log;
        typedResponse.paymentOrderId = (BigInteger) eventValues.getIndexedValues().get(0).getValue();
        typedResponse.invoiceId = (BigInteger) eventValues.getIndexedValues().get(1).getValue();
        typedResponse.customerRefNumber = (String) eventValues.getNonIndexedValues().get(0).getValue();
        return typedResponse;
    }

    public Flowable<BankPaymentRequestedEventResponse> bankPaymentRequestedEventFlowable(EthFilter filter) {
        return web3j.ethLogFlowable(filter).map(log -> getBankPaymentRequestedEventFromLog(log));
    }

    public Flowable<BankPaymentRequestedEventResponse> bankPaymentRequestedEventFlowable(DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(BANKPAYMENTREQUESTED_EVENT));
        return bankPaymentRequestedEventFlowable(filter);
    }

    public static List<ClaimCreatedEventResponse> getClaimCreatedEvents(TransactionReceipt transactionReceipt) {
        List<Contract.EventValuesWithLog> valueList = staticExtractEventParametersWithLog(CLAIMCREATED_EVENT, transactionReceipt);
        ArrayList<ClaimCreatedEventResponse> responses = new ArrayList<ClaimCreatedEventResponse>(valueList.size());
        for (Contract.EventValuesWithLog eventValues : valueList) {
            ClaimCreatedEventResponse typedResponse = new ClaimCreatedEventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse.claimId = (BigInteger) eventValues.getIndexedValues().get(0).getValue();
            typedResponse.projectId = (BigInteger) eventValues.getIndexedValues().get(1).getValue();
            typedResponse.contractorWallet = (String) eventValues.getIndexedValues().get(2).getValue();
            typedResponse.status = (BigInteger) eventValues.getNonIndexedValues().get(0).getValue();
            responses.add(typedResponse);
        }
        return responses;
    }

    public static ClaimCreatedEventResponse getClaimCreatedEventFromLog(Log log) {
        Contract.EventValuesWithLog eventValues = staticExtractEventParametersWithLog(CLAIMCREATED_EVENT, log);
        ClaimCreatedEventResponse typedResponse = new ClaimCreatedEventResponse();
        typedResponse.log = log;
        typedResponse.claimId = (BigInteger) eventValues.getIndexedValues().get(0).getValue();
        typedResponse.projectId = (BigInteger) eventValues.getIndexedValues().get(1).getValue();
        typedResponse.contractorWallet = (String) eventValues.getIndexedValues().get(2).getValue();
        typedResponse.status = (BigInteger) eventValues.getNonIndexedValues().get(0).getValue();
        return typedResponse;
    }

    public Flowable<ClaimCreatedEventResponse> claimCreatedEventFlowable(EthFilter filter) {
        return web3j.ethLogFlowable(filter).map(log -> getClaimCreatedEventFromLog(log));
    }

    public Flowable<ClaimCreatedEventResponse> claimCreatedEventFlowable(DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(CLAIMCREATED_EVENT));
        return claimCreatedEventFlowable(filter);
    }

    public static List<ClaimDocumentsUpdatedEventResponse> getClaimDocumentsUpdatedEvents(TransactionReceipt transactionReceipt) {
        List<Contract.EventValuesWithLog> valueList = staticExtractEventParametersWithLog(CLAIMDOCUMENTSUPDATED_EVENT, transactionReceipt);
        ArrayList<ClaimDocumentsUpdatedEventResponse> responses = new ArrayList<ClaimDocumentsUpdatedEventResponse>(valueList.size());
        for (Contract.EventValuesWithLog eventValues : valueList) {
            ClaimDocumentsUpdatedEventResponse typedResponse = new ClaimDocumentsUpdatedEventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse.claimId = (BigInteger) eventValues.getIndexedValues().get(0).getValue();
            typedResponse.documentCount = (BigInteger) eventValues.getNonIndexedValues().get(0).getValue();
            responses.add(typedResponse);
        }
        return responses;
    }

    public static ClaimDocumentsUpdatedEventResponse getClaimDocumentsUpdatedEventFromLog(Log log) {
        Contract.EventValuesWithLog eventValues = staticExtractEventParametersWithLog(CLAIMDOCUMENTSUPDATED_EVENT, log);
        ClaimDocumentsUpdatedEventResponse typedResponse = new ClaimDocumentsUpdatedEventResponse();
        typedResponse.log = log;
        typedResponse.claimId = (BigInteger) eventValues.getIndexedValues().get(0).getValue();
        typedResponse.documentCount = (BigInteger) eventValues.getNonIndexedValues().get(0).getValue();
        return typedResponse;
    }

    public Flowable<ClaimDocumentsUpdatedEventResponse> claimDocumentsUpdatedEventFlowable(EthFilter filter) {
        return web3j.ethLogFlowable(filter).map(log -> getClaimDocumentsUpdatedEventFromLog(log));
    }

    public Flowable<ClaimDocumentsUpdatedEventResponse> claimDocumentsUpdatedEventFlowable(DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(CLAIMDOCUMENTSUPDATED_EVENT));
        return claimDocumentsUpdatedEventFlowable(filter);
    }

    public static List<ClaimStatusChangedEventResponse> getClaimStatusChangedEvents(TransactionReceipt transactionReceipt) {
        List<Contract.EventValuesWithLog> valueList = staticExtractEventParametersWithLog(CLAIMSTATUSCHANGED_EVENT, transactionReceipt);
        ArrayList<ClaimStatusChangedEventResponse> responses = new ArrayList<ClaimStatusChangedEventResponse>(valueList.size());
        for (Contract.EventValuesWithLog eventValues : valueList) {
            ClaimStatusChangedEventResponse typedResponse = new ClaimStatusChangedEventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse.claimId = (BigInteger) eventValues.getIndexedValues().get(0).getValue();
            typedResponse.status = (BigInteger) eventValues.getNonIndexedValues().get(0).getValue();
            responses.add(typedResponse);
        }
        return responses;
    }

    public static ClaimStatusChangedEventResponse getClaimStatusChangedEventFromLog(Log log) {
        Contract.EventValuesWithLog eventValues = staticExtractEventParametersWithLog(CLAIMSTATUSCHANGED_EVENT, log);
        ClaimStatusChangedEventResponse typedResponse = new ClaimStatusChangedEventResponse();
        typedResponse.log = log;
        typedResponse.claimId = (BigInteger) eventValues.getIndexedValues().get(0).getValue();
        typedResponse.status = (BigInteger) eventValues.getNonIndexedValues().get(0).getValue();
        return typedResponse;
    }

    public Flowable<ClaimStatusChangedEventResponse> claimStatusChangedEventFlowable(EthFilter filter) {
        return web3j.ethLogFlowable(filter).map(log -> getClaimStatusChangedEventFromLog(log));
    }

    public Flowable<ClaimStatusChangedEventResponse> claimStatusChangedEventFlowable(DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(CLAIMSTATUSCHANGED_EVENT));
        return claimStatusChangedEventFlowable(filter);
    }

    public static List<InvoiceCreatedEventResponse> getInvoiceCreatedEvents(TransactionReceipt transactionReceipt) {
        List<Contract.EventValuesWithLog> valueList = staticExtractEventParametersWithLog(INVOICECREATED_EVENT, transactionReceipt);
        ArrayList<InvoiceCreatedEventResponse> responses = new ArrayList<InvoiceCreatedEventResponse>(valueList.size());
        for (Contract.EventValuesWithLog eventValues : valueList) {
            InvoiceCreatedEventResponse typedResponse = new InvoiceCreatedEventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse.invoiceId = (BigInteger) eventValues.getIndexedValues().get(0).getValue();
            typedResponse.claimId = (BigInteger) eventValues.getIndexedValues().get(1).getValue();
            typedResponse.status = (BigInteger) eventValues.getNonIndexedValues().get(0).getValue();
            responses.add(typedResponse);
        }
        return responses;
    }

    public static InvoiceCreatedEventResponse getInvoiceCreatedEventFromLog(Log log) {
        Contract.EventValuesWithLog eventValues = staticExtractEventParametersWithLog(INVOICECREATED_EVENT, log);
        InvoiceCreatedEventResponse typedResponse = new InvoiceCreatedEventResponse();
        typedResponse.log = log;
        typedResponse.invoiceId = (BigInteger) eventValues.getIndexedValues().get(0).getValue();
        typedResponse.claimId = (BigInteger) eventValues.getIndexedValues().get(1).getValue();
        typedResponse.status = (BigInteger) eventValues.getNonIndexedValues().get(0).getValue();
        return typedResponse;
    }

    public Flowable<InvoiceCreatedEventResponse> invoiceCreatedEventFlowable(EthFilter filter) {
        return web3j.ethLogFlowable(filter).map(log -> getInvoiceCreatedEventFromLog(log));
    }

    public Flowable<InvoiceCreatedEventResponse> invoiceCreatedEventFlowable(DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(INVOICECREATED_EVENT));
        return invoiceCreatedEventFlowable(filter);
    }

    public static List<InvoiceDocumentsUpdatedEventResponse> getInvoiceDocumentsUpdatedEvents(TransactionReceipt transactionReceipt) {
        List<Contract.EventValuesWithLog> valueList = staticExtractEventParametersWithLog(INVOICEDOCUMENTSUPDATED_EVENT, transactionReceipt);
        ArrayList<InvoiceDocumentsUpdatedEventResponse> responses = new ArrayList<InvoiceDocumentsUpdatedEventResponse>(valueList.size());
        for (Contract.EventValuesWithLog eventValues : valueList) {
            InvoiceDocumentsUpdatedEventResponse typedResponse = new InvoiceDocumentsUpdatedEventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse.invoiceId = (BigInteger) eventValues.getIndexedValues().get(0).getValue();
            typedResponse.documentCount = (BigInteger) eventValues.getNonIndexedValues().get(0).getValue();
            responses.add(typedResponse);
        }
        return responses;
    }

    public static InvoiceDocumentsUpdatedEventResponse getInvoiceDocumentsUpdatedEventFromLog(Log log) {
        Contract.EventValuesWithLog eventValues = staticExtractEventParametersWithLog(INVOICEDOCUMENTSUPDATED_EVENT, log);
        InvoiceDocumentsUpdatedEventResponse typedResponse = new InvoiceDocumentsUpdatedEventResponse();
        typedResponse.log = log;
        typedResponse.invoiceId = (BigInteger) eventValues.getIndexedValues().get(0).getValue();
        typedResponse.documentCount = (BigInteger) eventValues.getNonIndexedValues().get(0).getValue();
        return typedResponse;
    }

    public Flowable<InvoiceDocumentsUpdatedEventResponse> invoiceDocumentsUpdatedEventFlowable(EthFilter filter) {
        return web3j.ethLogFlowable(filter).map(log -> getInvoiceDocumentsUpdatedEventFromLog(log));
    }

    public Flowable<InvoiceDocumentsUpdatedEventResponse> invoiceDocumentsUpdatedEventFlowable(DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(INVOICEDOCUMENTSUPDATED_EVENT));
        return invoiceDocumentsUpdatedEventFlowable(filter);
    }

    public static List<InvoiceStatusChangedEventResponse> getInvoiceStatusChangedEvents(TransactionReceipt transactionReceipt) {
        List<Contract.EventValuesWithLog> valueList = staticExtractEventParametersWithLog(INVOICESTATUSCHANGED_EVENT, transactionReceipt);
        ArrayList<InvoiceStatusChangedEventResponse> responses = new ArrayList<InvoiceStatusChangedEventResponse>(valueList.size());
        for (Contract.EventValuesWithLog eventValues : valueList) {
            InvoiceStatusChangedEventResponse typedResponse = new InvoiceStatusChangedEventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse.invoiceId = (BigInteger) eventValues.getIndexedValues().get(0).getValue();
            typedResponse.status = (BigInteger) eventValues.getNonIndexedValues().get(0).getValue();
            responses.add(typedResponse);
        }
        return responses;
    }

    public static InvoiceStatusChangedEventResponse getInvoiceStatusChangedEventFromLog(Log log) {
        Contract.EventValuesWithLog eventValues = staticExtractEventParametersWithLog(INVOICESTATUSCHANGED_EVENT, log);
        InvoiceStatusChangedEventResponse typedResponse = new InvoiceStatusChangedEventResponse();
        typedResponse.log = log;
        typedResponse.invoiceId = (BigInteger) eventValues.getIndexedValues().get(0).getValue();
        typedResponse.status = (BigInteger) eventValues.getNonIndexedValues().get(0).getValue();
        return typedResponse;
    }

    public Flowable<InvoiceStatusChangedEventResponse> invoiceStatusChangedEventFlowable(EthFilter filter) {
        return web3j.ethLogFlowable(filter).map(log -> getInvoiceStatusChangedEventFromLog(log));
    }

    public Flowable<InvoiceStatusChangedEventResponse> invoiceStatusChangedEventFlowable(DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(INVOICESTATUSCHANGED_EVENT));
        return invoiceStatusChangedEventFlowable(filter);
    }

    public static List<PaymentCreatedForOrderEventResponse> getPaymentCreatedForOrderEvents(TransactionReceipt transactionReceipt) {
        List<Contract.EventValuesWithLog> valueList = staticExtractEventParametersWithLog(PAYMENTCREATEDFORORDER_EVENT, transactionReceipt);
        ArrayList<PaymentCreatedForOrderEventResponse> responses = new ArrayList<PaymentCreatedForOrderEventResponse>(valueList.size());
        for (Contract.EventValuesWithLog eventValues : valueList) {
            PaymentCreatedForOrderEventResponse typedResponse = new PaymentCreatedForOrderEventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse.paymentOrderId = (BigInteger) eventValues.getIndexedValues().get(0).getValue();
            typedResponse.paymentId = (BigInteger) eventValues.getIndexedValues().get(1).getValue();
            typedResponse.invoiceId = (BigInteger) eventValues.getIndexedValues().get(2).getValue();
            responses.add(typedResponse);
        }
        return responses;
    }

    public static PaymentCreatedForOrderEventResponse getPaymentCreatedForOrderEventFromLog(Log log) {
        Contract.EventValuesWithLog eventValues = staticExtractEventParametersWithLog(PAYMENTCREATEDFORORDER_EVENT, log);
        PaymentCreatedForOrderEventResponse typedResponse = new PaymentCreatedForOrderEventResponse();
        typedResponse.log = log;
        typedResponse.paymentOrderId = (BigInteger) eventValues.getIndexedValues().get(0).getValue();
        typedResponse.paymentId = (BigInteger) eventValues.getIndexedValues().get(1).getValue();
        typedResponse.invoiceId = (BigInteger) eventValues.getIndexedValues().get(2).getValue();
        return typedResponse;
    }

    public Flowable<PaymentCreatedForOrderEventResponse> paymentCreatedForOrderEventFlowable(EthFilter filter) {
        return web3j.ethLogFlowable(filter).map(log -> getPaymentCreatedForOrderEventFromLog(log));
    }

    public Flowable<PaymentCreatedForOrderEventResponse> paymentCreatedForOrderEventFlowable(DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(PAYMENTCREATEDFORORDER_EVENT));
        return paymentCreatedForOrderEventFlowable(filter);
    }

    public static List<PaymentOrderCreatedEventResponse> getPaymentOrderCreatedEvents(TransactionReceipt transactionReceipt) {
        List<Contract.EventValuesWithLog> valueList = staticExtractEventParametersWithLog(PAYMENTORDERCREATED_EVENT, transactionReceipt);
        ArrayList<PaymentOrderCreatedEventResponse> responses = new ArrayList<PaymentOrderCreatedEventResponse>(valueList.size());
        for (Contract.EventValuesWithLog eventValues : valueList) {
            PaymentOrderCreatedEventResponse typedResponse = new PaymentOrderCreatedEventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse.paymentOrderId = (BigInteger) eventValues.getIndexedValues().get(0).getValue();
            typedResponse.invoiceId = (BigInteger) eventValues.getIndexedValues().get(1).getValue();
            typedResponse.status = (BigInteger) eventValues.getNonIndexedValues().get(0).getValue();
            responses.add(typedResponse);
        }
        return responses;
    }

    public static PaymentOrderCreatedEventResponse getPaymentOrderCreatedEventFromLog(Log log) {
        Contract.EventValuesWithLog eventValues = staticExtractEventParametersWithLog(PAYMENTORDERCREATED_EVENT, log);
        PaymentOrderCreatedEventResponse typedResponse = new PaymentOrderCreatedEventResponse();
        typedResponse.log = log;
        typedResponse.paymentOrderId = (BigInteger) eventValues.getIndexedValues().get(0).getValue();
        typedResponse.invoiceId = (BigInteger) eventValues.getIndexedValues().get(1).getValue();
        typedResponse.status = (BigInteger) eventValues.getNonIndexedValues().get(0).getValue();
        return typedResponse;
    }

    public Flowable<PaymentOrderCreatedEventResponse> paymentOrderCreatedEventFlowable(EthFilter filter) {
        return web3j.ethLogFlowable(filter).map(log -> getPaymentOrderCreatedEventFromLog(log));
    }

    public Flowable<PaymentOrderCreatedEventResponse> paymentOrderCreatedEventFlowable(DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(PAYMENTORDERCREATED_EVENT));
        return paymentOrderCreatedEventFlowable(filter);
    }

    public static List<PaymentOrderStatusChangedEventResponse> getPaymentOrderStatusChangedEvents(TransactionReceipt transactionReceipt) {
        List<Contract.EventValuesWithLog> valueList = staticExtractEventParametersWithLog(PAYMENTORDERSTATUSCHANGED_EVENT, transactionReceipt);
        ArrayList<PaymentOrderStatusChangedEventResponse> responses = new ArrayList<PaymentOrderStatusChangedEventResponse>(valueList.size());
        for (Contract.EventValuesWithLog eventValues : valueList) {
            PaymentOrderStatusChangedEventResponse typedResponse = new PaymentOrderStatusChangedEventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse.paymentOrderId = (BigInteger) eventValues.getIndexedValues().get(0).getValue();
            typedResponse.status = (BigInteger) eventValues.getNonIndexedValues().get(0).getValue();
            responses.add(typedResponse);
        }
        return responses;
    }

    public static PaymentOrderStatusChangedEventResponse getPaymentOrderStatusChangedEventFromLog(Log log) {
        Contract.EventValuesWithLog eventValues = staticExtractEventParametersWithLog(PAYMENTORDERSTATUSCHANGED_EVENT, log);
        PaymentOrderStatusChangedEventResponse typedResponse = new PaymentOrderStatusChangedEventResponse();
        typedResponse.log = log;
        typedResponse.paymentOrderId = (BigInteger) eventValues.getIndexedValues().get(0).getValue();
        typedResponse.status = (BigInteger) eventValues.getNonIndexedValues().get(0).getValue();
        return typedResponse;
    }

    public Flowable<PaymentOrderStatusChangedEventResponse> paymentOrderStatusChangedEventFlowable(EthFilter filter) {
        return web3j.ethLogFlowable(filter).map(log -> getPaymentOrderStatusChangedEventFromLog(log));
    }

    public Flowable<PaymentOrderStatusChangedEventResponse> paymentOrderStatusChangedEventFlowable(DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(PAYMENTORDERSTATUSCHANGED_EVENT));
        return paymentOrderStatusChangedEventFlowable(filter);
    }

    public static List<ProjectApproverRemovedEventResponse> getProjectApproverRemovedEvents(TransactionReceipt transactionReceipt) {
        List<Contract.EventValuesWithLog> valueList = staticExtractEventParametersWithLog(PROJECTAPPROVERREMOVED_EVENT, transactionReceipt);
        ArrayList<ProjectApproverRemovedEventResponse> responses = new ArrayList<ProjectApproverRemovedEventResponse>(valueList.size());
        for (Contract.EventValuesWithLog eventValues : valueList) {
            ProjectApproverRemovedEventResponse typedResponse = new ProjectApproverRemovedEventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse.projectId = (BigInteger) eventValues.getIndexedValues().get(0).getValue();
            typedResponse.userHash = (byte[]) eventValues.getIndexedValues().get(1).getValue();
            responses.add(typedResponse);
        }
        return responses;
    }

    public static ProjectApproverRemovedEventResponse getProjectApproverRemovedEventFromLog(Log log) {
        Contract.EventValuesWithLog eventValues = staticExtractEventParametersWithLog(PROJECTAPPROVERREMOVED_EVENT, log);
        ProjectApproverRemovedEventResponse typedResponse = new ProjectApproverRemovedEventResponse();
        typedResponse.log = log;
        typedResponse.projectId = (BigInteger) eventValues.getIndexedValues().get(0).getValue();
        typedResponse.userHash = (byte[]) eventValues.getIndexedValues().get(1).getValue();
        return typedResponse;
    }

    public Flowable<ProjectApproverRemovedEventResponse> projectApproverRemovedEventFlowable(EthFilter filter) {
        return web3j.ethLogFlowable(filter).map(log -> getProjectApproverRemovedEventFromLog(log));
    }

    public Flowable<ProjectApproverRemovedEventResponse> projectApproverRemovedEventFlowable(DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(PROJECTAPPROVERREMOVED_EVENT));
        return projectApproverRemovedEventFlowable(filter);
    }

    public static List<ProjectCreatedEventResponse> getProjectCreatedEvents(TransactionReceipt transactionReceipt) {
        List<Contract.EventValuesWithLog> valueList = staticExtractEventParametersWithLog(PROJECTCREATED_EVENT, transactionReceipt);
        ArrayList<ProjectCreatedEventResponse> responses = new ArrayList<ProjectCreatedEventResponse>(valueList.size());
        for (Contract.EventValuesWithLog eventValues : valueList) {
            ProjectCreatedEventResponse typedResponse = new ProjectCreatedEventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse.projectId = (BigInteger) eventValues.getIndexedValues().get(0).getValue();
            typedResponse.developerWallet = (String) eventValues.getIndexedValues().get(1).getValue();
            typedResponse.externalProjectId = (String) eventValues.getNonIndexedValues().get(0).getValue();
            responses.add(typedResponse);
        }
        return responses;
    }

    public static ProjectCreatedEventResponse getProjectCreatedEventFromLog(Log log) {
        Contract.EventValuesWithLog eventValues = staticExtractEventParametersWithLog(PROJECTCREATED_EVENT, log);
        ProjectCreatedEventResponse typedResponse = new ProjectCreatedEventResponse();
        typedResponse.log = log;
        typedResponse.projectId = (BigInteger) eventValues.getIndexedValues().get(0).getValue();
        typedResponse.developerWallet = (String) eventValues.getIndexedValues().get(1).getValue();
        typedResponse.externalProjectId = (String) eventValues.getNonIndexedValues().get(0).getValue();
        return typedResponse;
    }

    public Flowable<ProjectCreatedEventResponse> projectCreatedEventFlowable(EthFilter filter) {
        return web3j.ethLogFlowable(filter).map(log -> getProjectCreatedEventFromLog(log));
    }

    public Flowable<ProjectCreatedEventResponse> projectCreatedEventFlowable(DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(PROJECTCREATED_EVENT));
        return projectCreatedEventFlowable(filter);
    }

    public static List<ProjectStatusChangedEventResponse> getProjectStatusChangedEvents(TransactionReceipt transactionReceipt) {
        List<Contract.EventValuesWithLog> valueList = staticExtractEventParametersWithLog(PROJECTSTATUSCHANGED_EVENT, transactionReceipt);
        ArrayList<ProjectStatusChangedEventResponse> responses = new ArrayList<ProjectStatusChangedEventResponse>(valueList.size());
        for (Contract.EventValuesWithLog eventValues : valueList) {
            ProjectStatusChangedEventResponse typedResponse = new ProjectStatusChangedEventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse.projectId = (BigInteger) eventValues.getIndexedValues().get(0).getValue();
            typedResponse.status = (BigInteger) eventValues.getNonIndexedValues().get(0).getValue();
            responses.add(typedResponse);
        }
        return responses;
    }

    public static ProjectStatusChangedEventResponse getProjectStatusChangedEventFromLog(Log log) {
        Contract.EventValuesWithLog eventValues = staticExtractEventParametersWithLog(PROJECTSTATUSCHANGED_EVENT, log);
        ProjectStatusChangedEventResponse typedResponse = new ProjectStatusChangedEventResponse();
        typedResponse.log = log;
        typedResponse.projectId = (BigInteger) eventValues.getIndexedValues().get(0).getValue();
        typedResponse.status = (BigInteger) eventValues.getNonIndexedValues().get(0).getValue();
        return typedResponse;
    }

    public Flowable<ProjectStatusChangedEventResponse> projectStatusChangedEventFlowable(EthFilter filter) {
        return web3j.ethLogFlowable(filter).map(log -> getProjectStatusChangedEventFromLog(log));
    }

    public Flowable<ProjectStatusChangedEventResponse> projectStatusChangedEventFlowable(DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(PROJECTSTATUSCHANGED_EVENT));
        return projectStatusChangedEventFlowable(filter);
    }

    public static List<ProjectUpdatedEventResponse> getProjectUpdatedEvents(TransactionReceipt transactionReceipt) {
        List<Contract.EventValuesWithLog> valueList = staticExtractEventParametersWithLog(PROJECTUPDATED_EVENT, transactionReceipt);
        ArrayList<ProjectUpdatedEventResponse> responses = new ArrayList<ProjectUpdatedEventResponse>(valueList.size());
        for (Contract.EventValuesWithLog eventValues : valueList) {
            ProjectUpdatedEventResponse typedResponse = new ProjectUpdatedEventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse.projectId = (BigInteger) eventValues.getIndexedValues().get(0).getValue();
            typedResponse.externalProjectId = (String) eventValues.getNonIndexedValues().get(0).getValue();
            responses.add(typedResponse);
        }
        return responses;
    }

    public static ProjectUpdatedEventResponse getProjectUpdatedEventFromLog(Log log) {
        Contract.EventValuesWithLog eventValues = staticExtractEventParametersWithLog(PROJECTUPDATED_EVENT, log);
        ProjectUpdatedEventResponse typedResponse = new ProjectUpdatedEventResponse();
        typedResponse.log = log;
        typedResponse.projectId = (BigInteger) eventValues.getIndexedValues().get(0).getValue();
        typedResponse.externalProjectId = (String) eventValues.getNonIndexedValues().get(0).getValue();
        return typedResponse;
    }

    public Flowable<ProjectUpdatedEventResponse> projectUpdatedEventFlowable(EthFilter filter) {
        return web3j.ethLogFlowable(filter).map(log -> getProjectUpdatedEventFromLog(log));
    }

    public Flowable<ProjectUpdatedEventResponse> projectUpdatedEventFlowable(DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(PROJECTUPDATED_EVENT));
        return projectUpdatedEventFlowable(filter);
    }

    public static List<RoleAdminChangedEventResponse> getRoleAdminChangedEvents(TransactionReceipt transactionReceipt) {
        List<Contract.EventValuesWithLog> valueList = staticExtractEventParametersWithLog(ROLEADMINCHANGED_EVENT, transactionReceipt);
        ArrayList<RoleAdminChangedEventResponse> responses = new ArrayList<RoleAdminChangedEventResponse>(valueList.size());
        for (Contract.EventValuesWithLog eventValues : valueList) {
            RoleAdminChangedEventResponse typedResponse = new RoleAdminChangedEventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse.role = (byte[]) eventValues.getIndexedValues().get(0).getValue();
            typedResponse.previousAdminRole = (byte[]) eventValues.getIndexedValues().get(1).getValue();
            typedResponse.newAdminRole = (byte[]) eventValues.getIndexedValues().get(2).getValue();
            responses.add(typedResponse);
        }
        return responses;
    }

    public static RoleAdminChangedEventResponse getRoleAdminChangedEventFromLog(Log log) {
        Contract.EventValuesWithLog eventValues = staticExtractEventParametersWithLog(ROLEADMINCHANGED_EVENT, log);
        RoleAdminChangedEventResponse typedResponse = new RoleAdminChangedEventResponse();
        typedResponse.log = log;
        typedResponse.role = (byte[]) eventValues.getIndexedValues().get(0).getValue();
        typedResponse.previousAdminRole = (byte[]) eventValues.getIndexedValues().get(1).getValue();
        typedResponse.newAdminRole = (byte[]) eventValues.getIndexedValues().get(2).getValue();
        return typedResponse;
    }

    public Flowable<RoleAdminChangedEventResponse> roleAdminChangedEventFlowable(EthFilter filter) {
        return web3j.ethLogFlowable(filter).map(log -> getRoleAdminChangedEventFromLog(log));
    }

    public Flowable<RoleAdminChangedEventResponse> roleAdminChangedEventFlowable(DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(ROLEADMINCHANGED_EVENT));
        return roleAdminChangedEventFlowable(filter);
    }

    public static List<RoleGrantedEventResponse> getRoleGrantedEvents(TransactionReceipt transactionReceipt) {
        List<Contract.EventValuesWithLog> valueList = staticExtractEventParametersWithLog(ROLEGRANTED_EVENT, transactionReceipt);
        ArrayList<RoleGrantedEventResponse> responses = new ArrayList<RoleGrantedEventResponse>(valueList.size());
        for (Contract.EventValuesWithLog eventValues : valueList) {
            RoleGrantedEventResponse typedResponse = new RoleGrantedEventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse.role = (byte[]) eventValues.getIndexedValues().get(0).getValue();
            typedResponse.account = (String) eventValues.getIndexedValues().get(1).getValue();
            typedResponse.sender = (String) eventValues.getIndexedValues().get(2).getValue();
            responses.add(typedResponse);
        }
        return responses;
    }

    public static RoleGrantedEventResponse getRoleGrantedEventFromLog(Log log) {
        Contract.EventValuesWithLog eventValues = staticExtractEventParametersWithLog(ROLEGRANTED_EVENT, log);
        RoleGrantedEventResponse typedResponse = new RoleGrantedEventResponse();
        typedResponse.log = log;
        typedResponse.role = (byte[]) eventValues.getIndexedValues().get(0).getValue();
        typedResponse.account = (String) eventValues.getIndexedValues().get(1).getValue();
        typedResponse.sender = (String) eventValues.getIndexedValues().get(2).getValue();
        return typedResponse;
    }

    public Flowable<RoleGrantedEventResponse> roleGrantedEventFlowable(EthFilter filter) {
        return web3j.ethLogFlowable(filter).map(log -> getRoleGrantedEventFromLog(log));
    }

    public Flowable<RoleGrantedEventResponse> roleGrantedEventFlowable(DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(ROLEGRANTED_EVENT));
        return roleGrantedEventFlowable(filter);
    }

    public static List<RoleRevokedEventResponse> getRoleRevokedEvents(TransactionReceipt transactionReceipt) {
        List<Contract.EventValuesWithLog> valueList = staticExtractEventParametersWithLog(ROLEREVOKED_EVENT, transactionReceipt);
        ArrayList<RoleRevokedEventResponse> responses = new ArrayList<RoleRevokedEventResponse>(valueList.size());
        for (Contract.EventValuesWithLog eventValues : valueList) {
            RoleRevokedEventResponse typedResponse = new RoleRevokedEventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse.role = (byte[]) eventValues.getIndexedValues().get(0).getValue();
            typedResponse.account = (String) eventValues.getIndexedValues().get(1).getValue();
            typedResponse.sender = (String) eventValues.getIndexedValues().get(2).getValue();
            responses.add(typedResponse);
        }
        return responses;
    }

    public static RoleRevokedEventResponse getRoleRevokedEventFromLog(Log log) {
        Contract.EventValuesWithLog eventValues = staticExtractEventParametersWithLog(ROLEREVOKED_EVENT, log);
        RoleRevokedEventResponse typedResponse = new RoleRevokedEventResponse();
        typedResponse.log = log;
        typedResponse.role = (byte[]) eventValues.getIndexedValues().get(0).getValue();
        typedResponse.account = (String) eventValues.getIndexedValues().get(1).getValue();
        typedResponse.sender = (String) eventValues.getIndexedValues().get(2).getValue();
        return typedResponse;
    }

    public Flowable<RoleRevokedEventResponse> roleRevokedEventFlowable(EthFilter filter) {
        return web3j.ethLogFlowable(filter).map(log -> getRoleRevokedEventFromLog(log));
    }

    public Flowable<RoleRevokedEventResponse> roleRevokedEventFlowable(DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(ROLEREVOKED_EVENT));
        return roleRevokedEventFlowable(filter);
    }

    public RemoteFunctionCall<byte[]> ADMIN_ROLE() {
        final Function function = new Function(FUNC_ADMIN_ROLE,
                Arrays.<Type>asList(),
                Arrays.<TypeReference<?>>asList(new TypeReference<Bytes32>() {}));
        return executeRemoteCallSingleValueReturn(function, byte[].class);
    }

    public RemoteFunctionCall<byte[]> DEFAULT_ADMIN_ROLE() {
        final Function function = new Function(FUNC_DEFAULT_ADMIN_ROLE,
                Arrays.<Type>asList(),
                Arrays.<TypeReference<?>>asList(new TypeReference<Bytes32>() {}));
        return executeRemoteCallSingleValueReturn(function, byte[].class);
    }

    public RemoteFunctionCall<byte[]> FINANCE_ROLE() {
        final Function function = new Function(FUNC_FINANCE_ROLE,
                Arrays.<Type>asList(),
                Arrays.<TypeReference<?>>asList(new TypeReference<Bytes32>() {}));
        return executeRemoteCallSingleValueReturn(function, byte[].class);
    }

    public RemoteFunctionCall<byte[]> PROJECT_OFFICER_ROLE() {
        final Function function = new Function(FUNC_PROJECT_OFFICER_ROLE,
                Arrays.<Type>asList(),
                Arrays.<TypeReference<?>>asList(new TypeReference<Bytes32>() {}));
        return executeRemoteCallSingleValueReturn(function, byte[].class);
    }

    public RemoteFunctionCall<byte[]> SETTLEMENT_BANK_ROLE() {
        final Function function = new Function(FUNC_SETTLEMENT_BANK_ROLE,
                Arrays.<Type>asList(),
                Arrays.<TypeReference<?>>asList(new TypeReference<Bytes32>() {}));
        return executeRemoteCallSingleValueReturn(function, byte[].class);
    }

    public RemoteFunctionCall<byte[]> SUPER_ADMIN_ROLE() {
        final Function function = new Function(FUNC_SUPER_ADMIN_ROLE,
                Arrays.<Type>asList(),
                Arrays.<TypeReference<?>>asList(new TypeReference<Bytes32>() {}));
        return executeRemoteCallSingleValueReturn(function, byte[].class);
    }

    public RemoteFunctionCall<TransactionReceipt> approvePaymentOrder(BigInteger paymentOrderId) {
        final Function function = new Function(
                FUNC_APPROVEPAYMENTORDER,
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Uint256(paymentOrderId)),
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<TransactionReceipt> claimApproverApprove(BigInteger claimId) {
        final Function function = new Function(
                FUNC_CLAIMAPPROVERAPPROVE,
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Uint256(claimId)),
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<TransactionReceipt> claimApproverReject(BigInteger claimId, byte[] authorHash, String commentRef) {
        final Function function = new Function(
                FUNC_CLAIMAPPROVERREJECT,
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Uint256(claimId),
                new org.web3j.abi.datatypes.generated.Bytes32(authorHash),
                new org.web3j.abi.datatypes.Utf8String(commentRef)),
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<TransactionReceipt> createPaymentOrder(CreatePaymentOrderInput input) {
        final Function function = new Function(
                FUNC_CREATEPAYMENTORDER,
                Arrays.<Type>asList(input),
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<TransactionReceipt> createProject(CreateProjectInput input) {
        final Function function = new Function(
                FUNC_CREATEPROJECT,
                Arrays.<Type>asList(input),
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<TransactionReceipt> deleteClaim(BigInteger claimId) {
        final Function function = new Function(
                FUNC_DELETECLAIM,
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Uint256(claimId)),
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<TransactionReceipt> deleteInvoice(BigInteger invoiceId) {
        final Function function = new Function(
                FUNC_DELETEINVOICE,
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Uint256(invoiceId)),
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<TransactionReceipt> deleteProject(BigInteger projectId) {
        final Function function = new Function(
                FUNC_DELETEPROJECT,
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Uint256(projectId)),
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<TransactionReceipt> discardClaim(BigInteger claimId) {
        final Function function = new Function(
                FUNC_DISCARDCLAIM,
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Uint256(claimId)),
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<TransactionReceipt> discardInvoice(BigInteger invoiceId) {
        final Function function = new Function(
                FUNC_DISCARDINVOICE,
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Uint256(invoiceId)),
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<TransactionReceipt> financeApproveInvoice(BigInteger invoiceId) {
        final Function function = new Function(
                FUNC_FINANCEAPPROVEINVOICE,
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Uint256(invoiceId)),
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<TransactionReceipt> financeRejectInvoice(BigInteger invoiceId, byte[] authorHash, String commentRef) {
        final Function function = new Function(
                FUNC_FINANCEREJECTINVOICE,
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Uint256(invoiceId),
                new org.web3j.abi.datatypes.generated.Bytes32(authorHash),
                new org.web3j.abi.datatypes.Utf8String(commentRef)),
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<ApprovalSlot> getClaimApprover(BigInteger claimId, BigInteger index) {
        final Function function = new Function(FUNC_GETCLAIMAPPROVER,
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Uint256(claimId),
                new org.web3j.abi.datatypes.generated.Uint256(index)),
                Arrays.<TypeReference<?>>asList(new TypeReference<ApprovalSlot>() {}));
        return executeRemoteCallSingleValueReturn(function, ApprovalSlot.class);
    }

    public RemoteFunctionCall<BigInteger> getClaimApproverCount(BigInteger claimId) {
        final Function function = new Function(FUNC_GETCLAIMAPPROVERCOUNT,
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Uint256(claimId)),
                Arrays.<TypeReference<?>>asList(new TypeReference<Uint256>() {}));
        return executeRemoteCallSingleValueReturn(function, BigInteger.class);
    }

    public RemoteFunctionCall<DocumentRecord> getClaimDocument(BigInteger claimId, BigInteger index) {
        final Function function = new Function(FUNC_GETCLAIMDOCUMENT,
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Uint256(claimId),
                new org.web3j.abi.datatypes.generated.Uint256(index)),
                Arrays.<TypeReference<?>>asList(new TypeReference<DocumentRecord>() {}));
        return executeRemoteCallSingleValueReturn(function, DocumentRecord.class);
    }

    public RemoteFunctionCall<List> getClaimInvoiceIds(BigInteger claimId) {
        final Function function = new Function(FUNC_GETCLAIMINVOICEIDS,
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Uint256(claimId)),
                Arrays.<TypeReference<?>>asList(new TypeReference<DynamicArray<Uint256>>() {}));
        return new RemoteFunctionCall<List>(function,
                new Callable<List>() {
                    @Override
                    @SuppressWarnings("unchecked")
                    public List call() throws Exception {
                        List<Type> result = (List<Type>) executeCallSingleValueReturn(function, List.class);
                        return convertToNative(result);
                    }
                });
    }

    public RemoteFunctionCall<Tuple9<BigInteger, String, BigInteger, Participant, Participant, BigInteger, BigInteger, BigInteger, BigInteger>> getClaimSummary(BigInteger claimId) {
        final Function function = new Function(FUNC_GETCLAIMSUMMARY,
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Uint256(claimId)),
                Arrays.<TypeReference<?>>asList(new TypeReference<Uint256>() {}, new TypeReference<Utf8String>() {}, new TypeReference<Uint8>() {}, new TypeReference<Participant>() {}, new TypeReference<Participant>() {}, new TypeReference<Uint256>() {}, new TypeReference<Uint256>() {}, new TypeReference<Uint64>() {}, new TypeReference<Uint64>() {}));
        return new RemoteFunctionCall<Tuple9<BigInteger, String, BigInteger, Participant, Participant, BigInteger, BigInteger, BigInteger, BigInteger>>(function,
                new Callable<Tuple9<BigInteger, String, BigInteger, Participant, Participant, BigInteger, BigInteger, BigInteger, BigInteger>>() {
                    @Override
                    public Tuple9<BigInteger, String, BigInteger, Participant, Participant, BigInteger, BigInteger, BigInteger, BigInteger> call() throws Exception {
                        List<Type> results = executeCallMultipleValueReturn(function);
                        return new Tuple9<BigInteger, String, BigInteger, Participant, Participant, BigInteger, BigInteger, BigInteger, BigInteger>(
                                (BigInteger) results.get(0).getValue(),
                                (String) results.get(1).getValue(),
                                (BigInteger) results.get(2).getValue(),
                                (Participant) results.get(3),
                                (Participant) results.get(4),
                                (BigInteger) results.get(5).getValue(),
                                (BigInteger) results.get(6).getValue(),
                                (BigInteger) results.get(7).getValue(),
                                (BigInteger) results.get(8).getValue());
                    }
                });
    }

    public RemoteFunctionCall<Tuple2<String, byte[]>> getInvoiceDocument(BigInteger invoiceId, BigInteger index) {
        final Function function = new Function(FUNC_GETINVOICEDOCUMENT,
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Uint256(invoiceId),
                new org.web3j.abi.datatypes.generated.Uint256(index)),
                Arrays.<TypeReference<?>>asList(new TypeReference<Utf8String>() {}, new TypeReference<Bytes32>() {}));
        return new RemoteFunctionCall<Tuple2<String, byte[]>>(function,
                new Callable<Tuple2<String, byte[]>>() {
                    @Override
                    public Tuple2<String, byte[]> call() throws Exception {
                        List<Type> results = executeCallMultipleValueReturn(function);
                        return new Tuple2<String, byte[]>(
                                (String) results.get(0).getValue(),
                                (byte[]) results.get(1).getValue());
                    }
                });
    }

    public RemoteFunctionCall<List> getInvoicePaymentOrderIds(BigInteger invoiceId) {
        final Function function = new Function(FUNC_GETINVOICEPAYMENTORDERIDS,
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Uint256(invoiceId)),
                Arrays.<TypeReference<?>>asList(new TypeReference<DynamicArray<Uint256>>() {}));
        return new RemoteFunctionCall<List>(function,
                new Callable<List>() {
                    @Override
                    @SuppressWarnings("unchecked")
                    public List call() throws Exception {
                        List<Type> result = (List<Type>) executeCallSingleValueReturn(function, List.class);
                        return convertToNative(result);
                    }
                });
    }

    public RemoteFunctionCall<Tuple10<BigInteger, BigInteger, BankAccountDetails, BigInteger, String, Participant, Participant, BigInteger, BigInteger, BigInteger>> getInvoiceSummary(BigInteger invoiceId) {
        final Function function = new Function(FUNC_GETINVOICESUMMARY,
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Uint256(invoiceId)),
                Arrays.<TypeReference<?>>asList(new TypeReference<Uint256>() {}, new TypeReference<Uint8>() {}, new TypeReference<BankAccountDetails>() {}, new TypeReference<Uint256>() {}, new TypeReference<Utf8String>() {}, new TypeReference<Participant>() {}, new TypeReference<Participant>() {}, new TypeReference<Uint256>() {}, new TypeReference<Uint64>() {}, new TypeReference<Uint64>() {}));
        return new RemoteFunctionCall<Tuple10<BigInteger, BigInteger, BankAccountDetails, BigInteger, String, Participant, Participant, BigInteger, BigInteger, BigInteger>>(function,
                new Callable<Tuple10<BigInteger, BigInteger, BankAccountDetails, BigInteger, String, Participant, Participant, BigInteger, BigInteger, BigInteger>>() {
                    @Override
                    public Tuple10<BigInteger, BigInteger, BankAccountDetails, BigInteger, String, Participant, Participant, BigInteger, BigInteger, BigInteger> call() throws Exception {
                        List<Type> results = executeCallMultipleValueReturn(function);
                        return new Tuple10<BigInteger, BigInteger, BankAccountDetails, BigInteger, String, Participant, Participant, BigInteger, BigInteger, BigInteger>(
                                (BigInteger) results.get(0).getValue(),
                                (BigInteger) results.get(1).getValue(),
                                (BankAccountDetails) results.get(2),
                                (BigInteger) results.get(3).getValue(),
                                (String) results.get(4).getValue(),
                                (Participant) results.get(5),
                                (Participant) results.get(6),
                                (BigInteger) results.get(7).getValue(),
                                (BigInteger) results.get(8).getValue(),
                                (BigInteger) results.get(9).getValue());
                    }
                });
    }

    public RemoteFunctionCall<ApprovalSlot> getPaymentOrderApprover(BigInteger paymentOrderId, BigInteger index) {
        final Function function = new Function(FUNC_GETPAYMENTORDERAPPROVER,
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Uint256(paymentOrderId),
                new org.web3j.abi.datatypes.generated.Uint256(index)),
                Arrays.<TypeReference<?>>asList(new TypeReference<ApprovalSlot>() {}));
        return executeRemoteCallSingleValueReturn(function, ApprovalSlot.class);
    }

    public RemoteFunctionCall<BigInteger> getPaymentOrderApproverCount(BigInteger paymentOrderId) {
        final Function function = new Function(FUNC_GETPAYMENTORDERAPPROVERCOUNT,
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Uint256(paymentOrderId)),
                Arrays.<TypeReference<?>>asList(new TypeReference<Uint256>() {}));
        return executeRemoteCallSingleValueReturn(function, BigInteger.class);
    }

    public RemoteFunctionCall<BigInteger> getPaymentOrderPaymentId(BigInteger paymentOrderId) {
        final Function function = new Function(FUNC_GETPAYMENTORDERPAYMENTID,
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Uint256(paymentOrderId)),
                Arrays.<TypeReference<?>>asList(new TypeReference<Uint256>() {}));
        return executeRemoteCallSingleValueReturn(function, BigInteger.class);
    }

    public RemoteFunctionCall<Tuple9<BigInteger, BigInteger, String, BigInteger, String, BigInteger, String, BigInteger, BigInteger>> getPaymentOrderSummary(BigInteger paymentOrderId) {
        final Function function = new Function(FUNC_GETPAYMENTORDERSUMMARY,
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Uint256(paymentOrderId)),
                Arrays.<TypeReference<?>>asList(new TypeReference<Uint256>() {}, new TypeReference<Uint8>() {}, new TypeReference<Utf8String>() {}, new TypeReference<Uint256>() {}, new TypeReference<Utf8String>() {}, new TypeReference<Uint256>() {}, new TypeReference<Utf8String>() {}, new TypeReference<Uint64>() {}, new TypeReference<Uint64>() {}));
        return new RemoteFunctionCall<Tuple9<BigInteger, BigInteger, String, BigInteger, String, BigInteger, String, BigInteger, BigInteger>>(function,
                new Callable<Tuple9<BigInteger, BigInteger, String, BigInteger, String, BigInteger, String, BigInteger, BigInteger>>() {
                    @Override
                    public Tuple9<BigInteger, BigInteger, String, BigInteger, String, BigInteger, String, BigInteger, BigInteger> call() throws Exception {
                        List<Type> results = executeCallMultipleValueReturn(function);
                        return new Tuple9<BigInteger, BigInteger, String, BigInteger, String, BigInteger, String, BigInteger, BigInteger>(
                                (BigInteger) results.get(0).getValue(),
                                (BigInteger) results.get(1).getValue(),
                                (String) results.get(2).getValue(),
                                (BigInteger) results.get(3).getValue(),
                                (String) results.get(4).getValue(),
                                (BigInteger) results.get(5).getValue(),
                                (String) results.get(6).getValue(),
                                (BigInteger) results.get(7).getValue(),
                                (BigInteger) results.get(8).getValue());
                    }
                });
    }

    public RemoteFunctionCall<List> getProjectClaimIds(BigInteger projectId) {
        final Function function = new Function(FUNC_GETPROJECTCLAIMIDS,
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Uint256(projectId)),
                Arrays.<TypeReference<?>>asList(new TypeReference<DynamicArray<Uint256>>() {}));
        return new RemoteFunctionCall<List>(function,
                new Callable<List>() {
                    @Override
                    @SuppressWarnings("unchecked")
                    public List call() throws Exception {
                        List<Type> result = (List<Type>) executeCallSingleValueReturn(function, List.class);
                        return convertToNative(result);
                    }
                });
    }

    public RemoteFunctionCall<BigInteger> getProjectPaymentApproverCount(BigInteger projectId) {
        final Function function = new Function(FUNC_GETPROJECTPAYMENTAPPROVERCOUNT,
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Uint256(projectId)),
                Arrays.<TypeReference<?>>asList(new TypeReference<Uint256>() {}));
        return executeRemoteCallSingleValueReturn(function, BigInteger.class);
    }

    public RemoteFunctionCall<Tuple11<String, String, BigInteger, Participant, List<Participant>, List<ApproverConfig>, List<ApproverConfig>, List<String>, BigInteger, BigInteger, BigInteger>> getProjectSummary(BigInteger projectId) {
        final Function function = new Function(FUNC_GETPROJECTSUMMARY,
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Uint256(projectId)),
                Arrays.<TypeReference<?>>asList(new TypeReference<Utf8String>() {}, new TypeReference<Utf8String>() {}, new TypeReference<Uint8>() {}, new TypeReference<Participant>() {}, new TypeReference<DynamicArray<Participant>>() {}, new TypeReference<DynamicArray<ApproverConfig>>() {}, new TypeReference<DynamicArray<ApproverConfig>>() {}, new TypeReference<DynamicArray<Utf8String>>() {}, new TypeReference<Uint64>() {}, new TypeReference<Uint64>() {}, new TypeReference<Uint256>() {}));
        return new RemoteFunctionCall<Tuple11<String, String, BigInteger, Participant, List<Participant>, List<ApproverConfig>, List<ApproverConfig>, List<String>, BigInteger, BigInteger, BigInteger>>(function,
                new Callable<Tuple11<String, String, BigInteger, Participant, List<Participant>, List<ApproverConfig>, List<ApproverConfig>, List<String>, BigInteger, BigInteger, BigInteger>>() {
                    @Override
                    public Tuple11<String, String, BigInteger, Participant, List<Participant>, List<ApproverConfig>, List<ApproverConfig>, List<String>, BigInteger, BigInteger, BigInteger> call() throws Exception {
                        List<Type> results = executeCallMultipleValueReturn(function);
                        return new Tuple11<String, String, BigInteger, Participant, List<Participant>, List<ApproverConfig>, List<ApproverConfig>, List<String>, BigInteger, BigInteger, BigInteger>(
                                (String) results.get(0).getValue(),
                                (String) results.get(1).getValue(),
                                (BigInteger) results.get(2).getValue(),
                                (Participant) results.get(3),
                                convertToNative((List<Participant>) results.get(4).getValue()),
                                convertToNative((List<ApproverConfig>) results.get(5).getValue()),
                                convertToNative((List<ApproverConfig>) results.get(6).getValue()),
                                convertToNative((List<Utf8String>) results.get(7).getValue()),
                                (BigInteger) results.get(8).getValue(),
                                (BigInteger) results.get(9).getValue(),
                                (BigInteger) results.get(10).getValue());
                    }
                });
    }

    public RemoteFunctionCall<byte[]> getRoleAdmin(byte[] role) {
        final Function function = new Function(FUNC_GETROLEADMIN,
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Bytes32(role)),
                Arrays.<TypeReference<?>>asList(new TypeReference<Bytes32>() {}));
        return executeRemoteCallSingleValueReturn(function, byte[].class);
    }

    public RemoteFunctionCall<TransactionReceipt> grantRole(byte[] role, String account) {
        final Function function = new Function(
                FUNC_GRANTROLE,
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Bytes32(role),
                new org.web3j.abi.datatypes.Address(160, account)),
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<Boolean> hasRole(byte[] role, String account) {
        final Function function = new Function(FUNC_HASROLE,
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Bytes32(role),
                new org.web3j.abi.datatypes.Address(160, account)),
                Arrays.<TypeReference<?>>asList(new TypeReference<Bool>() {}));
        return executeRemoteCallSingleValueReturn(function, Boolean.class);
    }

    public RemoteFunctionCall<TransactionReceipt> projectOfficerApproveClaim(BigInteger claimId) {
        final Function function = new Function(
                FUNC_PROJECTOFFICERAPPROVECLAIM,
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Uint256(claimId)),
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<TransactionReceipt> projectOfficerApproveInvoice(BigInteger invoiceId) {
        final Function function = new Function(
                FUNC_PROJECTOFFICERAPPROVEINVOICE,
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Uint256(invoiceId)),
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<TransactionReceipt> projectOfficerRejectClaim(BigInteger claimId, byte[] authorHash, String commentRef) {
        final Function function = new Function(
                FUNC_PROJECTOFFICERREJECTCLAIM,
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Uint256(claimId),
                new org.web3j.abi.datatypes.generated.Bytes32(authorHash),
                new org.web3j.abi.datatypes.Utf8String(commentRef)),
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<TransactionReceipt> projectOfficerRejectInvoice(BigInteger invoiceId, byte[] authorHash, String commentRef) {
        final Function function = new Function(
                FUNC_PROJECTOFFICERREJECTINVOICE,
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Uint256(invoiceId),
                new org.web3j.abi.datatypes.generated.Bytes32(authorHash),
                new org.web3j.abi.datatypes.Utf8String(commentRef)),
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<TransactionReceipt> recordBankPaymentReference(BigInteger paymentOrderId, String bankPaymentRef) {
        final Function function = new Function(
                FUNC_RECORDBANKPAYMENTREFERENCE,
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Uint256(paymentOrderId),
                new org.web3j.abi.datatypes.Utf8String(bankPaymentRef)),
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<TransactionReceipt> rejectPaymentOrder(BigInteger paymentOrderId, byte[] authorHash, String commentRef) {
        final Function function = new Function(
                FUNC_REJECTPAYMENTORDER,
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Uint256(paymentOrderId),
                new org.web3j.abi.datatypes.generated.Bytes32(authorHash),
                new org.web3j.abi.datatypes.Utf8String(commentRef)),
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<TransactionReceipt> removeProjectApprover(BigInteger projectId, byte[] userHash) {
        final Function function = new Function(
                FUNC_REMOVEPROJECTAPPROVER,
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Uint256(projectId),
                new org.web3j.abi.datatypes.generated.Bytes32(userHash)),
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<TransactionReceipt> renounceRole(byte[] role, String callerConfirmation) {
        final Function function = new Function(
                FUNC_RENOUNCEROLE,
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Bytes32(role),
                new org.web3j.abi.datatypes.Address(160, callerConfirmation)),
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<TransactionReceipt> requestProjectDeletion(BigInteger projectId) {
        final Function function = new Function(
                FUNC_REQUESTPROJECTDELETION,
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Uint256(projectId)),
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<TransactionReceipt> resubmitClaim(UpdateClaimInput input) {
        final Function function = new Function(
                FUNC_RESUBMITCLAIM,
                Arrays.<Type>asList(input),
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<TransactionReceipt> resubmitPaymentOrder(ResubmitPaymentOrderInput input) {
        final Function function = new Function(
                FUNC_RESUBMITPAYMENTORDER,
                Arrays.<Type>asList(input),
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<TransactionReceipt> revokeRole(byte[] role, String account) {
        final Function function = new Function(
                FUNC_REVOKEROLE,
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Bytes32(role),
                new org.web3j.abi.datatypes.Address(160, account)),
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<TransactionReceipt> submitClaim(SubmitClaimInput input) {
        final Function function = new Function(
                FUNC_SUBMITCLAIM,
                Arrays.<Type>asList(input),
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<TransactionReceipt> submitInvoice(SubmitInvoiceInput input) {
        final Function function = new Function(
                FUNC_SUBMITINVOICE,
                Arrays.<Type>asList(input),
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<Boolean> supportsInterface(byte[] interfaceId) {
        final Function function = new Function(FUNC_SUPPORTSINTERFACE,
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Bytes4(interfaceId)),
                Arrays.<TypeReference<?>>asList(new TypeReference<Bool>() {}));
        return executeRemoteCallSingleValueReturn(function, Boolean.class);
    }

    public RemoteFunctionCall<TransactionReceipt> updateClaim(UpdateClaimInput input) {
        final Function function = new Function(
                FUNC_UPDATECLAIM,
                Arrays.<Type>asList(input),
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<TransactionReceipt> updateInvoice(UpdateInvoiceInput input) {
        final Function function = new Function(
                FUNC_UPDATEINVOICE,
                Arrays.<Type>asList(input),
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<TransactionReceipt> updateProject(UpdateProjectInput input) {
        final Function function = new Function(
                FUNC_UPDATEPROJECT,
                Arrays.<Type>asList(input),
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<TransactionReceipt> updateProjectBankAccounts(BigInteger projectId, List<String> bankAccountRefs) {
        final Function function = new Function(
                FUNC_UPDATEPROJECTBANKACCOUNTS,
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Uint256(projectId),
                new org.web3j.abi.datatypes.DynamicArray<org.web3j.abi.datatypes.Utf8String>(
                        org.web3j.abi.datatypes.Utf8String.class,
                        org.web3j.abi.Utils.typeMap(bankAccountRefs, org.web3j.abi.datatypes.Utf8String.class))),
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    @Deprecated
    public static TopazLifecycle load(String contractAddress, Web3j web3j, Credentials credentials, BigInteger gasPrice, BigInteger gasLimit) {
        return new TopazLifecycle(contractAddress, web3j, credentials, gasPrice, gasLimit);
    }

    @Deprecated
    public static TopazLifecycle load(String contractAddress, Web3j web3j, TransactionManager transactionManager, BigInteger gasPrice, BigInteger gasLimit) {
        return new TopazLifecycle(contractAddress, web3j, transactionManager, gasPrice, gasLimit);
    }

    public static TopazLifecycle load(String contractAddress, Web3j web3j, Credentials credentials, ContractGasProvider contractGasProvider) {
        return new TopazLifecycle(contractAddress, web3j, credentials, contractGasProvider);
    }

    public static TopazLifecycle load(String contractAddress, Web3j web3j, TransactionManager transactionManager, ContractGasProvider contractGasProvider) {
        return new TopazLifecycle(contractAddress, web3j, transactionManager, contractGasProvider);
    }

    public static class AccountInfo extends DynamicStruct {
        public String accountName;

        public String accountNumber;

        public String addressLine1;

        public String addressLine2;

        public String bic;

        public String ultimateName;

        public AccountInfo(String accountName, String accountNumber, String addressLine1, String addressLine2, String bic, String ultimateName) {
            super(new org.web3j.abi.datatypes.Utf8String(accountName),
                    new org.web3j.abi.datatypes.Utf8String(accountNumber),
                    new org.web3j.abi.datatypes.Utf8String(addressLine1),
                    new org.web3j.abi.datatypes.Utf8String(addressLine2),
                    new org.web3j.abi.datatypes.Utf8String(bic),
                    new org.web3j.abi.datatypes.Utf8String(ultimateName));
            this.accountName = accountName;
            this.accountNumber = accountNumber;
            this.addressLine1 = addressLine1;
            this.addressLine2 = addressLine2;
            this.bic = bic;
            this.ultimateName = ultimateName;
        }

        public AccountInfo(Utf8String accountName, Utf8String accountNumber, Utf8String addressLine1, Utf8String addressLine2, Utf8String bic, Utf8String ultimateName) {
            super(accountName, accountNumber, addressLine1, addressLine2, bic, ultimateName);
            this.accountName = accountName.getValue();
            this.accountNumber = accountNumber.getValue();
            this.addressLine1 = addressLine1.getValue();
            this.addressLine2 = addressLine2.getValue();
            this.bic = bic.getValue();
            this.ultimateName = ultimateName.getValue();
        }
    }

    public static class Participant extends DynamicStruct {
        public String wallet;

        public String legalName;

        public String addressLine1;

        public String addressLine2;

        public String bic;

        public String lei;

        public String externalRef;

        public Participant(String wallet, String legalName, String addressLine1, String addressLine2, String bic, String lei, String externalRef) {
            super(new org.web3j.abi.datatypes.Address(160, wallet),
                    new org.web3j.abi.datatypes.Utf8String(legalName),
                    new org.web3j.abi.datatypes.Utf8String(addressLine1),
                    new org.web3j.abi.datatypes.Utf8String(addressLine2),
                    new org.web3j.abi.datatypes.Utf8String(bic),
                    new org.web3j.abi.datatypes.Utf8String(lei),
                    new org.web3j.abi.datatypes.Utf8String(externalRef));
            this.wallet = wallet;
            this.legalName = legalName;
            this.addressLine1 = addressLine1;
            this.addressLine2 = addressLine2;
            this.bic = bic;
            this.lei = lei;
            this.externalRef = externalRef;
        }

        public Participant(Address wallet, Utf8String legalName, Utf8String addressLine1, Utf8String addressLine2, Utf8String bic, Utf8String lei, Utf8String externalRef) {
            super(wallet, legalName, addressLine1, addressLine2, bic, lei, externalRef);
            this.wallet = wallet.getValue();
            this.legalName = legalName.getValue();
            this.addressLine1 = addressLine1.getValue();
            this.addressLine2 = addressLine2.getValue();
            this.bic = bic.getValue();
            this.lei = lei.getValue();
            this.externalRef = externalRef.getValue();
        }
    }

    public static class ApproverConfig extends DynamicStruct {
        public String wallet;

        public byte[] userHash;

        public String email;

        public String firstName;

        public String lastName;

        public String userProfileName;

        public String roleName;

        public String externalRef;

        public ApproverConfig(String wallet, byte[] userHash, String email, String firstName, String lastName, String userProfileName, String roleName, String externalRef) {
            super(new org.web3j.abi.datatypes.Address(160, wallet),
                    new org.web3j.abi.datatypes.generated.Bytes32(userHash),
                    new org.web3j.abi.datatypes.Utf8String(email),
                    new org.web3j.abi.datatypes.Utf8String(firstName),
                    new org.web3j.abi.datatypes.Utf8String(lastName),
                    new org.web3j.abi.datatypes.Utf8String(userProfileName),
                    new org.web3j.abi.datatypes.Utf8String(roleName),
                    new org.web3j.abi.datatypes.Utf8String(externalRef));
            this.wallet = wallet;
            this.userHash = userHash;
            this.email = email;
            this.firstName = firstName;
            this.lastName = lastName;
            this.userProfileName = userProfileName;
            this.roleName = roleName;
            this.externalRef = externalRef;
        }

        public ApproverConfig(Address wallet, Bytes32 userHash, Utf8String email, Utf8String firstName, Utf8String lastName, Utf8String userProfileName, Utf8String roleName, Utf8String externalRef) {
            super(wallet, userHash, email, firstName, lastName, userProfileName, roleName, externalRef);
            this.wallet = wallet.getValue();
            this.userHash = userHash.getValue();
            this.email = email.getValue();
            this.firstName = firstName.getValue();
            this.lastName = lastName.getValue();
            this.userProfileName = userProfileName.getValue();
            this.roleName = roleName.getValue();
            this.externalRef = externalRef.getValue();
        }
    }

    public static class ApprovalSlot extends DynamicStruct {
        public String wallet;

        public byte[] userHash;

        public String email;

        public String firstName;

        public String lastName;

        public String userProfileName;

        public String roleName;

        public String externalRef;

        public BigInteger status;

        public BigInteger actionTimestamp;

        public ApprovalSlot(String wallet, byte[] userHash, String email, String firstName, String lastName, String userProfileName, String roleName, String externalRef, BigInteger status, BigInteger actionTimestamp) {
            super(new org.web3j.abi.datatypes.Address(160, wallet),
                    new org.web3j.abi.datatypes.generated.Bytes32(userHash),
                    new org.web3j.abi.datatypes.Utf8String(email),
                    new org.web3j.abi.datatypes.Utf8String(firstName),
                    new org.web3j.abi.datatypes.Utf8String(lastName),
                    new org.web3j.abi.datatypes.Utf8String(userProfileName),
                    new org.web3j.abi.datatypes.Utf8String(roleName),
                    new org.web3j.abi.datatypes.Utf8String(externalRef),
                    new org.web3j.abi.datatypes.generated.Uint8(status),
                    new org.web3j.abi.datatypes.generated.Uint64(actionTimestamp));
            this.wallet = wallet;
            this.userHash = userHash;
            this.email = email;
            this.firstName = firstName;
            this.lastName = lastName;
            this.userProfileName = userProfileName;
            this.roleName = roleName;
            this.externalRef = externalRef;
            this.status = status;
            this.actionTimestamp = actionTimestamp;
        }

        public ApprovalSlot(Address wallet, Bytes32 userHash, Utf8String email, Utf8String firstName, Utf8String lastName, Utf8String userProfileName, Utf8String roleName, Utf8String externalRef, Uint8 status, Uint64 actionTimestamp) {
            super(wallet, userHash, email, firstName, lastName, userProfileName, roleName, externalRef, status, actionTimestamp);
            this.wallet = wallet.getValue();
            this.userHash = userHash.getValue();
            this.email = email.getValue();
            this.firstName = firstName.getValue();
            this.lastName = lastName.getValue();
            this.userProfileName = userProfileName.getValue();
            this.roleName = roleName.getValue();
            this.externalRef = externalRef.getValue();
            this.status = status.getValue();
            this.actionTimestamp = actionTimestamp.getValue();
        }
    }

    public static class DocumentRecord extends DynamicStruct {
        public String documentId;

        public byte[] documentHash;

        public DocumentRecord(String documentId, byte[] documentHash) {
            super(new org.web3j.abi.datatypes.Utf8String(documentId),
                    new org.web3j.abi.datatypes.generated.Bytes32(documentHash));
            this.documentId = documentId;
            this.documentHash = documentHash;
        }

        public DocumentRecord(Utf8String documentId, Bytes32 documentHash) {
            super(documentId, documentHash);
            this.documentId = documentId.getValue();
            this.documentHash = documentHash.getValue();
        }
    }

    public static class BankAccountDetails extends DynamicStruct {
        public String swiftAddress;

        public String bankAccountHolderName;

        public String bankAccountNumberRef;

        public String bankName;

        public String registeredAddress;

        public String currency;

        public BankAccountDetails(String swiftAddress, String bankAccountHolderName, String bankAccountNumberRef, String bankName, String registeredAddress, String currency) {
            super(new org.web3j.abi.datatypes.Utf8String(swiftAddress),
                    new org.web3j.abi.datatypes.Utf8String(bankAccountHolderName),
                    new org.web3j.abi.datatypes.Utf8String(bankAccountNumberRef),
                    new org.web3j.abi.datatypes.Utf8String(bankName),
                    new org.web3j.abi.datatypes.Utf8String(registeredAddress),
                    new org.web3j.abi.datatypes.Utf8String(currency));
            this.swiftAddress = swiftAddress;
            this.bankAccountHolderName = bankAccountHolderName;
            this.bankAccountNumberRef = bankAccountNumberRef;
            this.bankName = bankName;
            this.registeredAddress = registeredAddress;
            this.currency = currency;
        }

        public BankAccountDetails(Utf8String swiftAddress, Utf8String bankAccountHolderName, Utf8String bankAccountNumberRef, Utf8String bankName, Utf8String registeredAddress, Utf8String currency) {
            super(swiftAddress, bankAccountHolderName, bankAccountNumberRef, bankName, registeredAddress, currency);
            this.swiftAddress = swiftAddress.getValue();
            this.bankAccountHolderName = bankAccountHolderName.getValue();
            this.bankAccountNumberRef = bankAccountNumberRef.getValue();
            this.bankName = bankName.getValue();
            this.registeredAddress = registeredAddress.getValue();
            this.currency = currency.getValue();
        }
    }

    public static class DocumentInput extends DynamicStruct {
        public String documentId;

        public byte[] documentHash;

        public DocumentInput(String documentId, byte[] documentHash) {
            super(new org.web3j.abi.datatypes.Utf8String(documentId),
                    new org.web3j.abi.datatypes.generated.Bytes32(documentHash));
            this.documentId = documentId;
            this.documentHash = documentHash;
        }

        public DocumentInput(Utf8String documentId, Bytes32 documentHash) {
            super(documentId, documentHash);
            this.documentId = documentId.getValue();
            this.documentHash = documentHash.getValue();
        }
    }

    public static class CreatePaymentOrderInput extends DynamicStruct {
        public BigInteger invoiceId;

        public AccountInfo fromAccount;

        public String customerRefNumber;

        public String chargeBearer;

        public List<String> remittanceInformation;

        public String purposeCode;

        public BigInteger valueDate;

        public List<String> bankInformation;

        public String paymentType;

        public String preparerRef;

        public CreatePaymentOrderInput(BigInteger invoiceId, AccountInfo fromAccount, String customerRefNumber, String chargeBearer, List<String> remittanceInformation, String purposeCode, BigInteger valueDate, List<String> bankInformation, String paymentType, String preparerRef) {
            super(new org.web3j.abi.datatypes.generated.Uint256(invoiceId),
                    fromAccount,
                    new org.web3j.abi.datatypes.Utf8String(customerRefNumber),
                    new org.web3j.abi.datatypes.Utf8String(chargeBearer),
                    new org.web3j.abi.datatypes.DynamicArray<org.web3j.abi.datatypes.Utf8String>(
                            org.web3j.abi.datatypes.Utf8String.class,
                            org.web3j.abi.Utils.typeMap(remittanceInformation, org.web3j.abi.datatypes.Utf8String.class)),
                    new org.web3j.abi.datatypes.Utf8String(purposeCode),
                    new org.web3j.abi.datatypes.generated.Uint64(valueDate),
                    new org.web3j.abi.datatypes.DynamicArray<org.web3j.abi.datatypes.Utf8String>(
                            org.web3j.abi.datatypes.Utf8String.class,
                            org.web3j.abi.Utils.typeMap(bankInformation, org.web3j.abi.datatypes.Utf8String.class)),
                    new org.web3j.abi.datatypes.Utf8String(paymentType),
                    new org.web3j.abi.datatypes.Utf8String(preparerRef));
            this.invoiceId = invoiceId;
            this.fromAccount = fromAccount;
            this.customerRefNumber = customerRefNumber;
            this.chargeBearer = chargeBearer;
            this.remittanceInformation = remittanceInformation;
            this.purposeCode = purposeCode;
            this.valueDate = valueDate;
            this.bankInformation = bankInformation;
            this.paymentType = paymentType;
            this.preparerRef = preparerRef;
        }

        public CreatePaymentOrderInput(Uint256 invoiceId, AccountInfo fromAccount, Utf8String customerRefNumber, Utf8String chargeBearer, DynamicArray<Utf8String> remittanceInformation, Utf8String purposeCode, Uint64 valueDate, DynamicArray<Utf8String> bankInformation, Utf8String paymentType, Utf8String preparerRef) {
            super(invoiceId, fromAccount, customerRefNumber, chargeBearer, remittanceInformation, purposeCode, valueDate, bankInformation, paymentType, preparerRef);
            this.invoiceId = invoiceId.getValue();
            this.fromAccount = fromAccount;
            this.customerRefNumber = customerRefNumber.getValue();
            this.chargeBearer = chargeBearer.getValue();
            this.remittanceInformation = remittanceInformation.getValue().stream().map(v -> v.getValue()).collect(Collectors.toList());
            this.purposeCode = purposeCode.getValue();
            this.valueDate = valueDate.getValue();
            this.bankInformation = bankInformation.getValue().stream().map(v -> v.getValue()).collect(Collectors.toList());
            this.paymentType = paymentType.getValue();
            this.preparerRef = preparerRef.getValue();
        }
    }

    public static class CreateProjectInput extends DynamicStruct {
        public String externalProjectId;

        public String name;

        public Participant developer;

        public List<Participant> mainContractors;

        public List<ApproverConfig> claimApprovers;

        public List<ApproverConfig> paymentApprovers;

        public List<String> bankAccountRefs;

        public CreateProjectInput(String externalProjectId, String name, Participant developer, List<Participant> mainContractors, List<ApproverConfig> claimApprovers, List<ApproverConfig> paymentApprovers, List<String> bankAccountRefs) {
            super(new org.web3j.abi.datatypes.Utf8String(externalProjectId),
                    new org.web3j.abi.datatypes.Utf8String(name),
                    developer,
                    new org.web3j.abi.datatypes.DynamicArray<Participant>(Participant.class, mainContractors),
                    new org.web3j.abi.datatypes.DynamicArray<ApproverConfig>(ApproverConfig.class, claimApprovers),
                    new org.web3j.abi.datatypes.DynamicArray<ApproverConfig>(ApproverConfig.class, paymentApprovers),
                    new org.web3j.abi.datatypes.DynamicArray<org.web3j.abi.datatypes.Utf8String>(
                            org.web3j.abi.datatypes.Utf8String.class,
                            org.web3j.abi.Utils.typeMap(bankAccountRefs, org.web3j.abi.datatypes.Utf8String.class)));
            this.externalProjectId = externalProjectId;
            this.name = name;
            this.developer = developer;
            this.mainContractors = mainContractors;
            this.claimApprovers = claimApprovers;
            this.paymentApprovers = paymentApprovers;
            this.bankAccountRefs = bankAccountRefs;
        }

        public CreateProjectInput(Utf8String externalProjectId, Utf8String name, Participant developer, @Parameterized(type = Participant.class) DynamicArray<Participant> mainContractors, @Parameterized(type = ApproverConfig.class) DynamicArray<ApproverConfig> claimApprovers, @Parameterized(type = ApproverConfig.class) DynamicArray<ApproverConfig> paymentApprovers, DynamicArray<Utf8String> bankAccountRefs) {
            super(externalProjectId, name, developer, mainContractors, claimApprovers, paymentApprovers, bankAccountRefs);
            this.externalProjectId = externalProjectId.getValue();
            this.name = name.getValue();
            this.developer = developer;
            this.mainContractors = mainContractors.getValue();
            this.claimApprovers = claimApprovers.getValue();
            this.paymentApprovers = paymentApprovers.getValue();
            this.bankAccountRefs = bankAccountRefs.getValue().stream().map(v -> v.getValue()).collect(Collectors.toList());
        }
    }

    public static class UpdateClaimInput extends DynamicStruct {
        public BigInteger claimId;

        public String descriptionRef;

        public List<DocumentInput> documents;

        public UpdateClaimInput(BigInteger claimId, String descriptionRef, List<DocumentInput> documents) {
            super(new org.web3j.abi.datatypes.generated.Uint256(claimId),
                    new org.web3j.abi.datatypes.Utf8String(descriptionRef),
                    new org.web3j.abi.datatypes.DynamicArray<DocumentInput>(DocumentInput.class, documents));
            this.claimId = claimId;
            this.descriptionRef = descriptionRef;
            this.documents = documents;
        }

        public UpdateClaimInput(Uint256 claimId, Utf8String descriptionRef, @Parameterized(type = DocumentInput.class) DynamicArray<DocumentInput> documents) {
            super(claimId, descriptionRef, documents);
            this.claimId = claimId.getValue();
            this.descriptionRef = descriptionRef.getValue();
            this.documents = documents.getValue();
        }
    }

    public static class ResubmitPaymentOrderInput extends DynamicStruct {
        public BigInteger paymentOrderId;

        public AccountInfo fromAccount;

        public String customerRefNumber;

        public String chargeBearer;

        public List<String> remittanceInformation;

        public String purposeCode;

        public BigInteger valueDate;

        public List<String> bankInformation;

        public String paymentType;

        public String preparerRef;

        public ResubmitPaymentOrderInput(BigInteger paymentOrderId, AccountInfo fromAccount, String customerRefNumber, String chargeBearer, List<String> remittanceInformation, String purposeCode, BigInteger valueDate, List<String> bankInformation, String paymentType, String preparerRef) {
            super(new org.web3j.abi.datatypes.generated.Uint256(paymentOrderId),
                    fromAccount,
                    new org.web3j.abi.datatypes.Utf8String(customerRefNumber),
                    new org.web3j.abi.datatypes.Utf8String(chargeBearer),
                    new org.web3j.abi.datatypes.DynamicArray<org.web3j.abi.datatypes.Utf8String>(
                            org.web3j.abi.datatypes.Utf8String.class,
                            org.web3j.abi.Utils.typeMap(remittanceInformation, org.web3j.abi.datatypes.Utf8String.class)),
                    new org.web3j.abi.datatypes.Utf8String(purposeCode),
                    new org.web3j.abi.datatypes.generated.Uint64(valueDate),
                    new org.web3j.abi.datatypes.DynamicArray<org.web3j.abi.datatypes.Utf8String>(
                            org.web3j.abi.datatypes.Utf8String.class,
                            org.web3j.abi.Utils.typeMap(bankInformation, org.web3j.abi.datatypes.Utf8String.class)),
                    new org.web3j.abi.datatypes.Utf8String(paymentType),
                    new org.web3j.abi.datatypes.Utf8String(preparerRef));
            this.paymentOrderId = paymentOrderId;
            this.fromAccount = fromAccount;
            this.customerRefNumber = customerRefNumber;
            this.chargeBearer = chargeBearer;
            this.remittanceInformation = remittanceInformation;
            this.purposeCode = purposeCode;
            this.valueDate = valueDate;
            this.bankInformation = bankInformation;
            this.paymentType = paymentType;
            this.preparerRef = preparerRef;
        }

        public ResubmitPaymentOrderInput(Uint256 paymentOrderId, AccountInfo fromAccount, Utf8String customerRefNumber, Utf8String chargeBearer, DynamicArray<Utf8String> remittanceInformation, Utf8String purposeCode, Uint64 valueDate, DynamicArray<Utf8String> bankInformation, Utf8String paymentType, Utf8String preparerRef) {
            super(paymentOrderId, fromAccount, customerRefNumber, chargeBearer, remittanceInformation, purposeCode, valueDate, bankInformation, paymentType, preparerRef);
            this.paymentOrderId = paymentOrderId.getValue();
            this.fromAccount = fromAccount;
            this.customerRefNumber = customerRefNumber.getValue();
            this.chargeBearer = chargeBearer.getValue();
            this.remittanceInformation = remittanceInformation.getValue().stream().map(v -> v.getValue()).collect(Collectors.toList());
            this.purposeCode = purposeCode.getValue();
            this.valueDate = valueDate.getValue();
            this.bankInformation = bankInformation.getValue().stream().map(v -> v.getValue()).collect(Collectors.toList());
            this.paymentType = paymentType.getValue();
            this.preparerRef = preparerRef.getValue();
        }
    }

    public static class SubmitClaimInput extends DynamicStruct {
        public BigInteger projectId;

        public String descriptionRef;

        public List<DocumentInput> documents;

        public SubmitClaimInput(BigInteger projectId, String descriptionRef, List<DocumentInput> documents) {
            super(new org.web3j.abi.datatypes.generated.Uint256(projectId),
                    new org.web3j.abi.datatypes.Utf8String(descriptionRef),
                    new org.web3j.abi.datatypes.DynamicArray<DocumentInput>(DocumentInput.class, documents));
            this.projectId = projectId;
            this.descriptionRef = descriptionRef;
            this.documents = documents;
        }

        public SubmitClaimInput(Uint256 projectId, Utf8String descriptionRef, @Parameterized(type = DocumentInput.class) DynamicArray<DocumentInput> documents) {
            super(projectId, descriptionRef, documents);
            this.projectId = projectId.getValue();
            this.descriptionRef = descriptionRef.getValue();
            this.documents = documents.getValue();
        }
    }

    public static class SubmitInvoiceInput extends DynamicStruct {
        public BigInteger claimId;

        public BigInteger amountMinor;

        public BankAccountDetails bankAccount;

        public List<DocumentInput> documents;

        public SubmitInvoiceInput(BigInteger claimId, BigInteger amountMinor, BankAccountDetails bankAccount, List<DocumentInput> documents) {
            super(new org.web3j.abi.datatypes.generated.Uint256(claimId),
                    new org.web3j.abi.datatypes.generated.Uint256(amountMinor),
                    bankAccount,
                    new org.web3j.abi.datatypes.DynamicArray<DocumentInput>(DocumentInput.class, documents));
            this.claimId = claimId;
            this.amountMinor = amountMinor;
            this.bankAccount = bankAccount;
            this.documents = documents;
        }

        public SubmitInvoiceInput(Uint256 claimId, Uint256 amountMinor, BankAccountDetails bankAccount, @Parameterized(type = DocumentInput.class) DynamicArray<DocumentInput> documents) {
            super(claimId, amountMinor, bankAccount, documents);
            this.claimId = claimId.getValue();
            this.amountMinor = amountMinor.getValue();
            this.bankAccount = bankAccount;
            this.documents = documents.getValue();
        }
    }

    public static class UpdateInvoiceInput extends DynamicStruct {
        public BigInteger invoiceId;

        public BigInteger amountMinor;

        public BankAccountDetails bankAccount;

        public List<DocumentInput> documents;

        public UpdateInvoiceInput(BigInteger invoiceId, BigInteger amountMinor, BankAccountDetails bankAccount, List<DocumentInput> documents) {
            super(new org.web3j.abi.datatypes.generated.Uint256(invoiceId),
                    new org.web3j.abi.datatypes.generated.Uint256(amountMinor),
                    bankAccount,
                    new org.web3j.abi.datatypes.DynamicArray<DocumentInput>(DocumentInput.class, documents));
            this.invoiceId = invoiceId;
            this.amountMinor = amountMinor;
            this.bankAccount = bankAccount;
            this.documents = documents;
        }

        public UpdateInvoiceInput(Uint256 invoiceId, Uint256 amountMinor, BankAccountDetails bankAccount, @Parameterized(type = DocumentInput.class) DynamicArray<DocumentInput> documents) {
            super(invoiceId, amountMinor, bankAccount, documents);
            this.invoiceId = invoiceId.getValue();
            this.amountMinor = amountMinor.getValue();
            this.bankAccount = bankAccount;
            this.documents = documents.getValue();
        }
    }

    public static class UpdateProjectInput extends DynamicStruct {
        public BigInteger projectId;

        public String externalProjectId;

        public String name;

        public List<Participant> mainContractors;

        public List<ApproverConfig> claimApprovers;

        public List<ApproverConfig> paymentApprovers;

        public List<String> bankAccountRefs;

        public UpdateProjectInput(BigInteger projectId, String externalProjectId, String name, List<Participant> mainContractors, List<ApproverConfig> claimApprovers, List<ApproverConfig> paymentApprovers, List<String> bankAccountRefs) {
            super(new org.web3j.abi.datatypes.generated.Uint256(projectId),
                    new org.web3j.abi.datatypes.Utf8String(externalProjectId),
                    new org.web3j.abi.datatypes.Utf8String(name),
                    new org.web3j.abi.datatypes.DynamicArray<Participant>(Participant.class, mainContractors),
                    new org.web3j.abi.datatypes.DynamicArray<ApproverConfig>(ApproverConfig.class, claimApprovers),
                    new org.web3j.abi.datatypes.DynamicArray<ApproverConfig>(ApproverConfig.class, paymentApprovers),
                    new org.web3j.abi.datatypes.DynamicArray<org.web3j.abi.datatypes.Utf8String>(
                            org.web3j.abi.datatypes.Utf8String.class,
                            org.web3j.abi.Utils.typeMap(bankAccountRefs, org.web3j.abi.datatypes.Utf8String.class)));
            this.projectId = projectId;
            this.externalProjectId = externalProjectId;
            this.name = name;
            this.mainContractors = mainContractors;
            this.claimApprovers = claimApprovers;
            this.paymentApprovers = paymentApprovers;
            this.bankAccountRefs = bankAccountRefs;
        }

        public UpdateProjectInput(Uint256 projectId, Utf8String externalProjectId, Utf8String name, @Parameterized(type = Participant.class) DynamicArray<Participant> mainContractors, @Parameterized(type = ApproverConfig.class) DynamicArray<ApproverConfig> claimApprovers, @Parameterized(type = ApproverConfig.class) DynamicArray<ApproverConfig> paymentApprovers, DynamicArray<Utf8String> bankAccountRefs) {
            super(projectId, externalProjectId, name, mainContractors, claimApprovers, paymentApprovers, bankAccountRefs);
            this.projectId = projectId.getValue();
            this.externalProjectId = externalProjectId.getValue();
            this.name = name.getValue();
            this.mainContractors = mainContractors.getValue();
            this.claimApprovers = claimApprovers.getValue();
            this.paymentApprovers = paymentApprovers.getValue();
            this.bankAccountRefs = bankAccountRefs.getValue().stream().map(v -> v.getValue()).collect(Collectors.toList());
        }
    }

    public static class BankPaymentReferenceRecordedEventResponse extends BaseEventResponse {
        public BigInteger paymentOrderId;

        public String bankPaymentRef;
    }

    public static class BankPaymentRequestedEventResponse extends BaseEventResponse {
        public BigInteger paymentOrderId;

        public BigInteger invoiceId;

        public String customerRefNumber;
    }

    public static class ClaimCreatedEventResponse extends BaseEventResponse {
        public BigInteger claimId;

        public BigInteger projectId;

        public String contractorWallet;

        public BigInteger status;
    }

    public static class ClaimDocumentsUpdatedEventResponse extends BaseEventResponse {
        public BigInteger claimId;

        public BigInteger documentCount;
    }

    public static class ClaimStatusChangedEventResponse extends BaseEventResponse {
        public BigInteger claimId;

        public BigInteger status;
    }

    public static class InvoiceCreatedEventResponse extends BaseEventResponse {
        public BigInteger invoiceId;

        public BigInteger claimId;

        public BigInteger status;
    }

    public static class InvoiceDocumentsUpdatedEventResponse extends BaseEventResponse {
        public BigInteger invoiceId;

        public BigInteger documentCount;
    }

    public static class InvoiceStatusChangedEventResponse extends BaseEventResponse {
        public BigInteger invoiceId;

        public BigInteger status;
    }

    public static class PaymentCreatedForOrderEventResponse extends BaseEventResponse {
        public BigInteger paymentOrderId;

        public BigInteger paymentId;

        public BigInteger invoiceId;
    }

    public static class PaymentOrderCreatedEventResponse extends BaseEventResponse {
        public BigInteger paymentOrderId;

        public BigInteger invoiceId;

        public BigInteger status;
    }

    public static class PaymentOrderStatusChangedEventResponse extends BaseEventResponse {
        public BigInteger paymentOrderId;

        public BigInteger status;
    }

    public static class ProjectApproverRemovedEventResponse extends BaseEventResponse {
        public BigInteger projectId;

        public byte[] userHash;
    }

    public static class ProjectCreatedEventResponse extends BaseEventResponse {
        public BigInteger projectId;

        public String developerWallet;

        public String externalProjectId;
    }

    public static class ProjectStatusChangedEventResponse extends BaseEventResponse {
        public BigInteger projectId;

        public BigInteger status;
    }

    public static class ProjectUpdatedEventResponse extends BaseEventResponse {
        public BigInteger projectId;

        public String externalProjectId;
    }

    public static class RoleAdminChangedEventResponse extends BaseEventResponse {
        public byte[] role;

        public byte[] previousAdminRole;

        public byte[] newAdminRole;
    }

    public static class RoleGrantedEventResponse extends BaseEventResponse {
        public byte[] role;

        public String account;

        public String sender;
    }

    public static class RoleRevokedEventResponse extends BaseEventResponse {
        public byte[] role;

        public String account;

        public String sender;
    }
}
