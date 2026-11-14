-- 阶段三：MySQL 8.4 RANGE 分区（评审/实验用，不在默认 Flyway 中执行）
-- 注意：MySQL 要求分区键必须包含在所有唯一键中，因此分区表单独设计为历史归档表。

CREATE TABLE ledger_entry_archive (
    entry_id        BIGINT      NOT NULL,
    journal_id      BIGINT      NOT NULL,
    account_id      BIGINT      NOT NULL,
    direction       VARCHAR(6)  NOT NULL,
    amount_minor    BIGINT      NOT NULL,
    balance_before  BIGINT      NOT NULL,
    balance_after   BIGINT      NOT NULL,
    created_at      DATETIME(6) NOT NULL,
    PRIMARY KEY (entry_id, created_at),
    KEY idx_archive_account (account_id, created_at)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4
PARTITION BY RANGE (TO_DAYS(created_at)) (
    PARTITION p2026q1 VALUES LESS THAN (TO_DAYS('2026-04-01')),
    PARTITION p2026q2 VALUES LESS THAN (TO_DAYS('2026-07-01')),
    PARTITION p2026q3 VALUES LESS THAN (TO_DAYS('2026-10-01')),
    PARTITION p2026q4 VALUES LESS THAN (TO_DAYS('2027-01-01')),
    PARTITION pmax   VALUES LESS THAN MAXVALUE
);

-- 归档：从热表搬移已完成期间数据（低峰批处理）
-- INSERT INTO ledger_entry_archive SELECT * FROM ledger_entry WHERE created_at < '2026-04-01';
-- DELETE FROM ledger_entry WHERE created_at < '2026-04-01';