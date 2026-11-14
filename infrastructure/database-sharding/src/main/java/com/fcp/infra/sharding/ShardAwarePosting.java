package com.fcp.infra.sharding;

/** 跨分片交易判定：阶段一/二恒为同分片，阶段三据此触发分布式事务协议。 */
public final class ShardAwarePosting {
    private final ShardRouter router;

    public ShardAwarePosting(ShardRouter router) { this.router = router; }

    public boolean sameShard(long payerAccountId, long payeeAccountId) {
        return router.shardOf(payerAccountId) == router.shardOf(payeeAccountId);
    }
}