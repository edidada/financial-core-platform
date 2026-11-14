-- 阶段三：PostgreSQL 声明式分区（评审/实验用）

CREATE TABLE ledger_entry_archive (
    entry_id        BIGINT      NOT NULL,
    journal_id      BIGINT      NOT NULL,
    account_id      BIGINT      NOT NULL,
    direction       VARCHAR(6)  NOT NULL,
    amount_minor    BIGINT      NOT NULL,
    balance_before  BIGINT      NOT NULL,
    balance_after   BIGINT      NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL,
    PRIMARY KEY (entry_id, created_at)
) PARTITION BY RANGE (created_at);

CREATE TABLE ledger_entry_archive_2026q1 PARTITION OF ledger_entry_archive
    FOR VALUES FROM ('2026-01-01') TO ('2026-04-01');
CREATE TABLE ledger_entry_archive_2026q2 PARTITION OF ledger_entry_archive
    FOR VALUES FROM ('2026-04-01') TO ('2026-07-01');
CREATE TABLE ledger_entry_archive_2026q3 PARTITION OF ledger_entry_archive
    FOR VALUES FROM ('2026-07-01') TO ('2026-10-01');
CREATE TABLE ledger_entry_archive_2026q4 PARTITION OF ledger_entry_archive
    FOR VALUES FROM ('2026-10-01') TO ('2027-01-01');

CREATE INDEX idx_archive_account ON ledger_entry_archive (account_id, created_at);