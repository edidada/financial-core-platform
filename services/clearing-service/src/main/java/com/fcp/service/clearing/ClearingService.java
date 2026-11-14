package com.fcp.service.clearing;

import com.fcp.common.exception.UnbalancedJournalException;
import com.fcp.engine.batch.BatchResult;
import com.fcp.engine.batch.ChunkSplitter;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** 清分清算：按时间窗把多笔交易按账户汇总成净额，结果可重算、可追溯（README 第四节）。 */
@Service
public class ClearingService {
    private final com.fcp.common.utils.IdGenerator ids;

    public ClearingService(com.fcp.common.utils.IdGenerator ids) { this.ids = ids; }

    public ClearingBatch net(String window, String currency, List<long[]> raw) {
        Map<Long, long[]> sums = new LinkedHashMap<>();  // [debit, credit]
        for (long[] r : raw) {
            long[] s = sums.computeIfAbsent(r[0], k -> new long[2]);
            s[0] += r[1];  // debit
            s[1] += r[2];  // credit
        }
        List<ClearingItem> items = new ArrayList<>();
        for (Map.Entry<Long, long[]> e : sums.entrySet()) {
            long[] s = e.getValue();
            items.add(new ClearingItem(e.getKey(), s[0], s[1], s[1] - s[0], currency));
        }
        ClearingBatch batch = new ClearingBatch(ids.nextId(), window, currency, List.copyOf(items));
        if (batch.netSum() != 0) throw new UnbalancedJournalException("clearing net sum = " + batch.netSum());
        return batch;
    }

    /** 并行分块清算，用于阶段三大批量数据 */
    public ClearingResult clearParallel(String window, String currency, List<long[]> raw, int chunkSize) {
        long start = System.currentTimeMillis();
        List<List<long[]>> chunks = ChunkSplitter.split(raw, chunkSize);
        Map<Long, long[]> sums = new ConcurrentHashMap<>();
        com.fcp.engine.batch.BatchEngine engine = new com.fcp.engine.batch.BatchEngine(chunks.size());
        try {
            BatchResult br = engine.run(chunks, chunk -> {
                for (long[] r : chunk) {
                    long[] s = sums.computeIfAbsent(r[0], k -> new long[2]);
                    synchronized (s) { s[0] += r[1]; s[1] += r[2]; }
                }
            });
            long net = 0;
            for (long[] s : sums.values()) net += s[1] - s[0];
            return new ClearingResult(ids.nextId(), sums.size(), net, net == 0 && br.allSucceeded(),
                    System.currentTimeMillis() - start);
        } finally {
            engine.close();
        }
    }
}