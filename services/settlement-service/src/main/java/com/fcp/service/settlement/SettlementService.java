package com.fcp.service.settlement;

import com.fcp.common.exception.ResourceNotFoundException;
import com.fcp.service.clearing.ClearingBatch;
import com.fcp.service.clearing.ClearingItem;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/** 资金结算：把清算净额转成对外的结算指令，与账务入账解耦（README 第三节）。 */
@Service
public class SettlementService {
    private final JdbcTemplate jdbc;
    private final com.fcp.common.utils.IdGenerator ids;

    public SettlementService(JdbcTemplate jdbc, com.fcp.common.utils.IdGenerator ids) {
        this.jdbc = jdbc; this.ids = ids;
    }

    public List<SettlementInstruction> instruct(ClearingBatch batch, String channel) {
        List<SettlementInstruction> out = new ArrayList<>();
        for (ClearingItem item : batch.items()) {
            if (item.netMinor() == 0) continue;
            long id = ids.nextId();
            jdbc.update("INSERT INTO settlement_instruction (settlement_id, clearing_batch_id, account_id, "
                            + "amount_minor, currency, channel, status, created_at) VALUES (?,?,?,?,?,?,?,?)",
                    id, batch.batchId(), item.accountId(), Math.abs(item.netMinor()), item.currency(),
                    channel, SettlementStatus.INSTRUCTED.name(), Timestamp.from(Instant.now()));
            out.add(new SettlementInstruction(id, batch.batchId(), item.accountId(),
                    Math.abs(item.netMinor()), item.currency(), channel, SettlementStatus.INSTRUCTED));
        }
        return List.copyOf(out);
    }

    public long pendingCount() {
        Long n = jdbc.queryForObject("SELECT COUNT(*) FROM settlement_instruction WHERE status = 'PENDING'", Long.class);
        return n == null ? 0L : n;
    }

    public void confirm(long settlementId) {
        int n = jdbc.update("UPDATE settlement_instruction SET status = ? WHERE settlement_id = ? AND status = ?",
                SettlementStatus.CONFIRMED.name(), settlementId, SettlementStatus.INSTRUCTED.name());
        if (n != 1) throw new ResourceNotFoundException("Settlement instruction", settlementId);
    }
}