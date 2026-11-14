package com.fcp.service.limit;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.Instant;

/** 交易限额：单笔 + 日累计，条件 UPDATE 保证并发下不超限（与账户扣款同构）。 */
@Service
public class LimitService {
    private final JdbcTemplate jdbc;
    private final com.fcp.common.utils.IdGenerator ids;

    public LimitService(JdbcTemplate jdbc, com.fcp.common.utils.IdGenerator ids) {
        this.jdbc = jdbc; this.ids = ids;
    }

    public void configure(long accountId, long perTxMax, long dailyMax) {
        long id = ids.nextId();
        jdbc.update("INSERT INTO account_limit (limit_id, account_id, per_tx_max, daily_max, used_today, version, updated_at) "
                        + "VALUES (?,?,?,?,?,?,?)",
                id, accountId, perTxMax, dailyMax, 0L, 0L, Timestamp.from(Instant.now()));
    }

    /** @return true 表示已成功占用额度；false 表示超限 */
    public boolean tryConsume(long accountId, long amountMinor) {
        return jdbc.update("UPDATE account_limit SET used_today = used_today + ?, version = version + 1, "
                        + "updated_at = ? WHERE account_id = ? AND used_today + ? <= daily_max AND ? <= per_tx_max",
                amountMinor, Timestamp.from(Instant.now()), accountId, amountMinor, amountMinor) == 1;
    }

    public LimitCheck check(long accountId, long amountMinor) {
        Long used = jdbc.queryForObject("SELECT used_today FROM account_limit WHERE account_id = ?", Long.class, accountId);
        Long daily = jdbc.queryForObject("SELECT daily_max FROM account_limit WHERE account_id = ?", Long.class, accountId);
        Long perTx = jdbc.queryForObject("SELECT per_tx_max FROM account_limit WHERE account_id = ?", Long.class, accountId);
        if (used == null || daily == null || perTx == null) return new LimitCheck(false, "no limit configured", 0);
        if (amountMinor > perTx) return new LimitCheck(false, "per-tx limit exceeded", daily - used);
        if (used + amountMinor > daily) return new LimitCheck(false, "daily limit exceeded", daily - used);
        return new LimitCheck(true, "OK", daily - used - amountMinor);
    }
}