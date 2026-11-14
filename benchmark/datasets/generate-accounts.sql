-- 阶段三数据集生成（PostgreSQL 版，generate_series）
INSERT INTO account (account_id, tenant_id, account_no, currency, ledger_set, status, created_at)
SELECT 10000 + g, 1, 'BENCH-' || g, 'CNY', 'DEFAULT', 'ACTIVE', CURRENT_TIMESTAMP
FROM generate_series(1, 10000) AS g;

INSERT INTO account_balance (account_id, opening_balance, available_balance, frozen_balance, version, updated_at)
SELECT 10000 + g, 1000000, 1000000, 0, 0, CURRENT_TIMESTAMP
FROM generate_series(1, 10000) AS g;