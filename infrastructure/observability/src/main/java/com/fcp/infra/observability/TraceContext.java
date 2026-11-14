package com.fcp.infra.observability;

/** 全局 TraceId / MDC 传递，网关入口生成、全链路透传。 */
public final class TraceContext {
    private static final ThreadLocal<String> TRACE_ID = new ThreadLocal<>();

    private TraceContext() {}

    public static void set(String traceId) { TRACE_ID.set(traceId); }
    public static String get() { return TRACE_ID.get(); }
    public static void clear() { TRACE_ID.remove(); }

    public static String newTraceId() {
        return Long.toHexString(System.currentTimeMillis()) + "-" +
                Long.toHexString(Thread.currentThread().threadId());
    }
}