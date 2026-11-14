package com.fcp.benchmark;

import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.options.OptionsBuilder;

public final class BenchmarkRunner {
    private BenchmarkRunner() {}

    public static void main(String[] args) throws Exception {
        new Runner(new OptionsBuilder()
                .include(PostingBenchmark.class.getSimpleName())
                .include(ReconciliationBenchmark.class.getSimpleName())
                .build()).run();
    }
}