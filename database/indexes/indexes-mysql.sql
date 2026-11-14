-- 阶段三：索引与执行计划分析用的补充索引（MySQL 8.4）
-- 主索引已在 Flyway V1/V2 中建立；此处为亿级数据场景的覆盖索引实验。

-- 账户流水按时间范围查询：覆盖 balance_before/after 避免回表
CREATE INDEX idx_entry_account_time_cover
    ON ledger_entry (account_id, created_at, amount_minor, balance_after);

-- 对账按凭证批量拉取
CREATE INDEX idx_entry_journal_dir ON ledger_entry (journal_id, direction);

-- 交易恢复扫描：状态 + 下次重试时间（V1 已建，此处为评审记录）
-- CREATE INDEX idx_txn_status_retry ON transaction (status, next_retry_at);

-- 审计按 actor + 时间
-- CREATE INDEX idx_audit_actor_time ON audit_log (actor, happened_at);