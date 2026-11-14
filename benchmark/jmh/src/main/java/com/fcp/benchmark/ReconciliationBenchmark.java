package com.fcp.benchmark;

import com.fcp.engine.recon.ReconciliationMatcher;
import com.fcp.engine.recon.ReconciliationRecord;
import org.openjdk.jmh.annotations.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/** 亿级对账的匹配引擎微基准：哈希连接 O(n+m)。 */
@State(Scope.Thread)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@Warmup(iterations = 2, time = 1)
@Measurement(iterations = 3, time = 1)
@Fork(1)
public class ReconciliationBenchmark {

    @Param({"1000", "10000"})
    private int size;

    private List<ReconciliationRecord> left;
    private List<ReconciliationRecord> right;
    private ReconciliationMatcher matcher;

    @Setup(Level.Trial)
    public void setup() {
        left = new ArrayList<>(size);
        right = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            left.add(new ReconciliationRecord("K" + i, 100L + i, "CNY", "internal"));
            right.add(new ReconciliationRecord("K" + i, 100L + i, "CNY", "channel"));
        }
        matcher = new ReconciliationMatcher();
    }

    @Benchmark
    public Object matchAllEqual() {
        return matcher.match(left, right);
    }
}