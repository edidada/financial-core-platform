package com.fcp.tests;

import com.fcp.service.fee.FeeService;
import com.fcp.service.risk.RiskService;
import com.fcp.service.clearing.ClearingService;
import com.fcp.service.reconciliation.ReconciliationService;
import com.fcp.infra.id.EnvironmentWorkerIdProvider;
import com.fcp.infra.id.ShardedIdGeneratorFactory;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class BusinessServicesTest {
    private final com.fcp.common.utils.IdGenerator ids =
            ShardedIdGeneratorFactory.create(new EnvironmentWorkerIdProvider(9L));

    @Test void feeIsIntegerAndCapped() {
        var q = new FeeService().quote(100_000L, "CNY");
        assertEquals(100L, q.feeMinor());
        assertEquals(99_900L, q.netMinor());
        assertEquals(5000L, new FeeService().quote(100_000_000L, "CNY").feeMinor());
    }

    @Test void riskBlocksLargeAndDegrades() {
        var d = new RiskService().evaluate(2_000_000L, "CNY", 10, 30);
        assertEquals("REJECT", d.outcome());
        assertNotNull(d.route());
    }

    @Test void clearingNetIsAlwaysZero() {
        var svc = new ClearingService(ids);
        var batch = svc.net("2026-11-14", "CNY", List.of(
                new long[]{1L, 100L, 0L}, new long[]{2L, 0L, 60L}, new long[]{3L, 0L, 40L}));
        assertEquals(0L, batch.netSum());
        assertEquals(3, batch.items().size());
    }
}