CREATE TABLE chain_identity (
    identity_key SMALLINT PRIMARY KEY CHECK (identity_key = 1),
    chain_id NUMERIC(78, 0) NOT NULL CHECK (chain_id > 0),
    genesis_hash CHAR(66) NOT NULL
        CHECK (genesis_hash ~ '^0x[0-9a-f]{64}$'),
    bound_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE nonce_cursor (
    signer_address CHAR(42) PRIMARY KEY
        CHECK (signer_address ~ '^0x[0-9a-f]{40}$'),
    next_nonce NUMERIC(78, 0) NOT NULL CHECK (next_nonce >= 0),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
