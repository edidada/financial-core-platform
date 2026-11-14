-- MySQL 8.4 版数据集生成（递归 CTE，需 cte_max_recursion_depth 足够大）
SET SESSION cte_max_recursion_depth = 20000;

WITH RECURSIVE seq (n) AS (
  SELECT 1 UNION ALL SELECT n + 1 FROM seq WHERE n < 10000
)
INSERT INTO account (account_id, tenant_id, account_no, currency, ledger_set, status, created_at)
SELECT 10000 + n, 1, CONCAT('BENCH-', n), 'CNY', 'DEFAULT', 'ACTIVE', CURRENT_TIMESTAMP(6)
FROM seq;

WITH RECURSIVE seq (n) AS (
  SELECT 1 UNION ALL SELECT n + 1 FROM seq WHERE n < 10000
)
INSERT INTO account_balance (account_id, opening_balance, available_balance, frozen_balance, version, updated_at)
SELECT 10000 + n, 1000000, 1000000, 0, 0, CURRENT_TIMESTAMP(6)
FROM seq;