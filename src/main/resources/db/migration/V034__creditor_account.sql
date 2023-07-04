CREATE TABLE IF NOT EXISTS creditor_account(
    id                  UUID NOT NULL,
    client_id           UUID NOT NULL,
    account_holder_name VARCHAR(256) NOT NULL,
    account_number      VARCHAR(256) NOT NULL,
    PRIMARY KEY(id),
    UNIQUE (client_id, account_number),
    FOREIGN KEY (client_id) REFERENCES client (client_id)
);
