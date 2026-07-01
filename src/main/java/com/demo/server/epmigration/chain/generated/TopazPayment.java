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
import org.web3j.abi.FunctionEncoder;
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
import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameter;
import org.web3j.protocol.core.RemoteCall;
import org.web3j.protocol.core.RemoteFunctionCall;
import org.web3j.protocol.core.methods.request.EthFilter;
import org.web3j.protocol.core.methods.response.BaseEventResponse;
import org.web3j.protocol.core.methods.response.Log;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.tuples.generated.Tuple10;
import org.web3j.tuples.generated.Tuple7;
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
public class TopazPayment extends Contract {
    public static final String BINARY = "60803461007357601f62001f0a38819003918201601f19168301916001600160401b038311848410176100785780849260209460405283398101031261007357516001600160a01b0381168103610073576100629060018055600160025561008e565b50604051611deb90816200011f8239f35b600080fd5b634e487b7160e01b600052604160045260246000fd5b6001600160a01b031660008181527fad3228b676f7d3cd4284a5443f17f1962b36e491b30a40b2405849e597ba5fb5602052604081205490919060ff1661011a57818052816020526040822081835260205260408220600160ff1982541617905533917f2f8788117e7eff1d82e926ec794901d17c78024a50270940304540a733656f0d8180a4600190565b509056fe608080604052600436101561001357600080fd5b60003560e01c90816301ffc9a7146115b8575080630fb20ee7146113a45780631e4e0091146112f7578063248a9ca3146112c85780632f2ff15d1461129757806336568abe1461125e57806341d6f699146112325780635364221514610d8b5780638757a65514610d5f578063880d781114610c4257806391d1485414610c02578063a217fddf14610be6578063a5a5540a14610ad3578063c9d0d06114610a98578063cc44d267146107af578063d547741f1461077c578063e3c8df76146101255763fb78a520146100e557600080fd5b346101205760003660031901126101205760206040517f13598b09680b3f4eed2c949bfaf87416b54347f92976ed5924092b25ee2737328152f35b600080fd5b3461012057600319602036820112610120576001600160401b0360043511610120576102009060043536030112610120573360009081527fce7826a13a6e01f8ca132af1545f75d5d6f777cb620fbac4cd7597f6f4e0749b60205260409020547f41ac124fbf89049ac777c15f0cfc3c622029726a2d75350d07277b4060edf8699060ff161561075e5760043560040135156107185760246004350135156106da5760446001600160a01b03806101eb6101e6600480358087019101611941565b611b50565b1615610695576102086101e6606460043501600435600401611941565b16156106505760e46004350135156105fc5761022e60c460043501600435600401611671565b9050156105b75761024a61010460043501600435600401611671565b90501561057257600435600401356000526004602052604060002054610555576020906104a960246001549261027f84611868565b60015583600052600385526104096104006040600020926103f1602d8501600160801b60ff60801b1982541617815588865560043560040135600187015586600435013560028701556102e96102df60c460043501600435600401611671565b90601d89016116f4565b60e46004350135601e87015561031761030d61010460043501600435600401611671565b90601f89016116f4565b61033861032f61012460043501600435600401611671565b908c89016116f4565b61035a61035061016460043501600435600401611671565b90602289016116f4565b6103696101846004350161180b565b90602387016001600160401b0380199316838254161790556103a36103996101c460043501600435600401611671565b9060258a016116f4565b6103c56103bb6101e460043501600435600401611671565b9060268a016116f4565b602b8701600160401b60ff60401b198254161790556001600160401b034216809282541617815561181f565b60043501600435600401611941565b60038301611b64565b610429610420606460043501600435600401611941565b600a8301611b64565b610449610440608460043501600435600401611956565b60118301611c20565b61046961046060a460043501600435600401611956565b60178301611c20565b61048b6104816101446004350160043560040161196b565b9060218401611cb6565b6104a06101a46004350160043560040161196b565b92909101611cb6565b6004356004013560005260048252806040600020556104d260c460043501600435600401611671565b827f0f84d7eca8f9eaabbe334299276eb627d1f0290e895937888ebd4268b487dee961050961010460043501600435600401611671565b94909361054a610526604051938493606085526060850191611847565b6004803560e4810135858d0152848303604086015260248101359991013597611847565b0390a4604051908152f35b6024604051638fb0366160e01b8152600435600401356004820152fd5b6064907f696e737472756374656443757272656e637920697320726571756972656400006040519163d647364f60e01b835260206004840152601e6024840152820152fd5b6064907f637573746f6d65725265664e756d6265722069732072657175697265640000006040519163d647364f60e01b835260206004840152601d6024840152820152fd5b6084907f696e7374727563746564416d6f756e744d696e6f72206d75737420626520706f6040519163d647364f60e01b835260206004840152602660248401528201526573697469766560d01b6064820152fd5b6064907f70617965652077616c6c657420697320726571756972656400000000000000006040519163d647364f60e01b83526020600484015260186024840152820152fd5b60405163d647364f60e01b815260206004820152601860248201527f70617965722077616c6c6574206973207265717569726564000000000000000081840152606490fd5b60405163d647364f60e01b81526020600482015260156024820152741a5b9d9bda58d95259081a5cc81c995c5d5a5c9959605a1b6044820152606490fd5b60405163d647364f60e01b815260206004820152601a60248201527f7061796d656e744f7264657249642069732072657175697265640000000000006044820152606490fd5b6044906040519063e2517d3f60e01b82523360048301526024820152fd5b34610120576107ad61078d3661160b565b908060005260006020526107a86001604060002001546119fc565b611adb565b005b346101205760206003198181360112610120576004356001600160401b039182821161012057608082600401918336030112610120576107ed6119a0565b8035918215610a5a57602494858201946108078685611671565b905015610a1c57604483019261081d8486611671565b9050156109d75760640190806108328361180b565b16156109995761084186611a22565b91602b83019060ff825460401c16600481101561098457600103610935576001846108fa8561090b95680300000000000000008b9a988f610903987f7b754cdddc047d0cf3cc2cc8a82a299a84f8b393eb55bb0abb2af880343c72c49f9e8f6108d7916109309f916108c860296108cd946108bf6108dc9985611671565b929091016116f4565b611671565b90602a8b016116f4565b61180b565b1668ffffffffffffffffff19835416171790554216602d830161181f565b01549886611671565b949095611671565b90610923604051968796604088526040880191611847565b9285840390860152611847565b0390a3005b604051631186d0b760e31b8152600481018690526021818c01527f7061796d656e74206973206e6f74206177616974696e672072656a656374696f6044820152603760f91b6064820152608490fd5b8a634e487b7160e01b60005260216004526000fd5b60405163d647364f60e01b8152600481018490526016818a0152751c995a9958dd11185d19481a5cc81c995c5d5a5c995960521b6044820152606490fd5b60405163d647364f60e01b8152600481018490526018818a01527f72656a656374526561736f6e20697320726571756972656400000000000000006044820152606490fd5b60405163d647364f60e01b815260048101839052601681890152751c995a9958dd10dbd919481a5cc81c995c5d5a5c995960521b6044820152606490fd5b60405163d647364f60e01b81526020600482015260156024820152741c185e5b595b9d1259081a5cc81c995c5d5a5c9959605a1b6044820152606490fd5b346101205760003660031901126101205760206040517f41ac124fbf89049ac777c15f0cfc3c622029726a2d75350d07277b4060edf8698152f35b346101205760203660031901126101205760043580600052600560205260406000209060ff600883015460401c1615610bce57602082610bca600182015491610bbc610b216002830161188d565b91610bae610b316003830161188d565b610ba0610b406004850161188d565b610b92610b4f6005870161188d565b93610b84610b6b6007610b6460068b0161188d565b990161188d565b9960e06040519e8f9e8f908152015260e08d0190611631565b908b820360408d0152611631565b9089820360608b0152611631565b908782036080890152611631565b9085820360a0870152611631565b9083820360c0850152611631565b0390f35b602490604051906333ebb4a760e11b82526004820152fd5b3461012057600036600319011261012057602060405160008152f35b3461012057610c103661160b565b90600052600060205260406000209060018060a01b0316600052602052602060ff604060002054166040519015158152f35b3461012057602036600319011261012057610c5e600435611a22565b600181015460028201549160ff602b82015460401c1690601e81015492602c82015492610c8d601d840161188d565b92610c9a601f820161188d565b93610ca76027830161188d565b610cbf602a610cb86029860161188d565b940161188d565b9460405199610140918b5260208b01526004851015610d49578998610d3e97610d00610d219584610d139560608f610d2f9c604082015201528d0190611631565b9160808c01528a820360a08c0152611631565b9088820360c08a0152611631565b9086820360e0880152611631565b90848203610100860152611631565b906101208301520390f35b634e487b7160e01b600052602160045260246000fd5b346101205760203660031901126101205760043560005260046020526020604060002054604051908152f35b3461012057600319602036820112610120576001600160401b03600435116101205760e0906004353603011261012057610dc36119a0565b6004356004013515610a5a576024610de48160043501600435600401611671565b9050156111ed576044610e008160043501600435600401611671565b9050156111a957610e1b606460043501600435600401611671565b90501561116557610e36608460043501600435600401611671565b90501561112157610e5160c460043501600435600401611671565b9050156110e557610e6760043560040135611a22565b9060ff602b83015460401c1660048110156110d057600281141590816110c4575b5061105a5760043560040135600052600660205260406000205461103e579161054a610f607f4b448b3644167885a1b1bd5f282a6e033563c7ae9240e6ac74a650a23d50b8216110246001610f6a96610ffd6001600160401b0360209a610fe160078b6002549d8e9b8c95610efc87611868565b60025586600052602060059052610f51610f476040600020956008870199600160401b60ff60401b198c5416178b558755600435600401358e88015560043501600435600401611671565b90600287016116f4565b60043501600435600401611671565b90600384016116f4565b610f8b610f81606460043501600435600401611671565b90600484016116f4565b610fac610fa2608460043501600435600401611671565b90600584016116f4565b610fcd610fc360a460043501600435600401611671565b90600684016116f4565b6108bf60c460043501600435600401611671565b814216821982541617815587602c8501555416602d830161181f565b60043560040135600052600689528460406000205501549460043501600435600401611671565b60405188815260048035013595909283928a840191611847565b826040516395ac6bc360e01b8152600435600401356004820152fd5b827f7061796d656e74206d757374206265206163636570746564206f722072656a65608492603c60405193631186d0b760e31b8552602060048601528401528201527f63746564206265666f72652072656365697074206372656174696f6e000000006064820152fd5b60039150141584610e88565b83634e487b7160e01b60005260216004526000fd5b90741d985b1d5951185d19481a5cc81c995c5d5a5c9959605a1b60649260156040519363d647364f60e01b855260206004860152840152820152fd5b907f6f72646572696e67496e737469747574696f6e20697320726571756972656400606492601f6040519363d647364f60e01b855260206004860152840152820152fd5b907f6f72646572696e67437573746f6d657220697320726571756972656400000000606492601c6040519363d647364f60e01b855260206004860152840152820152fd5b907f72656c617465645265666572656e636520697320726571756972656400000000606492601c6040519363d647364f60e01b855260206004860152840152820152fd5b606490601d6040519163d647364f60e01b8352602060048401528201527f7472616e73616374696f6e5265664e756d2069732072657175697265640000006044820152fd5b346101205760203660031901126101205760043560005260066020526020604060002054604051908152f35b346101205761126c3661160b565b336001600160a01b03821603611285576107ad91611adb565b60405163334bd91960e11b8152600490fd5b34610120576107ad6112a83661160b565b908060005260006020526112c36001604060002001546119fc565b611a5d565b346101205760203660031901126101205760043560005260006020526020600160406000200154604051908152f35b34610120576040366003190112610120573360009081527fad3228b676f7d3cd4284a5443f17f1962b36e491b30a40b2405849e597ba5fb56020526040902054602435906004359060ff16156113855780600052600060205260016040600020019082825492557fbd79b86ffe0ab8e8776151514217cd7cacd52c909f66475c3af44e129f0b00ff600080a4005b60405163e2517d3f60e01b815233600482015260006024820152604490fd5b346101205760031960203682018113610120576001600160401b039060043582811161012057606081600401948236030112610120576113e26119a0565b8335928315610a5a5760248201916113fa8387611671565b90501561157257604401908061140f8361180b565b161561152c5761141e85611a22565b91602b83019060ff825460401c166004811015610d49576001036114db577fb7b13c8b893ad8ee055e60eb3e9018cd57d94eca407195f90656bd733a0822bf95936114c06114c89487946114896001956108d761147f8f9c6109309d611671565b90602789016116f4565b826028860191166001600160401b03198254161790556802000000000000000060ff60401b198254161790554216602d830161181f565b015496611671565b9290604051938385948552840191611847565b604051631186d0b760e31b815260048101879052602260248201527f7061796d656e74206973206e6f74206177616974696e6720616363657074616e604482015261636560f01b6064820152608490fd5b60405163d647364f60e01b815260048101859052601a60248201527f736574746c656d656e74446174652069732072657175697265640000000000006044820152606490fd5b60405163d647364f60e01b815260048101859052601d60248201527f736574746c656d656e7442616e6b5265662069732072657175697265640000006044820152606490fd5b34610120576020366003190112610120576004359063ffffffff60e01b821680920361012057602091637965db0b60e01b81149081156115fa575b5015158152f35b6301ffc9a760e01b149050836115f3565b604090600319011261012057600435906024356001600160a01b03811681036101205790565b919082519283825260005b84811061165d575050826000602080949584010152601f8019910116010190565b60208183018101518483018201520161163c565b903590601e198136030182121561012057018035906001600160401b0382116101205760200191813603831361012057565b90600182811c921680156116d3575b60208310146116bd57565b634e487b7160e01b600052602260045260246000fd5b91607f16916116b2565b8181106116e8575050565b600081556001016116dd565b9092916001600160401b0381116117f55761170f82546116a3565b601f81116117b8575b506000601f82116001146117535781929394600092611748575b50508160011b916000199060031b1c1916179055565b013590503880611732565b601f19821694838252602091602081209281905b8882106117a057505083600195969710611786575b505050811b019055565b0135600019600384901b60f8161c1916905538808061177c565b80600184968294958701358155019501920190611767565b6117e590836000526020600020601f840160051c810191602085106117eb575b601f0160051c01906116dd565b38611718565b90915081906117d8565b634e487b7160e01b600052604160045260246000fd5b356001600160401b03811681036101205790565b9067ffffffffffffffff60401b82549160401b169067ffffffffffffffff60401b1916179055565b908060209392818452848401376000828201840152601f01601f1916010190565b60001981146118775760010190565b634e487b7160e01b600052601160045260246000fd5b906040519160009080546118a0816116a3565b9081865260209260019160018116908160001461192357506001146118e7575b50505050829003601f01601f191682016001600160401b038111838210176117f557604052565b909293506000528260002091836000935b83851061190f5750505050830101388080806118c0565b8054888601830152930192849082016118f8565b60ff191688860152505050151560051b8401019050388080806118c0565b90359060de1981360301821215610120570190565b90359060be1981360301821215610120570190565b903590601e198136030182121561012057018035906001600160401b03821161012057602001918160051b3603831361012057565b3360009081527f6fd1d4b849b6c0189a07a7d6b18bc50d418a483f1f3dbd1c204a4a4df3451c6c60205260409020547f13598b09680b3f4eed2c949bfaf87416b54347f92976ed5924092b25ee2737329060ff161561075e5750565b80600052600060205260406000203360005260205260ff604060002054161561075e5750565b9081600052600360205260406000209160ff602d84015460801c1615611a455750565b602490604051906338e2cedd60e11b82526004820152fd5b9060009180835282602052604083209160018060a01b03169182845260205260ff60408420541615600014611ad657808352826020526040832082845260205260408320600160ff198254161790557f2f8788117e7eff1d82e926ec794901d17c78024a50270940304540a733656f0d339380a4600190565b505090565b9060009180835282602052604083209160018060a01b03169182845260205260ff604084205416600014611ad65780835282602052604083208284526020526040832060ff1981541690557ff6391f5c32d9c69d2a47ea670b442974b53935d1edc7fd64eb21e047a839171b339380a4600190565b356001600160a01b03811681036101205790565b611c1e91600691906108bf906001600160a01b03611b8182611b50565b84546001600160a01b0319169116178355611bac611ba26020830183611671565b90600186016116f4565b611bc6611bbc6040830183611671565b90600286016116f4565b611be0611bd66060830183611671565b90600386016116f4565b611bfa611bf06080830183611671565b90600486016116f4565b611c14611c0a60a0830183611671565b90600586016116f4565b60c0810190611671565b565b6005906108bf611c1e93611c3e611c378280611671565b90856116f4565b611c4e611ba26020830183611671565b611c5e611bbc6040830183611671565b611c6e611bd66060830183611671565b611c7e611bf06080830183611671565b60a0810190611671565b8054821015611ca05760005260206000200190600090565b634e487b7160e01b600052603260045260246000fd5b9290915b83548015611d425760001901611cd08186611c88565b611d2e57611cde81546116a3565b80611ced575b50508455611cba565b601f808211600114611d07575050600090555b3880611ce4565b611d2792600093849282845260208420940160051c8401600185016116dd565b5555611d00565b60246000634e487b7160e01b815280600452fd5b5090929160005b828110611d565750505050565b611d658160051b830183611671565b8592919254600160401b8110156117f557806001611d869201885587611c88565b919091611d9f57600193611d99926116f4565b01611d49565b634e487b7160e01b600052600060045260246000fdfea2646970667358221220b93254047709d8ba8b769ab228bc6f0bca67e85228681febf53a690d110c9ad964736f6c63430008180033";

    public static final String FUNC_DEFAULT_ADMIN_ROLE = "DEFAULT_ADMIN_ROLE";

    public static final String FUNC_LIFECYCLE_ROLE = "LIFECYCLE_ROLE";

    public static final String FUNC_PAYMENT_OPERATOR_ROLE = "PAYMENT_OPERATOR_ROLE";

    public static final String FUNC_ACCEPTPAYMENT = "acceptPayment";

    public static final String FUNC_CREATEPAYMENT = "createPayment";

    public static final String FUNC_CREATEPAYMENTRECEIPT = "createPaymentReceipt";

    public static final String FUNC_GETPAYMENTIDBYPAYMENTORDERID = "getPaymentIdByPaymentOrderId";

    public static final String FUNC_GETPAYMENTRECEIPTIDBYPAYMENTID = "getPaymentReceiptIdByPaymentId";

    public static final String FUNC_GETPAYMENTRECEIPTSUMMARY = "getPaymentReceiptSummary";

    public static final String FUNC_GETPAYMENTSUMMARY = "getPaymentSummary";

    public static final String FUNC_GETROLEADMIN = "getRoleAdmin";

    public static final String FUNC_GRANTROLE = "grantRole";

    public static final String FUNC_HASROLE = "hasRole";

    public static final String FUNC_REJECTPAYMENT = "rejectPayment";

    public static final String FUNC_RENOUNCEROLE = "renounceRole";

    public static final String FUNC_REVOKEROLE = "revokeRole";

    public static final String FUNC_SETROLEADMIN = "setRoleAdmin";

    public static final String FUNC_SUPPORTSINTERFACE = "supportsInterface";

    public static final Event PAYMENTACCEPTED_EVENT = new Event("PaymentAccepted", 
            Arrays.<TypeReference<?>>asList(new TypeReference<Uint256>(true) {}, new TypeReference<Uint256>(true) {}, new TypeReference<Utf8String>() {}));
    ;

    public static final Event PAYMENTCREATED_EVENT = new Event("PaymentCreated", 
            Arrays.<TypeReference<?>>asList(new TypeReference<Uint256>(true) {}, new TypeReference<Uint256>(true) {}, new TypeReference<Uint256>(true) {}, new TypeReference<Utf8String>() {}, new TypeReference<Uint256>() {}, new TypeReference<Utf8String>() {}));
    ;

    public static final Event PAYMENTRECEIPTCREATED_EVENT = new Event("PaymentReceiptCreated", 
            Arrays.<TypeReference<?>>asList(new TypeReference<Uint256>(true) {}, new TypeReference<Uint256>(true) {}, new TypeReference<Uint256>(true) {}, new TypeReference<Utf8String>() {}));
    ;

    public static final Event PAYMENTREJECTED_EVENT = new Event("PaymentRejected", 
            Arrays.<TypeReference<?>>asList(new TypeReference<Uint256>(true) {}, new TypeReference<Uint256>(true) {}, new TypeReference<Utf8String>() {}, new TypeReference<Utf8String>() {}));
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
    protected TopazPayment(String contractAddress, Web3j web3j, Credentials credentials, BigInteger gasPrice, BigInteger gasLimit) {
        super(BINARY, contractAddress, web3j, credentials, gasPrice, gasLimit);
    }

    protected TopazPayment(String contractAddress, Web3j web3j, Credentials credentials, ContractGasProvider contractGasProvider) {
        super(BINARY, contractAddress, web3j, credentials, contractGasProvider);
    }

    @Deprecated
    protected TopazPayment(String contractAddress, Web3j web3j, TransactionManager transactionManager, BigInteger gasPrice, BigInteger gasLimit) {
        super(BINARY, contractAddress, web3j, transactionManager, gasPrice, gasLimit);
    }

    protected TopazPayment(String contractAddress, Web3j web3j, TransactionManager transactionManager, ContractGasProvider contractGasProvider) {
        super(BINARY, contractAddress, web3j, transactionManager, contractGasProvider);
    }

    public static List<PaymentAcceptedEventResponse> getPaymentAcceptedEvents(TransactionReceipt transactionReceipt) {
        List<Contract.EventValuesWithLog> valueList = staticExtractEventParametersWithLog(PAYMENTACCEPTED_EVENT, transactionReceipt);
        ArrayList<PaymentAcceptedEventResponse> responses = new ArrayList<PaymentAcceptedEventResponse>(valueList.size());
        for (Contract.EventValuesWithLog eventValues : valueList) {
            PaymentAcceptedEventResponse typedResponse = new PaymentAcceptedEventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse.paymentId = (BigInteger) eventValues.getIndexedValues().get(0).getValue();
            typedResponse.paymentOrderId = (BigInteger) eventValues.getIndexedValues().get(1).getValue();
            typedResponse.settlementBankRef = (String) eventValues.getNonIndexedValues().get(0).getValue();
            responses.add(typedResponse);
        }
        return responses;
    }

    public static PaymentAcceptedEventResponse getPaymentAcceptedEventFromLog(Log log) {
        Contract.EventValuesWithLog eventValues = staticExtractEventParametersWithLog(PAYMENTACCEPTED_EVENT, log);
        PaymentAcceptedEventResponse typedResponse = new PaymentAcceptedEventResponse();
        typedResponse.log = log;
        typedResponse.paymentId = (BigInteger) eventValues.getIndexedValues().get(0).getValue();
        typedResponse.paymentOrderId = (BigInteger) eventValues.getIndexedValues().get(1).getValue();
        typedResponse.settlementBankRef = (String) eventValues.getNonIndexedValues().get(0).getValue();
        return typedResponse;
    }

    public Flowable<PaymentAcceptedEventResponse> paymentAcceptedEventFlowable(EthFilter filter) {
        return web3j.ethLogFlowable(filter).map(log -> getPaymentAcceptedEventFromLog(log));
    }

    public Flowable<PaymentAcceptedEventResponse> paymentAcceptedEventFlowable(DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(PAYMENTACCEPTED_EVENT));
        return paymentAcceptedEventFlowable(filter);
    }

    public static List<PaymentCreatedEventResponse> getPaymentCreatedEvents(TransactionReceipt transactionReceipt) {
        List<Contract.EventValuesWithLog> valueList = staticExtractEventParametersWithLog(PAYMENTCREATED_EVENT, transactionReceipt);
        ArrayList<PaymentCreatedEventResponse> responses = new ArrayList<PaymentCreatedEventResponse>(valueList.size());
        for (Contract.EventValuesWithLog eventValues : valueList) {
            PaymentCreatedEventResponse typedResponse = new PaymentCreatedEventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse.paymentId = (BigInteger) eventValues.getIndexedValues().get(0).getValue();
            typedResponse.paymentOrderId = (BigInteger) eventValues.getIndexedValues().get(1).getValue();
            typedResponse.invoiceId = (BigInteger) eventValues.getIndexedValues().get(2).getValue();
            typedResponse.customerRefNumber = (String) eventValues.getNonIndexedValues().get(0).getValue();
            typedResponse.instructedAmountMinor = (BigInteger) eventValues.getNonIndexedValues().get(1).getValue();
            typedResponse.instructedCurrency = (String) eventValues.getNonIndexedValues().get(2).getValue();
            responses.add(typedResponse);
        }
        return responses;
    }

    public static PaymentCreatedEventResponse getPaymentCreatedEventFromLog(Log log) {
        Contract.EventValuesWithLog eventValues = staticExtractEventParametersWithLog(PAYMENTCREATED_EVENT, log);
        PaymentCreatedEventResponse typedResponse = new PaymentCreatedEventResponse();
        typedResponse.log = log;
        typedResponse.paymentId = (BigInteger) eventValues.getIndexedValues().get(0).getValue();
        typedResponse.paymentOrderId = (BigInteger) eventValues.getIndexedValues().get(1).getValue();
        typedResponse.invoiceId = (BigInteger) eventValues.getIndexedValues().get(2).getValue();
        typedResponse.customerRefNumber = (String) eventValues.getNonIndexedValues().get(0).getValue();
        typedResponse.instructedAmountMinor = (BigInteger) eventValues.getNonIndexedValues().get(1).getValue();
        typedResponse.instructedCurrency = (String) eventValues.getNonIndexedValues().get(2).getValue();
        return typedResponse;
    }

    public Flowable<PaymentCreatedEventResponse> paymentCreatedEventFlowable(EthFilter filter) {
        return web3j.ethLogFlowable(filter).map(log -> getPaymentCreatedEventFromLog(log));
    }

    public Flowable<PaymentCreatedEventResponse> paymentCreatedEventFlowable(DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(PAYMENTCREATED_EVENT));
        return paymentCreatedEventFlowable(filter);
    }

    public static List<PaymentReceiptCreatedEventResponse> getPaymentReceiptCreatedEvents(TransactionReceipt transactionReceipt) {
        List<Contract.EventValuesWithLog> valueList = staticExtractEventParametersWithLog(PAYMENTRECEIPTCREATED_EVENT, transactionReceipt);
        ArrayList<PaymentReceiptCreatedEventResponse> responses = new ArrayList<PaymentReceiptCreatedEventResponse>(valueList.size());
        for (Contract.EventValuesWithLog eventValues : valueList) {
            PaymentReceiptCreatedEventResponse typedResponse = new PaymentReceiptCreatedEventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse.paymentReceiptId = (BigInteger) eventValues.getIndexedValues().get(0).getValue();
            typedResponse.paymentId = (BigInteger) eventValues.getIndexedValues().get(1).getValue();
            typedResponse.paymentOrderId = (BigInteger) eventValues.getIndexedValues().get(2).getValue();
            typedResponse.transactionRefNum = (String) eventValues.getNonIndexedValues().get(0).getValue();
            responses.add(typedResponse);
        }
        return responses;
    }

    public static PaymentReceiptCreatedEventResponse getPaymentReceiptCreatedEventFromLog(Log log) {
        Contract.EventValuesWithLog eventValues = staticExtractEventParametersWithLog(PAYMENTRECEIPTCREATED_EVENT, log);
        PaymentReceiptCreatedEventResponse typedResponse = new PaymentReceiptCreatedEventResponse();
        typedResponse.log = log;
        typedResponse.paymentReceiptId = (BigInteger) eventValues.getIndexedValues().get(0).getValue();
        typedResponse.paymentId = (BigInteger) eventValues.getIndexedValues().get(1).getValue();
        typedResponse.paymentOrderId = (BigInteger) eventValues.getIndexedValues().get(2).getValue();
        typedResponse.transactionRefNum = (String) eventValues.getNonIndexedValues().get(0).getValue();
        return typedResponse;
    }

    public Flowable<PaymentReceiptCreatedEventResponse> paymentReceiptCreatedEventFlowable(EthFilter filter) {
        return web3j.ethLogFlowable(filter).map(log -> getPaymentReceiptCreatedEventFromLog(log));
    }

    public Flowable<PaymentReceiptCreatedEventResponse> paymentReceiptCreatedEventFlowable(DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(PAYMENTRECEIPTCREATED_EVENT));
        return paymentReceiptCreatedEventFlowable(filter);
    }

    public static List<PaymentRejectedEventResponse> getPaymentRejectedEvents(TransactionReceipt transactionReceipt) {
        List<Contract.EventValuesWithLog> valueList = staticExtractEventParametersWithLog(PAYMENTREJECTED_EVENT, transactionReceipt);
        ArrayList<PaymentRejectedEventResponse> responses = new ArrayList<PaymentRejectedEventResponse>(valueList.size());
        for (Contract.EventValuesWithLog eventValues : valueList) {
            PaymentRejectedEventResponse typedResponse = new PaymentRejectedEventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse.paymentId = (BigInteger) eventValues.getIndexedValues().get(0).getValue();
            typedResponse.paymentOrderId = (BigInteger) eventValues.getIndexedValues().get(1).getValue();
            typedResponse.rejectCode = (String) eventValues.getNonIndexedValues().get(0).getValue();
            typedResponse.rejectReason = (String) eventValues.getNonIndexedValues().get(1).getValue();
            responses.add(typedResponse);
        }
        return responses;
    }

    public static PaymentRejectedEventResponse getPaymentRejectedEventFromLog(Log log) {
        Contract.EventValuesWithLog eventValues = staticExtractEventParametersWithLog(PAYMENTREJECTED_EVENT, log);
        PaymentRejectedEventResponse typedResponse = new PaymentRejectedEventResponse();
        typedResponse.log = log;
        typedResponse.paymentId = (BigInteger) eventValues.getIndexedValues().get(0).getValue();
        typedResponse.paymentOrderId = (BigInteger) eventValues.getIndexedValues().get(1).getValue();
        typedResponse.rejectCode = (String) eventValues.getNonIndexedValues().get(0).getValue();
        typedResponse.rejectReason = (String) eventValues.getNonIndexedValues().get(1).getValue();
        return typedResponse;
    }

    public Flowable<PaymentRejectedEventResponse> paymentRejectedEventFlowable(EthFilter filter) {
        return web3j.ethLogFlowable(filter).map(log -> getPaymentRejectedEventFromLog(log));
    }

    public Flowable<PaymentRejectedEventResponse> paymentRejectedEventFlowable(DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(PAYMENTREJECTED_EVENT));
        return paymentRejectedEventFlowable(filter);
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

    public RemoteFunctionCall<byte[]> DEFAULT_ADMIN_ROLE() {
        final Function function = new Function(FUNC_DEFAULT_ADMIN_ROLE, 
                Arrays.<Type>asList(), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Bytes32>() {}));
        return executeRemoteCallSingleValueReturn(function, byte[].class);
    }

    public RemoteFunctionCall<byte[]> LIFECYCLE_ROLE() {
        final Function function = new Function(FUNC_LIFECYCLE_ROLE, 
                Arrays.<Type>asList(), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Bytes32>() {}));
        return executeRemoteCallSingleValueReturn(function, byte[].class);
    }

    public RemoteFunctionCall<byte[]> PAYMENT_OPERATOR_ROLE() {
        final Function function = new Function(FUNC_PAYMENT_OPERATOR_ROLE, 
                Arrays.<Type>asList(), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Bytes32>() {}));
        return executeRemoteCallSingleValueReturn(function, byte[].class);
    }

    public RemoteFunctionCall<TransactionReceipt> acceptPayment(PaymentAcceptance acceptance) {
        final Function function = new Function(
                FUNC_ACCEPTPAYMENT, 
                Arrays.<Type>asList(acceptance), 
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<TransactionReceipt> createPayment(PaymentRequest request) {
        final Function function = new Function(
                FUNC_CREATEPAYMENT, 
                Arrays.<Type>asList(request), 
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<TransactionReceipt> createPaymentReceipt(PaymentReceiptRequest request) {
        final Function function = new Function(
                FUNC_CREATEPAYMENTRECEIPT, 
                Arrays.<Type>asList(request), 
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<BigInteger> getPaymentIdByPaymentOrderId(BigInteger paymentOrderId) {
        final Function function = new Function(FUNC_GETPAYMENTIDBYPAYMENTORDERID, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Uint256(paymentOrderId)), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Uint256>() {}));
        return executeRemoteCallSingleValueReturn(function, BigInteger.class);
    }

    public RemoteFunctionCall<BigInteger> getPaymentReceiptIdByPaymentId(BigInteger paymentId) {
        final Function function = new Function(FUNC_GETPAYMENTRECEIPTIDBYPAYMENTID, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Uint256(paymentId)), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Uint256>() {}));
        return executeRemoteCallSingleValueReturn(function, BigInteger.class);
    }

    public RemoteFunctionCall<Tuple7<BigInteger, String, String, String, String, String, String>> getPaymentReceiptSummary(BigInteger paymentReceiptId) {
        final Function function = new Function(FUNC_GETPAYMENTRECEIPTSUMMARY, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Uint256(paymentReceiptId)), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Uint256>() {}, new TypeReference<Utf8String>() {}, new TypeReference<Utf8String>() {}, new TypeReference<Utf8String>() {}, new TypeReference<Utf8String>() {}, new TypeReference<Utf8String>() {}, new TypeReference<Utf8String>() {}));
        return new RemoteFunctionCall<Tuple7<BigInteger, String, String, String, String, String, String>>(function,
                new Callable<Tuple7<BigInteger, String, String, String, String, String, String>>() {
                    @Override
                    public Tuple7<BigInteger, String, String, String, String, String, String> call() throws Exception {
                        List<Type> results = executeCallMultipleValueReturn(function);
                        return new Tuple7<BigInteger, String, String, String, String, String, String>(
                                (BigInteger) results.get(0).getValue(), 
                                (String) results.get(1).getValue(), 
                                (String) results.get(2).getValue(), 
                                (String) results.get(3).getValue(), 
                                (String) results.get(4).getValue(), 
                                (String) results.get(5).getValue(), 
                                (String) results.get(6).getValue());
                    }
                });
    }

    public RemoteFunctionCall<Tuple10<BigInteger, BigInteger, BigInteger, String, BigInteger, String, String, String, String, BigInteger>> getPaymentSummary(BigInteger paymentId) {
        final Function function = new Function(FUNC_GETPAYMENTSUMMARY, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Uint256(paymentId)), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Uint256>() {}, new TypeReference<Uint256>() {}, new TypeReference<Uint8>() {}, new TypeReference<Utf8String>() {}, new TypeReference<Uint256>() {}, new TypeReference<Utf8String>() {}, new TypeReference<Utf8String>() {}, new TypeReference<Utf8String>() {}, new TypeReference<Utf8String>() {}, new TypeReference<Uint256>() {}));
        return new RemoteFunctionCall<Tuple10<BigInteger, BigInteger, BigInteger, String, BigInteger, String, String, String, String, BigInteger>>(function,
                new Callable<Tuple10<BigInteger, BigInteger, BigInteger, String, BigInteger, String, String, String, String, BigInteger>>() {
                    @Override
                    public Tuple10<BigInteger, BigInteger, BigInteger, String, BigInteger, String, String, String, String, BigInteger> call() throws Exception {
                        List<Type> results = executeCallMultipleValueReturn(function);
                        return new Tuple10<BigInteger, BigInteger, BigInteger, String, BigInteger, String, String, String, String, BigInteger>(
                                (BigInteger) results.get(0).getValue(), 
                                (BigInteger) results.get(1).getValue(), 
                                (BigInteger) results.get(2).getValue(), 
                                (String) results.get(3).getValue(), 
                                (BigInteger) results.get(4).getValue(), 
                                (String) results.get(5).getValue(), 
                                (String) results.get(6).getValue(), 
                                (String) results.get(7).getValue(), 
                                (String) results.get(8).getValue(), 
                                (BigInteger) results.get(9).getValue());
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

    public RemoteFunctionCall<TransactionReceipt> rejectPayment(PaymentRejection rejection) {
        final Function function = new Function(
                FUNC_REJECTPAYMENT, 
                Arrays.<Type>asList(rejection), 
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

    public RemoteFunctionCall<TransactionReceipt> revokeRole(byte[] role, String account) {
        final Function function = new Function(
                FUNC_REVOKEROLE, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Bytes32(role), 
                new org.web3j.abi.datatypes.Address(160, account)), 
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<TransactionReceipt> setRoleAdmin(byte[] role, byte[] adminRole) {
        final Function function = new Function(
                FUNC_SETROLEADMIN, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Bytes32(role), 
                new org.web3j.abi.datatypes.generated.Bytes32(adminRole)), 
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<Boolean> supportsInterface(byte[] interfaceId) {
        final Function function = new Function(FUNC_SUPPORTSINTERFACE, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Bytes4(interfaceId)), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Bool>() {}));
        return executeRemoteCallSingleValueReturn(function, Boolean.class);
    }

    @Deprecated
    public static TopazPayment load(String contractAddress, Web3j web3j, Credentials credentials, BigInteger gasPrice, BigInteger gasLimit) {
        return new TopazPayment(contractAddress, web3j, credentials, gasPrice, gasLimit);
    }

    @Deprecated
    public static TopazPayment load(String contractAddress, Web3j web3j, TransactionManager transactionManager, BigInteger gasPrice, BigInteger gasLimit) {
        return new TopazPayment(contractAddress, web3j, transactionManager, gasPrice, gasLimit);
    }

    public static TopazPayment load(String contractAddress, Web3j web3j, Credentials credentials, ContractGasProvider contractGasProvider) {
        return new TopazPayment(contractAddress, web3j, credentials, contractGasProvider);
    }

    public static TopazPayment load(String contractAddress, Web3j web3j, TransactionManager transactionManager, ContractGasProvider contractGasProvider) {
        return new TopazPayment(contractAddress, web3j, transactionManager, contractGasProvider);
    }

    public static RemoteCall<TopazPayment> deploy(Web3j web3j, Credentials credentials, ContractGasProvider contractGasProvider, String admin) {
        String encodedConstructor = FunctionEncoder.encodeConstructor(Arrays.<Type>asList(new org.web3j.abi.datatypes.Address(160, admin)));
        return deployRemoteCall(TopazPayment.class, web3j, credentials, contractGasProvider, BINARY, encodedConstructor);
    }

    public static RemoteCall<TopazPayment> deploy(Web3j web3j, TransactionManager transactionManager, ContractGasProvider contractGasProvider, String admin) {
        String encodedConstructor = FunctionEncoder.encodeConstructor(Arrays.<Type>asList(new org.web3j.abi.datatypes.Address(160, admin)));
        return deployRemoteCall(TopazPayment.class, web3j, transactionManager, contractGasProvider, BINARY, encodedConstructor);
    }

    @Deprecated
    public static RemoteCall<TopazPayment> deploy(Web3j web3j, Credentials credentials, BigInteger gasPrice, BigInteger gasLimit, String admin) {
        String encodedConstructor = FunctionEncoder.encodeConstructor(Arrays.<Type>asList(new org.web3j.abi.datatypes.Address(160, admin)));
        return deployRemoteCall(TopazPayment.class, web3j, credentials, gasPrice, gasLimit, BINARY, encodedConstructor);
    }

    @Deprecated
    public static RemoteCall<TopazPayment> deploy(Web3j web3j, TransactionManager transactionManager, BigInteger gasPrice, BigInteger gasLimit, String admin) {
        String encodedConstructor = FunctionEncoder.encodeConstructor(Arrays.<Type>asList(new org.web3j.abi.datatypes.Address(160, admin)));
        return deployRemoteCall(TopazPayment.class, web3j, transactionManager, gasPrice, gasLimit, BINARY, encodedConstructor);
    }

    public static class PaymentAcceptance extends DynamicStruct {
        public BigInteger paymentId;

        public String settlementBankRef;

        public BigInteger settlementDate;

        public PaymentAcceptance(BigInteger paymentId, String settlementBankRef, BigInteger settlementDate) {
            super(new org.web3j.abi.datatypes.generated.Uint256(paymentId), 
                    new org.web3j.abi.datatypes.Utf8String(settlementBankRef), 
                    new org.web3j.abi.datatypes.generated.Uint64(settlementDate));
            this.paymentId = paymentId;
            this.settlementBankRef = settlementBankRef;
            this.settlementDate = settlementDate;
        }

        public PaymentAcceptance(Uint256 paymentId, Utf8String settlementBankRef, Uint64 settlementDate) {
            super(paymentId, settlementBankRef, settlementDate);
            this.paymentId = paymentId.getValue();
            this.settlementBankRef = settlementBankRef.getValue();
            this.settlementDate = settlementDate.getValue();
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

    public static class PaymentReceiptRequest extends DynamicStruct {
        public BigInteger paymentId;

        public String transactionRefNum;

        public String relatedReference;

        public String orderingCustomer;

        public String orderingInstitution;

        public String remittanceInfo;

        public String valueDate;

        public PaymentReceiptRequest(BigInteger paymentId, String transactionRefNum, String relatedReference, String orderingCustomer, String orderingInstitution, String remittanceInfo, String valueDate) {
            super(new org.web3j.abi.datatypes.generated.Uint256(paymentId), 
                    new org.web3j.abi.datatypes.Utf8String(transactionRefNum), 
                    new org.web3j.abi.datatypes.Utf8String(relatedReference), 
                    new org.web3j.abi.datatypes.Utf8String(orderingCustomer), 
                    new org.web3j.abi.datatypes.Utf8String(orderingInstitution), 
                    new org.web3j.abi.datatypes.Utf8String(remittanceInfo), 
                    new org.web3j.abi.datatypes.Utf8String(valueDate));
            this.paymentId = paymentId;
            this.transactionRefNum = transactionRefNum;
            this.relatedReference = relatedReference;
            this.orderingCustomer = orderingCustomer;
            this.orderingInstitution = orderingInstitution;
            this.remittanceInfo = remittanceInfo;
            this.valueDate = valueDate;
        }

        public PaymentReceiptRequest(Uint256 paymentId, Utf8String transactionRefNum, Utf8String relatedReference, Utf8String orderingCustomer, Utf8String orderingInstitution, Utf8String remittanceInfo, Utf8String valueDate) {
            super(paymentId, transactionRefNum, relatedReference, orderingCustomer, orderingInstitution, remittanceInfo, valueDate);
            this.paymentId = paymentId.getValue();
            this.transactionRefNum = transactionRefNum.getValue();
            this.relatedReference = relatedReference.getValue();
            this.orderingCustomer = orderingCustomer.getValue();
            this.orderingInstitution = orderingInstitution.getValue();
            this.remittanceInfo = remittanceInfo.getValue();
            this.valueDate = valueDate.getValue();
        }
    }

    public static class PaymentRejection extends DynamicStruct {
        public BigInteger paymentId;

        public String rejectCode;

        public String rejectReason;

        public BigInteger rejectDate;

        public PaymentRejection(BigInteger paymentId, String rejectCode, String rejectReason, BigInteger rejectDate) {
            super(new org.web3j.abi.datatypes.generated.Uint256(paymentId), 
                    new org.web3j.abi.datatypes.Utf8String(rejectCode), 
                    new org.web3j.abi.datatypes.Utf8String(rejectReason), 
                    new org.web3j.abi.datatypes.generated.Uint64(rejectDate));
            this.paymentId = paymentId;
            this.rejectCode = rejectCode;
            this.rejectReason = rejectReason;
            this.rejectDate = rejectDate;
        }

        public PaymentRejection(Uint256 paymentId, Utf8String rejectCode, Utf8String rejectReason, Uint64 rejectDate) {
            super(paymentId, rejectCode, rejectReason, rejectDate);
            this.paymentId = paymentId.getValue();
            this.rejectCode = rejectCode.getValue();
            this.rejectReason = rejectReason.getValue();
            this.rejectDate = rejectDate.getValue();
        }
    }

    public static class PaymentRequest extends DynamicStruct {
        public BigInteger paymentOrderId;

        public BigInteger invoiceId;

        public Participant payer;

        public Participant payee;

        public AccountInfo fromAccount;

        public AccountInfo toAccount;

        public String customerRefNumber;

        public BigInteger instructedAmountMinor;

        public String instructedCurrency;

        public String chargeBearer;

        public List<String> remittanceInformation;

        public String purposeCode;

        public BigInteger valueDate;

        public List<String> bankInformation;

        public String paymentType;

        public String preparerRef;

        public PaymentRequest(BigInteger paymentOrderId, BigInteger invoiceId, Participant payer, Participant payee, AccountInfo fromAccount, AccountInfo toAccount, String customerRefNumber, BigInteger instructedAmountMinor, String instructedCurrency, String chargeBearer, List<String> remittanceInformation, String purposeCode, BigInteger valueDate, List<String> bankInformation, String paymentType, String preparerRef) {
            super(new org.web3j.abi.datatypes.generated.Uint256(paymentOrderId), 
                    new org.web3j.abi.datatypes.generated.Uint256(invoiceId), 
                    payer, 
                    payee, 
                    fromAccount, 
                    toAccount, 
                    new org.web3j.abi.datatypes.Utf8String(customerRefNumber), 
                    new org.web3j.abi.datatypes.generated.Uint256(instructedAmountMinor), 
                    new org.web3j.abi.datatypes.Utf8String(instructedCurrency), 
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
            this.invoiceId = invoiceId;
            this.payer = payer;
            this.payee = payee;
            this.fromAccount = fromAccount;
            this.toAccount = toAccount;
            this.customerRefNumber = customerRefNumber;
            this.instructedAmountMinor = instructedAmountMinor;
            this.instructedCurrency = instructedCurrency;
            this.chargeBearer = chargeBearer;
            this.remittanceInformation = remittanceInformation;
            this.purposeCode = purposeCode;
            this.valueDate = valueDate;
            this.bankInformation = bankInformation;
            this.paymentType = paymentType;
            this.preparerRef = preparerRef;
        }

        public PaymentRequest(Uint256 paymentOrderId, Uint256 invoiceId, Participant payer, Participant payee, AccountInfo fromAccount, AccountInfo toAccount, Utf8String customerRefNumber, Uint256 instructedAmountMinor, Utf8String instructedCurrency, Utf8String chargeBearer, DynamicArray<Utf8String> remittanceInformation, Utf8String purposeCode, Uint64 valueDate, DynamicArray<Utf8String> bankInformation, Utf8String paymentType, Utf8String preparerRef) {
            super(paymentOrderId, invoiceId, payer, payee, fromAccount, toAccount, customerRefNumber, instructedAmountMinor, instructedCurrency, chargeBearer, remittanceInformation, purposeCode, valueDate, bankInformation, paymentType, preparerRef);
            this.paymentOrderId = paymentOrderId.getValue();
            this.invoiceId = invoiceId.getValue();
            this.payer = payer;
            this.payee = payee;
            this.fromAccount = fromAccount;
            this.toAccount = toAccount;
            this.customerRefNumber = customerRefNumber.getValue();
            this.instructedAmountMinor = instructedAmountMinor.getValue();
            this.instructedCurrency = instructedCurrency.getValue();
            this.chargeBearer = chargeBearer.getValue();
            this.remittanceInformation = remittanceInformation.getValue().stream().map(v -> v.getValue()).collect(Collectors.toList());
            this.purposeCode = purposeCode.getValue();
            this.valueDate = valueDate.getValue();
            this.bankInformation = bankInformation.getValue().stream().map(v -> v.getValue()).collect(Collectors.toList());
            this.paymentType = paymentType.getValue();
            this.preparerRef = preparerRef.getValue();
        }
    }

    public static class PaymentAcceptedEventResponse extends BaseEventResponse {
        public BigInteger paymentId;

        public BigInteger paymentOrderId;

        public String settlementBankRef;
    }

    public static class PaymentCreatedEventResponse extends BaseEventResponse {
        public BigInteger paymentId;

        public BigInteger paymentOrderId;

        public BigInteger invoiceId;

        public String customerRefNumber;

        public BigInteger instructedAmountMinor;

        public String instructedCurrency;
    }

    public static class PaymentReceiptCreatedEventResponse extends BaseEventResponse {
        public BigInteger paymentReceiptId;

        public BigInteger paymentId;

        public BigInteger paymentOrderId;

        public String transactionRefNum;
    }

    public static class PaymentRejectedEventResponse extends BaseEventResponse {
        public BigInteger paymentId;

        public BigInteger paymentOrderId;

        public String rejectCode;

        public String rejectReason;
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
