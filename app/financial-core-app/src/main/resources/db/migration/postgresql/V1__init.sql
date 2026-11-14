-- PostgreSQL 迁移。金额单位：分 (BIGINT)；主键与时间戳由应用层写入。

CREATE TABLE account (
    account_id      BIGINT       NOT NULL,
    tenant_id       BIGINT       NOT NULL,
    account_no      VARCHAR(64)  NOT NULL,
    currency        CHAR(3)      NOT NULL,
    ledger_set      VARCHAR(32)  NOT NULL,
    status          VARCHAR(16)  NOT NULL,
    created_at      TIMESTAMPTZ  NOT NULL,
    PRIMARY KEY (account_id),
    UNIQUE (tenant_id, account_no)
);

CREATE TABLE account_balance (
    account_id         BIGINT      NOT NULL,
    opening_balance    BIGINT      NOT NULL,
    available_balance  BIGINT      NOT NULL,
    frozen_balance     BIGINT      NOT NULL,
    version            BIGINT      NOT NULL,
    updated_at         TIMESTAMPTZ NOT NULL,
    PRIMARY KEY (account_id),
    CONSTRAINT chk_balance_nonneg CHECK (available_balance >= 0 AND frozen_balance >= 0)
);

CREATE TABLE transaction (
    transaction_id     BIGINT       NOT NULL,
    tenant_id          BIGINT       NOT NULL,
    idempotency_key    VARCHAR(128) NOT NULL,
    request_hash       CHAR(64)     NOT NULL,
    type               VARCHAR(24)  NOT NULL,
    status             VARCHAR(24)  NOT NULL,
    payer_account_id   BIGINT,
    payee_account_id   BIGINT,
    amount_minor       BIGINT       NOT NULL,
    currency           CHAR(3)      NOT NULL,
    journal_id         BIGINT,
    version            BIGINT       NOT NULL,
    next_retry_at      TIMESTAMPTZ,
    created_at         TIMESTAMPTZ  NOT NULL,
    updated_at         TIMESTAMPTZ  NOT NULL,
    PRIMARY KEY (transaction_id),
    UNIQUE (tenant_id, idempotency_key),
    CONSTRAINT chk_txn_amount_positive CHECK (amount_minor > 0)
);

CREATE TABLE journal (
    journal_id     BIGINT       NOT NULL,
    biz_id         VARCHAR(128) NOT NULL,
    type           VARCHAR(24)  NOT NULL,
    currency       CHAR(3)      NOT NULL,
    ledger_set     VARCHAR(32)  NOT NULL,
    reversal_of    BIGINT,
    reversed_by    BIGINT,
    posted_at      TIMESTAMPTZ  NOT NULL,
    PRIMARY KEY (journal_id),
    UNIQUE (biz_id)
);

CREATE TABLE ledger_entry (
    entry_id        BIGINT      NOT NULL,
    journal_id      BIGINT      NOT NULL,
    account_id      BIGINT      NOT NULL,
    direction       VARCHAR(6)  NOT NULL,
    amount_minor    BIGINT      NOT NULL,
    balance_before  BIGINT      NOT NULL,
    balance_after   BIGINT      NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL,
    PRIMARY KEY (entry_id),
    CONSTRAINT chk_entry_direction CHECK (direction IN ('DEBIT', 'CREDIT')),
    CONSTRAINT chk_entry_amount_positive CHECK (amount_minor > 0)
);

CREATE TABLE idempotency_record (
    idempotency_key  VARCHAR(128) NOT NULL,
    tenant_id        BIGINT       NOT NULL,
    transaction_id   BIGINT       NOT NULL,
    request_hash     CHAR(64)     NOT NULL,
    response_status  INT,
    response_body    TEXT,
    created_at       TIMESTAMPTZ  NOT NULL,
    PRIMARY KEY (tenant_id, idempotency_key)
);

CREATE TABLE outbox_event (
    event_id      BIGINT      NOT NULL,
    aggregate_id  BIGINT      NOT NULL,
    event_type    VARCHAR(64) NOT NULL,
    payload       TEXT        NOT NULL,
    published     BOOLEAN     NOT NULL,
    created_at    TIMESTAMPTZ NOT NULL,
    PRIMARY KEY (event_id)
);

CREATE INDEX idx_txn_status_retry ON transaction (status, next_retry_at);
CREATE INDEX idx_journal_reversal ON journal (reversal_of);
CREATE INDEX idx_entry_journal ON ledger_entry (journal_id);
CREATE INDEX idx_entry_account_created ON ledger_entry (account_id, created_at);
CREATE INDEX idx_outbox_published ON outbox_event (published, created_at);