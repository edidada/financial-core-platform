package com.fcp.engine.batch;

import java.util.ArrayList;
import java.util.List;

/** 大集合按固定大小切块，控制单批事务规模。 */
public final class ChunkSplitter {
    private ChunkSplitter() {}

    public static <T> List<List<T>> split(List<T> items, int chunkSize) {
        if (chunkSize < 1) throw new IllegalArgumentException("chunkSize must be >= 1");
        List<List<T>> chunks = new ArrayList<>();
        for (int i = 0; i < items.size(); i += chunkSize) {
            chunks.add(List.copyOf(items.subList(i, Math.min(items.size(), i + chunkSize))));
        }
        return chunks;
    }
}