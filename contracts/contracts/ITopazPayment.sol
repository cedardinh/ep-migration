// SPDX-License-Identifier: MIT
pragma solidity ^0.8.24;

import {TopazTypes} from "./TopazTypes.sol";

interface ITopazPayment {
    function createPayment(TopazTypes.PaymentRequest calldata request)
        external
        returns (uint256 paymentId);
}


