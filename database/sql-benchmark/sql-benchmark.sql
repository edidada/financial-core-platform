-- 阶段三：SQL 基准测试（记录 CPU/内存/磁盘/DB 版本/数据量/并发数/执行计划）
-- 每次测试必须记录 P50/P95/P99/P99.9、GC、数据库等待事件（README 第六节）。

-- B1 热点账户流水查询（1 亿条中取最近 50 条）
SELECT entry_id, direction, amount_minor, balance_after, created_at
FROM ledger_entry WHERE account_id = ? ORDER BY created_at DESC LIMIT 50;

-- B2 全量借贷平衡校验（批处理）
SELECT COUNT(*) FROM (
    SELECT journal_id FROM ledger_entry GROUP BY journal_id
    HAVING SUM(CASE WHEN direction='DEBIT' THEN amount_minor ELSE 0 END)
        <> SUM(CASE WHEN direction='CREDIT' THEN amount_minor ELSE 0 END)
) t;

-- B3 余额重算（离线一致性）
SELECT b.account_id, b.available_balance - b.opening_balance AS actual,
       COALESCE(SUM(CASE WHEN e.direction='CREDIT' THEN e.amount_minor ELSE -e.amount_minor END), 0) AS recomputed
FROM account_balance b LEFT JOIN ledger_entry e ON e.account_id = b.account_id
GROUP BY b.account_id, b.available_balance, b.opening_balance;