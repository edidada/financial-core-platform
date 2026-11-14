package com.fcp.infra.sharding;

/** 阶段三分库分表路由；阶段一/二 shardCount = 1（单库），保证行为可平滑演进。 */
public final class ShardRouter {
    private final int shardCount;

    public ShardRouter(int shardCount) {
        if (shardCount < 1) throw new IllegalArgumentException("shardCount must be >= 1");
        this.shardCount = shardCount;
    }

    public int shardOf(long accountId) {
        // 与雪花 ID 低位无关的稳定取模；负数取绝对值避免负分片
        return (int) Math.floorMod(accountId, (long) shardCount);
    }

    public String tableName(String logicalTable, long accountId) {
        return logicalTable + "_" + shardOf(accountId);
    }

    public int shardCount() { return shardCount; }
}