package com.fcp.engine.batch;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Consumer;

/**
 * 通用批处理引擎：固定分片 + 虚拟线程执行器（Java 21），
 * 用于清算/对账/报表的大批量计算（阶段三 1 亿条对账）。
 */
public final class BatchEngine implements AutoCloseable {
    private final int parallelism;
    private final ExecutorService executor;

    public BatchEngine(int parallelism) {
        this.parallelism = parallelism;
        this.executor = Executors.newVirtualThreadPerTaskExecutor();
    }

    public <T> BatchResult run(List<T> items, Consumer<T> task) {
        long start = System.currentTimeMillis();
        List<Callable<Boolean>> jobs = new ArrayList<>(items.size());
        for (T item : items) {
            jobs.add(() -> { task.accept(item); return Boolean.TRUE; });
        }
        long ok = 0, fail = 0;
        try {
            List<Future<Boolean>> futures = executor.invokeAll(jobs);
            for (Future<Boolean> f : futures) {
                try { if (Boolean.TRUE.equals(f.get())) ok++; else fail++; }
                catch (Exception e) { fail++; }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            fail = items.size();
        }
        return new BatchResult(items.size(), ok, fail, System.currentTimeMillis() - start);
    }

    public int parallelism() { return parallelism; }

    @Override public void close() { executor.close(); }
}