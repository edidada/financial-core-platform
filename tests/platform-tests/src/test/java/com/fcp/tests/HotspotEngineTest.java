package com.fcp.tests;

import com.fcp.engine.hotspot.HotspotBuffer;
import com.fcp.engine.hotspot.HotspotProof;
import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import static org.junit.jupiter.api.Assertions.*;

class HotspotEngineTest {
    @Test void neverOverdrafts() {
        HotspotBuffer b = new HotspotBuffer(100L);
        assertTrue(b.admit("a", 1L, true, 60L));
        assertTrue(b.admit("b", 2L, false, 60L));
        assertFalse(b.admit("c", 1L, true, 50L));   // 60+50 > 100
        assertEquals(2, b.size());
    }

    @Test void nettingKeepsBalance() {
        HotspotBuffer b = new HotspotBuffer(1000L);
        for (int i = 0; i < 100; i++) {
            b.admit("d" + i, 1L, true, 5L);
            b.admit("c" + i, 2L, false, 5L);
        }
        var net = b.drain();
        assertTrue(HotspotBuffer.balanced(net));
        assertEquals(500L, net.stream().filter(HotspotBuffer.NetEntry::debit).mapToLong(HotspotBuffer.NetEntry::amountMinor).sum());
    }

    @Test void randomizedInvariants() {
        Random rnd = new Random(42);
        List<long[]> seq = new ArrayList<>();
        for (int i = 0; i < 5_000; i++) {
            seq.add(new long[]{1L, 2L, 1L + rnd.nextInt(20)});
        }
        assertTrue(HotspotProof.verify(1_000_000L, seq, 42L));
    }
}