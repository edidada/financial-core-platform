-- 阶段二~五 新增表：Outbox 投递、消费去重、分布式锁、审计、支付/清算/结算/对账/退款/限额/流动性
-- 金额一律 BIGINT 最小货币单位；时间戳由应用层写入。

CREATE TABLE distributed_lock (
    lock_key     VARCHAR(128) NOT NULL,
    owner        VARCHAR(128) NOT NULL,
    expires_at   DATETIME(6)  NOT NULL,
    acquired_at  DATETIME(6)  NOT NULL,
    PRIMARY KEY (lock_key)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE consumed_message (
    message_key  VARCHAR(128) NOT NULL,
    consumed_at  DATETIME(6)  NOT NULL,
    PRIMARY KEY (message_key)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE audit_log (
    audit_id       BIGINT       NOT NULL,
    actor          VARCHAR(128) NOT NULL,
    action         VARCHAR(64)  NOT NULL,
    resource_type  VARCHAR(64),
    resource_id    VARCHAR(128),
    trace_id       VARCHAR(64),
    detail         TEXT,
    happened_at    DATETIME(6)  NOT NULL,
    PRIMARY KEY (audit_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;
CREATE INDEX idx_audit_actor_time ON audit_log (actor, happened_at);

CREATE TABLE payment_instruction (
    payment_id        BIGINT       NOT NULL,
    transaction_id    BIGINT       NOT NULL,
    payer_account_id  BIGINT       NOT NULL,
    payee_account_id  BIGINT       NOT NULL,
    amount_minor      BIGINT       NOT NULL,
    currency          CHAR(3)      NOT NULL,
    channel           VARCHAR(32)  NOT NULL,
    external_ref      VARCHAR(128),
    status            VARCHAR(24)  NOT NULL,
    version           BIGINT       NOT NULL,
    created_at        DATETIME(6)  NOT NULL,
    updated_at        DATETIME(6)  NOT NULL,
    PRIMARY KEY (payment_id),
    UNIQUE KEY uk_payment_external (channel, external_ref)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE settlement_instruction (
    settlement_id     BIGINT      NOT NULL,
    clearing_batch_id BIGINT      NOT NULL,
    account_id        BIGINT      NOT NULL,
    amount_minor      BIGINT      NOT NULL,
    currency          CHAR(3)     NOT NULL,
    channel           VARCHAR(32) NOT NULL,
    status            VARCHAR(24) NOT NULL,
    created_at        DATETIME(6) NOT NULL,
    PRIMARY KEY (settlement_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;
CREATE INDEX idx_settlement_status ON settlement_instruction (status, created_at);

CREATE TABLE reconciliation_task (
    task_id        BIGINT      NOT NULL,
    window_label   VARCHAR(64) NOT NULL,
    left_count     BIGINT      NOT NULL,
    right_count    BIGINT      NOT NULL,
    matched_count  BIGINT      NOT NULL,
    diff_count     BIGINT      NOT NULL,
    created_at     DATETIME(6) NOT NULL,
    PRIMARY KEY (task_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE reconciliation_diff (
    diff_id        BIGINT      NOT NULL,
    task_id        BIGINT      NOT NULL,
    biz_key        VARCHAR(128) NOT NULL,
    diff_type      VARCHAR(32) NOT NULL,
    left_amount    BIGINT,
    right_amount   BIGINT,
    created_at     DATETIME(6) NOT NULL,
    PRIMARY KEY (diff_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;
CREATE INDEX idx_recon_diff_task ON reconciliation_diff (task_id);

CREATE TABLE refund_order (
    refund_id              BIGINT       NOT NULL,
    original_transaction_id BIGINT      NOT NULL,
    amount_minor           BIGINT       NOT NULL,
    currency               CHAR(3)      NOT NULL,
    status                 VARCHAR(24)  NOT NULL,
    idempotency_key        VARCHAR(128) NOT NULL,
    created_at             DATETIME(6)  NOT NULL,
    PRIMARY KEY (refund_id),
    UNIQUE KEY uk_refund_idem (idempotency_key)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE account_limit (
    limit_id     BIGINT      NOT NULL,
    account_id   BIGINT      NOT NULL,
    per_tx_max   BIGINT      NOT NULL,
    daily_max    BIGINT      NOT NULL,
    used_today   BIGINT      NOT NULL,
    version      BIGINT      NOT NULL,
    updated_at   DATETIME(6) NOT NULL,
    PRIMARY KEY (limit_id),
    UNIQUE KEY uk_limit_account (account_id),
    CONSTRAINT chk_limit_nonneg CHECK (used_today >= 0 AND daily_max >= 0)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE liquidity_snapshot (
    snapshot_id       BIGINT      NOT NULL,
    account_id        BIGINT      NOT NULL,
    available_minor   BIGINT      NOT NULL,
    pending_out_minor BIGINT      NOT NULL,
    currency          CHAR(3)     NOT NULL,
    as_of             DATETIME(6) NOT NULL,
    PRIMARY KEY (snapshot_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;