package com.fcp.service.clearing;

import java.util.List;

public record ClearingBatch(long batchId, String window, String currency, List<ClearingItem> items) {
    /** 可重算、可追溯：净额合计必须为 0 */
    public long netSum() { return items.stream().mapToLong(ClearingItem::netMinor).sum(); }
}