package com.fcp.app;

import com.fcp.common.api.ApiResult;
import com.fcp.common.domain.Currency;
import com.fcp.engine.hotspot.HotspotProof;
import com.fcp.engine.recon.ReconciliationRecord;
import com.fcp.gateway.api.GatewayFilterChain;
import com.fcp.gateway.api.GatewayRequest;
import com.fcp.infra.audit.AuditEntry;
import com.fcp.infra.audit.AuditLogger;
import com.fcp.infra.consumer.MessageDeduplicator;
import com.fcp.infra.lock.DistributedLock;
import com.fcp.infra.observability.BusinessMetrics;
import com.fcp.infra.outbox.InMemoryEventPublisher;
import com.fcp.infra.outbox.OutboxRelay;
import com.fcp.infra.sharding.ShardRouter;
import com.fcp.service.clearing.ClearingBatch;
import com.fcp.service.clearing.ClearingService;
import com.fcp.service.fee.FeeService;
import com.fcp.service.limit.LimitService;
import com.fcp.service.liquidity.LiquidityService;
import com.fcp.service.payment.PaymentService;
import com.fcp.service.reconciliation.ReconciliationService;
import com.fcp.service.risk.RiskService;
import com.fcp.service.settlement.SettlementService;
import com.fcp.service.reporting.ReportingService;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** 阶段二~五能力的 REST 入口，供集成测试与架构评审验证。 */
@RestController
@RequestMapping("/v1/platform")
public class PlatformController {
    private final PaymentService payments;
    private final ClearingService clearing;
    private final SettlementService settlement;
    private final ReconciliationService reconciliation;
    private final FeeService fees;
    private final LimitService limits;
    private final RiskService risk;
    private final LiquidityService liquidity;
    private final ReportingService reporting;
    private final OutboxRelay outboxRelay;
    private final InMemoryEventPublisher publisher;
    private final MessageDeduplicator dedup;
    private final DistributedLock lock;
    private final ShardRouter shards;
    private final BusinessMetrics metrics;
    private final AuditLogger audit;
    private final GatewayFilterChain gateway;

    public PlatformController(PaymentService payments, ClearingService clearing,
                              SettlementService settlement, ReconciliationService reconciliation,
                              FeeService fees, LimitService limits, RiskService risk,
                              LiquidityService liquidity, ReportingService reporting,
                              OutboxRelay outboxRelay, InMemoryEventPublisher publisher,
                              MessageDeduplicator dedup, DistributedLock lock, ShardRouter shards,
                              BusinessMetrics metrics, AuditLogger audit, GatewayFilterChain gateway) {
        this.payments = payments; this.clearing = clearing; this.settlement = settlement;
        this.reconciliation = reconciliation; this.fees = fees; this.limits = limits;
        this.risk = risk; this.liquidity = liquidity; this.reporting = reporting;
        this.outboxRelay = outboxRelay; this.publisher = publisher; this.dedup = dedup;
        this.lock = lock; this.shards = shards; this.metrics = metrics;
        this.audit = audit; this.gateway = gateway;
    }

    @PostMapping("/payments")
    public ApiResult<?> payment(@RequestBody Map<String, Object> b) {
        long id = payments.submit(num(b, "transactionId"), num(b, "payer"), num(b, "payee"),
                num(b, "amountMinor"), Currency.of(str(b, "currency", "CNY")),
                str(b, "channel", "INTERNAL"), str(b, "externalRef", "ref-" + System.nanoTime()));
        return ApiResult.ok(Map.of("paymentId", id, "status", payments.findById(id).orElseThrow().status().name()));
    }

    @PostMapping("/clearing")
    public ApiResult<?> clearing(@RequestBody Map<String, Object> b) {
        @SuppressWarnings("unchecked")
        List<List<Number>> raw = (List<List<Number>>) b.get("rows");
        List<long[]> rows = new ArrayList<>();
        for (List<Number> r : raw) rows.add(new long[]{r.get(0).longValue(), r.get(1).longValue(), r.get(2).longValue()});
        ClearingBatch batch = clearing.net(str(b, "window", "manual"), str(b, "currency", "CNY"), rows);
        return ApiResult.ok(Map.of("batchId", batch.batchId(), "itemCount", batch.items().size(),
                "netSum", batch.netSum(), "balanced", batch.netSum() == 0));
    }

    @GetMapping("/fees/quote")
    public ApiResult<?> fee(@RequestParam long amountMinor,
                            @RequestParam(defaultValue = "CNY") String currency) {
        return ApiResult.ok(fees.quote(amountMinor, currency));
    }

    @PostMapping("/limits/configure")
    public ApiResult<?> configureLimit(@RequestParam long accountId,
                                       @RequestParam long perTxMax,
                                       @RequestParam long dailyMax) {
        limits.configure(accountId, perTxMax, dailyMax);
        return ApiResult.ok(Map.of("configured", true));
    }

    @GetMapping("/limits/check")
    public ApiResult<?> checkLimit(@RequestParam long accountId, @RequestParam long amountMinor) {
        return ApiResult.ok(limits.check(accountId, amountMinor));
    }

    @GetMapping("/risk/evaluate")
    public ApiResult<?> evaluateRisk(@RequestParam long amountMinor,
                                     @RequestParam(defaultValue = "CNY") String currency,
                                     @RequestParam(defaultValue = "0") int riskScore,
                                     @RequestParam(defaultValue = "30") int deviceAgeDays) {
        return ApiResult.ok(risk.evaluate(amountMinor, currency, riskScore, deviceAgeDays));
    }

    @PostMapping("/reconciliation")
    public ApiResult<?> reconcile(@RequestBody Map<String, Object> b) {
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> left = (List<Map<String, Object>>) b.getOrDefault("left", List.of());
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> right = (List<Map<String, Object>>) b.getOrDefault("right", List.of());
        var report = reconciliation.reconcile(str(b, "window", "manual"), toRecords(left), toRecords(right));
        return ApiResult.ok(Map.of("taskId", report.taskId(), "clean", report.clean(),
                "diffCount", report.result().diffs().size(), "matched", report.result().matched(),
                "elapsedMillis", report.elapsedMillis()));
    }

    @GetMapping("/reports/daily")
    public ApiResult<?> dailyReport(@RequestParam(required = false) String date) {
        return ApiResult.ok(reporting.daily(date == null ? LocalDate.now() : LocalDate.parse(date)));
    }

    @GetMapping("/liquidity")
    public ApiResult<?> liquidity(@RequestParam(defaultValue = "CNY") String currency) {
        return ApiResult.ok(liquidity.snapshot(currency));
    }

    @PostMapping("/settlement/confirm")
    public ApiResult<?> confirmSettlement(@RequestParam long settlementId) {
        settlement.confirm(settlementId);
        return ApiResult.ok(Map.of("confirmed", settlementId));
    }

    @PostMapping("/outbox/relay")
    public ApiResult<?> relay(@RequestParam(defaultValue = "100") int batchSize) {
        int n = outboxRelay.relayOnce(batchSize);
        metrics.increment("outboxRelay");
        return ApiResult.ok(Map.of("relayed", n, "totalPublished", publisher.events().size()));
    }

    @GetMapping("/outbox/events")
    public ApiResult<?> outboxEvents() {
        return ApiResult.ok(publisher.events());
    }

    @PostMapping("/consumer/dedup")
    public ApiResult<?> dedup(@RequestParam String messageKey) {
        return ApiResult.ok(Map.of("firstTime", dedup.tryClaim(messageKey),
                "consumedTotal", dedup.consumedCount()));
    }

    @PostMapping("/locks/acquire")
    public ApiResult<?> acquireLock(@RequestParam String key, @RequestParam(defaultValue = "30") long seconds) {
        return ApiResult.ok(Map.of("acquired", lock.tryLock(key, "rest", Duration.ofSeconds(seconds))));
    }

    @DeleteMapping("/locks/release")
    public ApiResult<?> releaseLock(@RequestParam String key) {
        lock.unlock(key, "rest");
        return ApiResult.ok(Map.of("released", true));
    }

    @GetMapping("/shards/{accountId}")
    public ApiResult<?> shard(@PathVariable long accountId) {
        return ApiResult.ok(Map.of("accountId", accountId, "shard", shards.shardOf(accountId),
                "shardCount", shards.shardCount()));
    }

    @PostMapping("/gateway/execute")
    public ApiResult<?> gateway(@RequestBody Map<String, Object> b) {
        long now = System.currentTimeMillis();
        String ts = String.valueOf(now);
        String nonce = str(b, "nonce", "n-" + System.nanoTime());
        String body = str(b, "body", "");
        String path = str(b, "path", "/v1/transactions");
        String sig = com.fcp.gateway.auth.RequestSigner.sign(
                System.getenv().getOrDefault("FCP_GATEWAY_SECRET", "dev-secret"),
                com.fcp.gateway.auth.RequestSigner.canonical("POST", path, ts, nonce, body));
        var req = new GatewayRequest("POST", path, str(b, "apiKey", "dev-key"), ts, nonce, sig, body, Map.of());
        var resp = gateway.execute(req, (r, trace) -> new com.fcp.gateway.api.GatewayResponse(200, "OK", trace), () -> now);
        audit.record(new AuditEntry("gateway", "GATEWAY_SMOKE", "HTTP", path, resp.traceId(), "OK", now));
        return ApiResult.ok(Map.of("status", resp.status(), "traceId", resp.traceId()));
    }

    @GetMapping("/hotspot/proof")
    public ApiResult<?> hotspotProof(@RequestParam(defaultValue = "5000") int n) {
        List<long[]> seq = new ArrayList<>();
        for (int i = 0; i < n; i++) seq.add(new long[]{1L, 2L, 1L + (i % 20)});
        return ApiResult.ok(Map.of("verified", HotspotProof.verify(1_000_000L, seq, 42L), "transactions", n));
    }

    @GetMapping("/ops/metrics")
    public ApiResult<?> metrics() {
        return ApiResult.ok(Map.of("outboxRelay", metrics.counter("outboxRelay"),
                "deadLetter", metrics.counter("deadLetter")));
    }

    private static List<ReconciliationRecord> toRecords(List<Map<String, Object>> rows) {
        List<ReconciliationRecord> out = new ArrayList<>();
        for (Map<String, Object> r : rows) {
            out.add(new ReconciliationRecord(String.valueOf(r.get("bizKey")),
                    ((Number) r.get("amountMinor")).longValue(),
                    String.valueOf(r.getOrDefault("currency", "CNY")),
                    String.valueOf(r.getOrDefault("source", "unknown"))));
        }
        return out;
    }

    private static long num(Map<String, Object> b, String k) { return ((Number) b.get(k)).longValue(); }
    private static String str(Map<String, Object> b, String k, String def) {
        Object v = b.get(k);
        return v == null ? def : String.valueOf(v);
    }
}