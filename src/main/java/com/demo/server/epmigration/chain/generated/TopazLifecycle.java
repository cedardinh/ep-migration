package com.demo.server.epmigration.chain.generated;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.DynamicArray;
import org.web3j.abi.datatypes.DynamicStruct;
import org.web3j.abi.datatypes.Event;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.Type;
import org.web3j.abi.datatypes.Utf8String;
import org.web3j.abi.datatypes.generated.Bytes32;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.RemoteFunctionCall;
import org.web3j.protocol.core.methods.response.BaseEventResponse;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.tx.Contract;
import org.web3j.tx.TransactionManager;
import org.web3j.tx.gas.ContractGasProvider;

/**
 * Minimal TopazLifecycle binding for the createProject transaction path.
 *
 * <p>The project intentionally keeps only the ABI types and event required by
 * {@code TopazLifecycleClient#createProject}. Contract deployment and unrelated
 * lifecycle methods are outside this application's scope.</p>
 */
@SuppressWarnings("rawtypes")
public final class TopazLifecycle extends Contract {
    private static final String BINARY = "";
    private static final String FUNC_CREATE_PROJECT = "createProject";

    private static final Event PROJECT_CREATED_EVENT = new Event(
            "ProjectCreated",
            Arrays.<TypeReference<?>>asList(
                    new TypeReference<Uint256>(true) {},
                    new TypeReference<Utf8String>() {},
                    new TypeReference<Address>(true) {}
            )
    );

    private TopazLifecycle(
            String contractAddress,
            Web3j web3j,
            TransactionManager transactionManager,
            ContractGasProvider gasProvider
    ) {
        super(BINARY, contractAddress, web3j, transactionManager, gasProvider);
    }

    public static TopazLifecycle load(
            String contractAddress,
            Web3j web3j,
            TransactionManager transactionManager,
            ContractGasProvider gasProvider
    ) {
        return new TopazLifecycle(
                contractAddress,
                web3j,
                transactionManager,
                gasProvider
        );
    }

    public RemoteFunctionCall<TransactionReceipt> createProject(
            CreateProjectInput input
    ) {
        Function function = new Function(
                FUNC_CREATE_PROJECT,
                Arrays.<Type>asList(input),
                Collections.<TypeReference<?>>emptyList()
        );
        return executeRemoteCallTransaction(function);
    }

    public static List<ProjectCreatedEventResponse> getProjectCreatedEvents(
            TransactionReceipt transactionReceipt
    ) {
        List<EventValuesWithLog> values =
                staticExtractEventParametersWithLog(
                        PROJECT_CREATED_EVENT,
                        transactionReceipt
                );
        List<ProjectCreatedEventResponse> responses =
                new ArrayList<ProjectCreatedEventResponse>(values.size());
        for (EventValuesWithLog value : values) {
            ProjectCreatedEventResponse response =
                    new ProjectCreatedEventResponse();
            response.log = value.getLog();
            response.projectId = (BigInteger) value
                    .getIndexedValues()
                    .get(0)
                    .getValue();
            response.developerWallet = (String) value
                    .getIndexedValues()
                    .get(1)
                    .getValue();
            response.externalProjectId = (String) value
                    .getNonIndexedValues()
                    .get(0)
                    .getValue();
            responses.add(response);
        }
        return responses;
    }

    public static final class Participant extends DynamicStruct {
        public final String wallet;
        public final String legalName;
        public final String addressLine1;
        public final String addressLine2;
        public final String bic;
        public final String lei;
        public final String externalRef;

        public Participant(
                String wallet,
                String legalName,
                String addressLine1,
                String addressLine2,
                String bic,
                String lei,
                String externalRef
        ) {
            super(
                    new Address(160, wallet),
                    new Utf8String(legalName),
                    new Utf8String(addressLine1),
                    new Utf8String(addressLine2),
                    new Utf8String(bic),
                    new Utf8String(lei),
                    new Utf8String(externalRef)
            );
            this.wallet = wallet;
            this.legalName = legalName;
            this.addressLine1 = addressLine1;
            this.addressLine2 = addressLine2;
            this.bic = bic;
            this.lei = lei;
            this.externalRef = externalRef;
        }
    }

    public static final class ApproverConfig extends DynamicStruct {
        public final String wallet;
        public final byte[] userHash;
        public final String email;
        public final String firstName;
        public final String lastName;
        public final String userProfileName;
        public final String roleName;
        public final String externalRef;

        public ApproverConfig(
                String wallet,
                byte[] userHash,
                String email,
                String firstName,
                String lastName,
                String userProfileName,
                String roleName,
                String externalRef
        ) {
            super(
                    new Address(160, wallet),
                    new Bytes32(userHash),
                    new Utf8String(email),
                    new Utf8String(firstName),
                    new Utf8String(lastName),
                    new Utf8String(userProfileName),
                    new Utf8String(roleName),
                    new Utf8String(externalRef)
            );
            this.wallet = wallet;
            this.userHash = userHash;
            this.email = email;
            this.firstName = firstName;
            this.lastName = lastName;
            this.userProfileName = userProfileName;
            this.roleName = roleName;
            this.externalRef = externalRef;
        }
    }

    public static final class CreateProjectInput extends DynamicStruct {
        public final String externalProjectId;
        public final String name;
        public final Participant developer;
        public final List<Participant> mainContractors;
        public final List<ApproverConfig> claimApprovers;
        public final List<ApproverConfig> paymentApprovers;
        public final List<String> bankAccountRefs;

        public CreateProjectInput(
                String externalProjectId,
                String name,
                Participant developer,
                List<Participant> mainContractors,
                List<ApproverConfig> claimApprovers,
                List<ApproverConfig> paymentApprovers,
                List<String> bankAccountRefs
        ) {
            super(
                    new Utf8String(externalProjectId),
                    new Utf8String(name),
                    developer,
                    new DynamicArray<Participant>(
                            Participant.class,
                            mainContractors
                    ),
                    new DynamicArray<ApproverConfig>(
                            ApproverConfig.class,
                            claimApprovers
                    ),
                    new DynamicArray<ApproverConfig>(
                            ApproverConfig.class,
                            paymentApprovers
                    ),
                    new DynamicArray<Utf8String>(
                            Utf8String.class,
                            org.web3j.abi.Utils.typeMap(
                                    bankAccountRefs,
                                    Utf8String.class
                            )
                    )
            );
            this.externalProjectId = externalProjectId;
            this.name = name;
            this.developer = developer;
            this.mainContractors = mainContractors;
            this.claimApprovers = claimApprovers;
            this.paymentApprovers = paymentApprovers;
            this.bankAccountRefs = bankAccountRefs;
        }
    }

    public static final class ProjectCreatedEventResponse
            extends BaseEventResponse {
        public BigInteger projectId;
        public String developerWallet;
        public String externalProjectId;
    }
}
