-- =====================================================================
-- V1__create_schema.sql
-- Fund Transfer Service — reference schema (illustrative)
-- ---------------------------------------------------------------------
-- The running application uses an in-memory H2 database with Hibernate
-- auto-DDL (spring.jpa.hibernate.ddl-auto=create-drop), so it does NOT
-- execute this file. It is included to demonstrate how the same schema
-- would be created and version-controlled with a migration tool such as
-- Flyway (V1__... naming) against a real RDBMS (PostgreSQL dialect shown).
--
-- It mirrors the JPA entities one-to-one:
--   Customer (1)---(*) Account
--   Customer (1)---(*) Beneficiary   [logical link to an account number]
--   Transaction        [records a transfer between two account numbers]
-- =====================================================================

-- ---------------------------------------------------------------------
-- customers
-- ---------------------------------------------------------------------
CREATE TABLE customers (
    id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    username   VARCHAR(50)  NOT NULL UNIQUE,
    password   VARCHAR(255) NOT NULL,          -- BCrypt hash
    full_name  VARCHAR(100) NOT NULL,
    email      VARCHAR(150) NOT NULL UNIQUE
);

-- ---------------------------------------------------------------------
-- accounts  (a customer may own several)
--   version  -> JPA @Version, optimistic locking
--   balance  -> guarded so it can never go negative
-- ---------------------------------------------------------------------
CREATE TABLE accounts (
    id             BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    account_number VARCHAR(34)   NOT NULL UNIQUE,
    type           VARCHAR(20)   NOT NULL CHECK (type IN ('SAVINGS','CURRENT')),
    balance        NUMERIC(19,2) NOT NULL CHECK (balance >= 0),
    customer_id    BIGINT        NOT NULL REFERENCES customers(id),
    version        BIGINT        NOT NULL DEFAULT 0
);
CREATE INDEX idx_accounts_customer ON accounts(customer_id);

-- ---------------------------------------------------------------------
-- beneficiaries  (a saved payee for a customer)
--   The "logical link": account_number must match an existing
--   accounts.account_number. A customer cannot save the same payee twice.
-- ---------------------------------------------------------------------
CREATE TABLE beneficiaries (
    id             BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    nickname       VARCHAR(50) NOT NULL,
    account_number VARCHAR(34) NOT NULL,
    customer_id    BIGINT      NOT NULL REFERENCES customers(id),
    CONSTRAINT uk_beneficiary_customer_account UNIQUE (customer_id, account_number)
);
CREATE INDEX idx_beneficiaries_customer ON beneficiaries(customer_id);

-- ---------------------------------------------------------------------
-- transactions  (immutable audit record of each transfer attempt)
--   Endpoints are stored as account NUMBERS (as in the entity), so a
--   successful and a failed attempt are both preserved for the trail.
-- ---------------------------------------------------------------------
CREATE TABLE transactions (
    id                         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    source_account_number      VARCHAR(34)   NOT NULL,
    destination_account_number VARCHAR(34)   NOT NULL,
    amount                     NUMERIC(19,2) NOT NULL CHECK (amount > 0),
    timestamp                  TIMESTAMP     NOT NULL,
    status                     VARCHAR(20)   NOT NULL CHECK (status IN ('SUCCESS','FAILED')),
    description                VARCHAR(255),
    CONSTRAINT chk_different_accounts CHECK (source_account_number <> destination_account_number)
);
CREATE INDEX idx_txn_source     ON transactions(source_account_number);
CREATE INDEX idx_txn_timestamp  ON transactions(timestamp);

-- =====================================================================
-- Notes
--   * On PostgreSQL, add FOREIGN KEY (source_account_number)
--     REFERENCES accounts(account_number) if you want the DB to enforce
--     that transaction endpoints reference real accounts. The JPA model
--     keeps them as plain strings for a lightweight, append-only ledger.
--   * To actually run migrations instead of Hibernate auto-DDL:
--       - add the Flyway dependency,
--       - set spring.jpa.hibernate.ddl-auto=validate,
--       - place this file at src/main/resources/db/migration/.
-- =====================================================================
