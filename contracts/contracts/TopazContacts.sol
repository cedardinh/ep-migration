// SPDX-License-Identifier: MIT
pragma solidity ^0.8.24;

import {TopazAccessControl} from "./TopazAccessControl.sol";
import {TopazTypes} from "./TopazTypes.sol";

contract TopazContacts is TopazAccessControl {
    struct ContactRecord {
        uint256 contactId;
        address wallet;
        string party;
        string contactType;
        string name;
        string contactPerson;
        string contactEmail;
        string contactNumber;
        string location;
        string domain;
        string accountName;
        uint64 createdAt;
        uint64 updatedAt;
        bool active;
        bool exists;
    }

    error UnknownContact(uint256 contactId);
    error UnknownPartyAccount(string party, string accountName);
    error DuplicateAccountName(string accountName);
    error InvalidInput(string reason);
    error InvalidState(string reason);

    uint256 private _nextContactId = 1;
    mapping(uint256 => ContactRecord) private _contacts;
    mapping(bytes32 => uint256) private _contactIdByPartyAccount;
    mapping(bytes32 => uint256) private _contactIdByAccount;
    mapping(bytes32 => uint256[]) private _contactIdsByParty;
    mapping(bytes32 => mapping(uint256 => bool)) private _isPartyIndexMember;
    mapping(bytes32 => uint256[]) private _contactIdsByType;
    mapping(bytes32 => mapping(uint256 => bool)) private _isTypeIndexMember;

    event ContactUpserted(
        uint256 indexed contactId,
        address indexed wallet,
        string party,
        string accountName,
        string contactType,
        bool created,
        bool active
    );

    event ContactDeactivated(uint256 indexed contactId, address indexed wallet, string party, string accountName);

    constructor(address admin) TopazAccessControl(admin) {}

    function upsertContact(TopazTypes.ContactInput calldata input)
        external
        onlyAdminOrSuperAdmin
        returns (uint256 contactId)
    {
        contactId = _upsertContact(input);
    }

    function batchUpsertContacts(TopazTypes.ContactInput[] calldata inputs)
        external
        onlyAdminOrSuperAdmin
        returns (uint256[] memory contactIds)
    {
        contactIds = new uint256[](inputs.length);
        for (uint256 i = 0; i < inputs.length; i++) {
            contactIds[i] = _upsertContact(inputs[i]);
        }
    }

    function deactivateContact(string calldata party, string calldata accountName)
        external
        onlyAdminOrSuperAdmin
    {
        bytes32 contactKey = _contactKey(party, accountName);
        uint256 contactId = _contactIdByPartyAccount[contactKey];
        if (contactId == 0) {
            revert UnknownPartyAccount(party, accountName);
        }

        ContactRecord storage contact = _contacts[contactId];
        if (!contact.active) {
            revert InvalidState("contact is already inactive");
        }

        contact.active = false;
        contact.updatedAt = _timestamp();

        emit ContactDeactivated(contactId, contact.wallet, contact.party, contact.accountName);
    }

    function getContactIdByPartyAccount(string calldata party, string calldata accountName)
        external
        view
        returns (uint256)
    {
        uint256 contactId = _contactIdByPartyAccount[_contactKey(party, accountName)];
        if (contactId == 0 || !_contacts[contactId].active) {
            return 0;
        }
        return contactId;
    }

    function getContactIdByAccount(string calldata accountName) external view returns (uint256) {
        uint256 contactId = _contactIdByAccount[_hashNormalized(accountName)];
        if (contactId == 0 || !_contacts[contactId].active) {
            return 0;
        }
        return contactId;
    }

    function getFirstActiveContactIdByParty(string calldata party) external view returns (uint256) {
        bytes32 partyKey = _hashNormalized(party);
        uint256[] storage contactIds = _contactIdsByParty[partyKey];
        for (uint256 i = 0; i < contactIds.length; i++) {
            ContactRecord storage contact = _contacts[contactIds[i]];
            if (contact.active && _hashNormalized(contact.party) == partyKey) {
                return contact.contactId;
            }
        }
        return 0;
    }

    function getActiveContactCountByParty(string calldata party) external view returns (uint256 count) {
        bytes32 partyKey = _hashNormalized(party);
        uint256[] storage contactIds = _contactIdsByParty[partyKey];
        count = 0;
        for (uint256 i = 0; i < contactIds.length; i++) {
            ContactRecord storage contact = _contacts[contactIds[i]];
            if (contact.active && _hashNormalized(contact.party) == partyKey) {
                count++;
            }
        }
        return count;
    }

    function getActiveContactIdByPartyAt(string calldata party, uint256 index) external view returns (uint256) {
        bytes32 partyKey = _hashNormalized(party);
        uint256[] storage contactIds = _contactIdsByParty[partyKey];
        uint256 currentIndex = 0;
        for (uint256 i = 0; i < contactIds.length; i++) {
            ContactRecord storage contact = _contacts[contactIds[i]];
            if (contact.active && _hashNormalized(contact.party) == partyKey) {
                if (currentIndex == index) {
                    return contact.contactId;
                }
                currentIndex++;
            }
        }
        revert InvalidInput("party contact index is out of bounds");
    }

    function getActiveContactCountByType(string calldata contactType) external view returns (uint256 count) {
        bytes32 typeKey = _hashNormalized(contactType);
        uint256[] storage contactIds = _contactIdsByType[typeKey];
        count = 0;
        for (uint256 i = 0; i < contactIds.length; i++) {
            ContactRecord storage contact = _contacts[contactIds[i]];
            if (contact.active && _hashNormalized(contact.contactType) == typeKey) {
                count++;
            }
        }
        return count;
    }

    function getActiveContactIdByTypeAt(string calldata contactType, uint256 index) external view returns (uint256) {
        bytes32 typeKey = _hashNormalized(contactType);
        uint256[] storage contactIds = _contactIdsByType[typeKey];
        uint256 currentIndex = 0;
        for (uint256 i = 0; i < contactIds.length; i++) {
            ContactRecord storage contact = _contacts[contactIds[i]];
            if (contact.active && _hashNormalized(contact.contactType) == typeKey) {
                if (currentIndex == index) {
                    return contact.contactId;
                }
                currentIndex++;
            }
        }
        revert InvalidInput("type contact index is out of bounds");
    }

    function getContactSummary(uint256 contactId)
        external
        view
        returns (
            address wallet,
            string memory party,
            string memory contactType,
            string memory name,
            string memory contactPerson,
            string memory contactEmail,
            string memory contactNumber,
            string memory location,
            string memory domain,
            string memory accountName,
            bool active,
            uint64 createdAt,
            uint64 updatedAt
        )
    {
        ContactRecord storage contact = _requireContact(contactId);
        return (
            contact.wallet,
            contact.party,
            contact.contactType,
            contact.name,
            contact.contactPerson,
            contact.contactEmail,
            contact.contactNumber,
            contact.location,
            contact.domain,
            contact.accountName,
            contact.active,
            contact.createdAt,
            contact.updatedAt
        );
    }

    function _upsertContact(TopazTypes.ContactInput calldata input) internal returns (uint256 contactId) {
        _validateInput(input);

        bytes32 partyKey = _hashNormalized(input.party);
        bytes32 typeKey = _hashNormalized(input.contactType);
        bytes32 contactKey = _contactKey(input.party, input.accountName);
        bytes32 accountKey = _hashNormalized(input.accountName);

        contactId = _contactIdByPartyAccount[contactKey];
        uint256 indexedAccountContactId = _contactIdByAccount[accountKey];
        if (indexedAccountContactId != 0 && indexedAccountContactId != contactId) {
            revert DuplicateAccountName(input.accountName);
        }

        bool created = contactId == 0;
        ContactRecord storage contact;
        if (created) {
            contactId = _nextContactId++;
            contact = _contacts[contactId];
            contact.contactId = contactId;
            contact.exists = true;
            contact.createdAt = _timestamp();
            _contactIdByPartyAccount[contactKey] = contactId;
            _contactIdByAccount[accountKey] = contactId;
            _addPartyIndex(partyKey, contactId);
        } else {
            contact = _requireContact(contactId);
        }

        contact.wallet = input.wallet;
        contact.party = input.party;
        contact.contactType = input.contactType;
        contact.name = input.name;
        contact.contactPerson = input.contactPerson;
        contact.contactEmail = input.contactEmail;
        contact.contactNumber = input.contactNumber;
        contact.location = input.location;
        contact.domain = input.domain;
        contact.accountName = input.accountName;
        contact.active = true;
        contact.updatedAt = _timestamp();

        _addTypeIndex(typeKey, contactId);

        emit ContactUpserted(contactId, input.wallet, input.party, input.accountName, input.contactType, created, true);
    }

    function _addPartyIndex(bytes32 partyKey, uint256 contactId) internal {
        if (_isPartyIndexMember[partyKey][contactId]) {
            return;
        }
        _isPartyIndexMember[partyKey][contactId] = true;
        _contactIdsByParty[partyKey].push(contactId);
    }

    function _addTypeIndex(bytes32 typeKey, uint256 contactId) internal {
        if (_isTypeIndexMember[typeKey][contactId]) {
            return;
        }
        _isTypeIndexMember[typeKey][contactId] = true;
        _contactIdsByType[typeKey].push(contactId);
    }

    function _requireContact(uint256 contactId) internal view returns (ContactRecord storage contact) {
        contact = _contacts[contactId];
        if (!contact.exists) {
            revert UnknownContact(contactId);
        }
    }

    function _validateInput(TopazTypes.ContactInput calldata input) internal pure {
        if (bytes(input.party).length == 0) {
            revert InvalidInput("party is required");
        }
        if (bytes(input.contactType).length == 0) {
            revert InvalidInput("contactType is required");
        }
        if (bytes(input.name).length == 0) {
            revert InvalidInput("name is required");
        }
        if (bytes(input.accountName).length == 0) {
            revert InvalidInput("accountName is required");
        }
    }

    function _contactKey(string memory party, string memory accountName) internal pure returns (bytes32) {
        return keccak256(abi.encodePacked(_toLower(party), "|", _toLower(accountName)));
    }

    function _hashNormalized(string memory value) internal pure returns (bytes32) {
        return keccak256(bytes(_toLower(value)));
    }

    function _toLower(string memory value) internal pure returns (string memory) {
        bytes memory buffer = bytes(value);
        for (uint256 i = 0; i < buffer.length; i++) {
            uint8 charCode = uint8(buffer[i]);
            if (charCode >= 65 && charCode <= 90) {
                buffer[i] = bytes1(charCode + 32);
            }
        }
        return string(buffer);
    }

    function _timestamp() internal view returns (uint64) {
        return uint64(block.timestamp);
    }
}
