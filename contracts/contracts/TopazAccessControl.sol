// SPDX-License-Identifier: MIT
pragma solidity ^0.8.24;

import {AccessControl} from "@openzeppelin/contracts/access/AccessControl.sol";

abstract contract TopazAccessControl is AccessControl {
    bytes32 public constant SUPER_ADMIN_ROLE = keccak256("SUPER_ADMIN_ROLE");
    bytes32 public constant ADMIN_ROLE = keccak256("ADMIN_ROLE");
    bytes32 public constant PROJECT_OFFICER_ROLE = keccak256("PROJECT_OFFICER_ROLE");
    bytes32 public constant FINANCE_ROLE = keccak256("FINANCE_ROLE");
    bytes32 public constant SETTLEMENT_BANK_ROLE = keccak256("SETTLEMENT_BANK_ROLE");

    error UnauthorizedCaller(address caller);

    constructor(address admin) {
        _grantRole(DEFAULT_ADMIN_ROLE, admin);
        _grantRole(SUPER_ADMIN_ROLE, admin);
        _grantRole(ADMIN_ROLE, admin);
    }

    modifier onlyAdminOrSuperAdmin() {
        if (!hasRole(ADMIN_ROLE, msg.sender) && !hasRole(SUPER_ADMIN_ROLE, msg.sender)) {
            revert UnauthorizedCaller(msg.sender);
        }
        _;
    }

    modifier onlyFinanceOrSettlementBank() {
        if (!hasRole(FINANCE_ROLE, msg.sender) && !hasRole(SETTLEMENT_BANK_ROLE, msg.sender)) {
            revert UnauthorizedCaller(msg.sender);
        }
        _;
    }
}

