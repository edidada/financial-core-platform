package com.fcp.engine.hotspot;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 阶段五专题：热点账户缓冲净额引擎。
 *
 * 问题：热点账户（如平台归集户）在 1000+ TPS 下每条交易都做 SELECT FOR UPDATE + UPDATE，
 * 行锁竞争导致吞吐急剧退化（README 6.1 热点账户测试）。
 *
 * 方案：在不破坏资金正确性的前提下，把同一账户在时间窗内的多笔变动先按方向累加成
 * 「净额」，再一次落库。关键约束：
 *  1) 净额结果必须仍满足 ΣDebit = ΣCredit（借贷平衡）；
 *  2) 每笔原始交易仍保留各自的 bizId，冲正可追溯；
 *  3) 缓冲只在内存中聚合，最终落库仍走同一本地事务与条件更新（不绕过 ADR-0003）；
 *  4) 净额不能掩盖余额不足：逐笔准入时先校验「已缓冲借方累计 + 本笔 <= 当前可用余额」。
 *
 * 本类只负责「聚合与准入判定」这一纯计算部分，落库由调用方事务完成，便于属性测试证明不变量。
 */
public final class HotspotBuffer {
    private final ReentrantLock lock = new ReentrantLock();
    private final LinkedHashMap<String, Pending> pending = new LinkedHashMap<>();
    private final long availableBalance;

    public HotspotBuffer(long availableBalance) { this.availableBalance = availableBalance; }

    /** @return true 表示准入；false 表示余额不足，必须拒绝（不超扣） */
    public boolean admit(String bizId, long accountId, boolean debit, long amountMinor) {
        if (amountMinor <= 0) throw new IllegalArgumentException("amount must be positive");
        lock.lock();
        try {
            long bufferedDebit = 0;
            for (Pending p : pending.values()) {
                if (p.accountId == accountId && p.debit) bufferedDebit += p.amountMinor;
            }
            if (debit && bufferedDebit + amountMinor > availableBalance) return false;
            pending.merge(bizId, new Pending(accountId, debit, amountMinor), (a, b) -> a);
            return true;
        } finally {
            lock.unlock();
        }
    }

    /** 折叠成净额：按 (账户, 方向) 汇总，返回可一次性落库的净额指令。 */
    public List<NetEntry> drain() {
        lock.lock();
        try {
            Map<AccountSide, Long> sums = new LinkedHashMap<>();
            for (Pending p : pending.values()) {
                sums.merge(new AccountSide(p.accountId, p.debit), p.amountMinor, Long::sum);
            }
            List<NetEntry> out = new ArrayList<>();
            for (Map.Entry<AccountSide, Long> e : sums.entrySet()) {
                out.add(new NetEntry(e.getKey().accountId(), e.getKey().debit(), e.getValue(), pending.size()));
            }
            pending.clear();
            return List.copyOf(out);
        } finally {
            lock.unlock();
        }
    }

    /** 净额折叠后借贷是否仍平衡（属性测试断言点）。 */
    public static boolean balanced(List<NetEntry> net) {
        long d = 0, c = 0;
        for (NetEntry e : net) { if (e.debit()) d += e.amountMinor(); else c += e.amountMinor(); }
        return d == c;
    }

    public int size() { lock.lock(); try { return pending.size(); } finally { lock.unlock(); } }

    public record NetEntry(long accountId, boolean debit, long amountMinor, int originalCount) {}
    private record AccountSide(long accountId, boolean debit) {}
    private record Pending(long accountId, boolean debit, long amountMinor) {}
}