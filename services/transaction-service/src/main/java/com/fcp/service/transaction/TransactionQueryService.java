package com.fcp.service.transaction;

import com.fcp.common.api.TransactionResponse;
import com.fcp.common.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class TransactionQueryService {
    private final TransactionRepository transactions;

    public TransactionQueryService(TransactionRepository transactions) {
        this.transactions = transactions;
    }

    public TransactionResponse byId(long id) {
        return transactions.findById(id).map(TransferService::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction", id));
    }

    public TransactionResponse byKey(long tenantId, String key) {
        return transactions.findByKey(tenantId, key).map(TransferService::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction by key", key));
    }
}