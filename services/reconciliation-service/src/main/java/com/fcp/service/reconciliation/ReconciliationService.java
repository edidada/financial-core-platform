package com.fcp.service.reconciliation;

import com.fcp.engine.recon.ReconciliationMatcher;
import com.fcp.engine.recon.ReconciliationRecord;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;

/** 多方对账：内部流水 vs 渠道流水，差异可定位、结果可复现（README 第四节）。 */
@Service
public class ReconciliationService {
    private final JdbcTemplate jdbc;
    private final ReconciliationMatcher matcher = new ReconciliationMatcher();
    private final com.fcp.common.utils.IdGenerator ids;

    public ReconciliationService(JdbcTemplate jdbc, com.fcp.common.utils.IdGenerator ids) {
        this.jdbc = jdbc; this.ids = ids;
    }

    public ReconciliationReport reconcile(String window, List<ReconciliationRecord> internal,
                                          List<ReconciliationRecord> external) {
        long start = System.currentTimeMillis();
        var result = matcher.match(internal, external);
        long taskId = ids.nextId();
        jdbc.update("INSERT INTO reconciliation_task (task_id, window_label, left_count, right_count, "
                        + "matched_count, diff_count, created_at) VALUES (?,?,?,?,?,?,?)",
                taskId, window, (int) result.leftCount(), (int) result.rightCount(), (int) result.matched(),
                result.diffs().size(), Timestamp.from(Instant.now()));
        return new ReconciliationReport(taskId, window, result, System.currentTimeMillis() - start);
    }
}