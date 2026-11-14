package com.fcp.service.account;

import com.fcp.common.domain.AccountStatus;
import com.fcp.common.domain.Currency;

import java.sql.Timestamp;

public record Account(
        long accountId,
        long tenantId,
        String accountNo,
        Currency currency,
        String ledgerSet,
        AccountStatus status,
        Timestamp createdAt) {}