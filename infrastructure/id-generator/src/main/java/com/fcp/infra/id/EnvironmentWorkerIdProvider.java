package com.fcp.infra.id;

public final class EnvironmentWorkerIdProvider implements WorkerIdProvider {
    private final long workerId;

    public EnvironmentWorkerIdProvider() {
        String raw = System.getenv("FCP_WORKER_ID");
        this.workerId = raw == null || raw.isBlank() ? 1L : Long.parseLong(raw.trim());
    }

    public EnvironmentWorkerIdProvider(long workerId) { this.workerId = workerId; }

    @Override public long workerId() { return workerId; }
}