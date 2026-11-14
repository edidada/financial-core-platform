package com.fcp.service.liquidity;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/** 流动性管理：汇总各账户可用资金与在途出款，给出预警。 */
@Service
public class LiquidityService {
    private final JdbcTemplate jdbc;
    private final com.fcp.common.utils.IdGenerator ids;

    public LiquidityService(JdbcTemplate jdbc, com.fcp.common.utils.IdGenerator ids) {
        this.jdbc = jdbc; this.ids = ids;
    }

    public List<LiquiditySnapshot> snapshot(String currency) {
        List<LiquiditySnapshot> out = new ArrayList<>();
        var rows = jdbc.query("SELECT b.account_id, b.available_balance FROM account_balance b "
                        + "JOIN account a ON a.account_id = b.account_id WHERE a.currency = ?",
                (rs, i) -> new long[]{rs.getLong(1), rs.getLong(2)}, currency);
        for (long[] r : rows) {
            long id = ids.nextId();
            long pending = 0L;  // 阶段二：在途结算由 settlement_instruction 汇总
            jdbc.update("INSERT INTO liquidity_snapshot (snapshot_id, account_id, available_minor, "
                            + "pending_out_minor, currency, as_of) VALUES (?,?,?,?,?,?)",
                    id, r[0], r[1], pending, currency, Timestamp.from(Instant.now()));
            out.add(new LiquiditySnapshot(id, r[0], r[1], pending, currency, System.currentTimeMillis()));
        }
        return List.copyOf(out);
    }
}