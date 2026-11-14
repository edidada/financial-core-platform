package com.fcp.tests;

import com.fcp.infra.circuit.*;
import com.fcp.infra.ratelimit.*;
import com.fcp.infra.sharding.*;
import com.fcp.infra.id.*;
import com.fcp.infra.observability.BusinessMetrics;
import org.junit.jupiter.api.Test;
import java.time.Duration;
import java.util.List;
import java.util.stream.IntStream;
import static org.junit.jupiter.api.Assertions.*;

class InfrastructureTest {
    @Test void tokenBucketEnforcesCapacity() {
        RateLimiter rl = new TokenBucketRateLimiter(2, 1, Duration.ofHours(1));
        assertTrue(rl.tryAcquire("k"));
        assertTrue(rl.tryAcquire("k"));
        assertFalse(rl.tryAcquire("k"));
    }

    @Test void circuitOpensAfterThreshold() {
        CircuitBreaker cb = new CircuitBreaker(2, Duration.ofHours(1));
        // 熔断器透传业务异常；累计失败达到阈值后转为 OPEN
        assertThrows(RuntimeException.class, () -> cb.execute(() -> { throw new RuntimeException("x"); }));
        assertThrows(RuntimeException.class, () -> cb.execute(() -> { throw new RuntimeException("x"); }));
        assertEquals(CircuitState.OPEN, cb.state());
        // OPEN 状态下直接短路，不再执行业务
        assertThrows(IllegalStateException.class, () -> cb.execute(() -> "ok"));
    }

    @Test void shardRouterStableAndSingleShardByDefault() {
        ShardRouter r = new ShardRouter(1);
        assertEquals(0, r.shardOf(123L));
        assertTrue(new ShardAwarePosting(r).sameShard(1L, 2L));
        assertEquals(3, new ShardRouter(4).shardOf(7L));
    }

    @Test void idsAreUniqueUnderConcurrency() {
        var gen = ShardedIdGeneratorFactory.create(new EnvironmentWorkerIdProvider(3L));
        var ids = IntStream.range(0, 10_000).parallel().mapToLong(i -> gen.nextId()).boxed().toList();
        assertEquals(10_000, ids.stream().distinct().count());
    }

    @Test void businessMetricsCount() {
        BusinessMetrics m = new BusinessMetrics();
        m.increment("tx"); m.increment("tx");
        m.observeLatency("lat", 2_000_000L);
        assertEquals(2L, m.counter("tx"));
        assertEquals(2.0, m.totalMillis("lat"), 0.001);
    }
}