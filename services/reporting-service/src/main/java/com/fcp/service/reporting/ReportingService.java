package com.fcp.service.reporting;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.sql.Timestamp;

/** 财务及监管报表：按日按币种汇总，必须借贷平衡。 */
@Service
public class ReportingService {
    private final JdbcTemplate jdbc;

    public ReportingService(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    public DailyReport daily(LocalDate date) {
        var row = jdbc.queryForMap(
                "SELECT COALESCE(SUM(CASE WHEN e.direction='DEBIT' THEN e.amount_minor ELSE 0 END),0) AS d, "
                        + "COALESCE(SUM(CASE WHEN e.direction='CREDIT' THEN e.amount_minor ELSE 0 END),0) AS c, "
                        + "COUNT(DISTINCT j.journal_id) AS n "
                        + "FROM journal j JOIN ledger_entry e ON e.journal_id = j.journal_id "
                        + "WHERE j.posted_at >= ? AND j.posted_at < ? AND j.currency = 'CNY'",
                Timestamp.valueOf(date.atStartOfDay()), Timestamp.valueOf(date.plusDays(1).atStartOfDay()));
        long d = ((Number) row.get("d")).longValue();
        long c = ((Number) row.get("c")).longValue();
        long n = ((Number) row.get("n")).longValue();
        return new DailyReport(date.toString(), "CNY", d, c, n, d == c);
    }
}