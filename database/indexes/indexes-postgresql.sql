-- 阶段三：PostgreSQL 补充索引（与 MySQL 版语义一致）

CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_entry_account_time_cover
    ON ledger_entry (account_id, created_at) INCLUDE (amount_minor, balance_after);

CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_entry_journal_dir
    ON ledger_entry (journal_id, direction);

-- 部分索引：仅未投递事件，减小索引体积
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_outbox_unpublished
    ON outbox_event (created_at) WHERE published = FALSE;