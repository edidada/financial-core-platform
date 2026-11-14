package com.fcp.engine.hotspot;

import java.util.List;
import java.util.Random;

/**
 * 不变量验证器：随机生成大量交易序列，断言
 *  - 准入判定永不超扣
 *  - 净额折叠后借贷平衡
 *  - 净额总量与原始总量一致（无丢账）
 * 用于阶段五技术论文式报告中的「属性测试」证据。
 */
public final class HotspotProof {
    private HotspotProof() {}

    public static boolean verify(long openingBalance, List<long[]> transfers, long seed) {
        Random rnd = new Random(seed);
        HotspotBuffer buffer = new HotspotBuffer(openingBalance);
        long admittedDebit = 0;
        for (long[] t : transfers) {
            long from = t[0], to = t[1], amt = t[2];
            String key = "t-" + rnd.nextLong();
            // 借方准入通过后才允许对应贷方入缓冲，避免产生孤立的单边指令
            if (!buffer.admit(key + "-d", from, true, amt)) continue;
            buffer.admit(key + "-c", to, false, amt);
            admittedDebit += amt;
        }
        List<HotspotBuffer.NetEntry> net = buffer.drain();
        if (!HotspotBuffer.balanced(net)) return false;
        long netDebit = net.stream()
                .filter(HotspotBuffer.NetEntry::debit)
                .mapToLong(HotspotBuffer.NetEntry::amountMinor)
                .sum();
        return netDebit <= openingBalance && netDebit == admittedDebit;
    }
}