package com.fcp.common.utils;

import java.util.concurrent.atomic.AtomicLong;

/** 进程内雪花 ID：41 位毫秒时间戳 + 10 位 worker + 12 位序列。 */
public final class SnowflakeIdGenerator implements IdGenerator {
    private static final long EPOCH = 1700000000000L;
    private static final long WORKER_BITS = 10L;
    private static final long SEQUENCE_BITS = 12L;
    private static final long MAX_WORKER = (1L << WORKER_BITS) - 1;
    private static final long MAX_SEQUENCE = (1L << SEQUENCE_BITS) - 1;

    private final long workerId;
    private final AtomicLong lastTimestamp = new AtomicLong(-1L);
    private final AtomicLong sequence = new AtomicLong(0L);

    public SnowflakeIdGenerator(long workerId) {
        if (workerId < 0 || workerId > MAX_WORKER) {
            throw new IllegalArgumentException("workerId must be in [0, " + MAX_WORKER + "]");
        }
        this.workerId = workerId;
    }

    @Override
    public long nextId() {
        synchronized (this) {
            long ts = System.currentTimeMillis();
            long last = lastTimestamp.get();
            if (ts < last) {
                throw new IllegalStateException("Clock moved backwards, refuse to generate id");
            }
            long seq;
            if (ts == last) {
                seq = sequence.incrementAndGet() & MAX_SEQUENCE;
                if (seq == 0) {
                    do {
                        ts = System.currentTimeMillis();
                        Thread.onSpinWait();
                    } while (ts <= last);
                    sequence.set(0);
                }
            } else {
                sequence.set(0);
                seq = 0;
            }
            lastTimestamp.set(ts);
            return ((ts - EPOCH) << (WORKER_BITS + SEQUENCE_BITS))
                    | (workerId << SEQUENCE_BITS)
                    | seq;
        }
    }
}