package com.fcp.service.ledger;

import com.fcp.common.api.InvariantReport;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

/** 资金不变量 I-1~I-4 的离线校验（README 第十节「成果 A」）。 */
@Service
public class LedgerInvariantChecker {
    private final JdbcTemplate jdbc;

    public LedgerInvariantChecker(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public InvariantReport check() {
        Map<String, Long> v = new LinkedHashMap<>();
        v.put("I-1 unbalanced journals", count(
                "SELECT COUNT(*) FROM (SELECT journal_id FROM ledger_entry GROUP BY journal_id "
                        + "HAVING SUM(CASE WHEN direction='DEBIT' THEN amount_minor ELSE 0 END) "
                        + "<> SUM(CASE WHEN direction='CREDIT' THEN amount_minor ELSE 0 END)) t"));
        v.put("I-2 balance mismatch", count(
                "SELECT COUNT(*) FROM (SELECT b.account_id FROM account_balance b "
                        + "LEFT JOIN ledger_entry e ON e.account_id = b.account_id "
                        + "GROUP BY b.account_id, b.available_balance, b.opening_balance "
                        + "HAVING b.available_balance - b.opening_balance <> "
                        + "COALESCE(SUM(CASE WHEN e.direction='CREDIT' THEN e.amount_minor "
                        + "ELSE -e.amount_minor END), 0)) t"));
        v.put("I-3 duplicate biz_id", count(
                "SELECT COUNT(*) FROM (SELECT biz_id FROM journal GROUP BY biz_id HAVING COUNT(*) > 1) t"));
        v.put("I-4 negative balance", count(
                "SELECT COUNT(*) FROM account_balance WHERE available_balance < 0 OR frozen_balance < 0"));
        return InvariantReport.of(v);
    }

    private long count(String sql) {
        Long n = jdbc.queryForObject(sql, Long.class);
        return n == null ? 0L : n;
    }
}