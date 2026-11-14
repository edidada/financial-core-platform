package com.fcp.service.transaction;

import com.fcp.common.domain.TransactionStatus;
import com.fcp.common.exception.InvalidStateTransitionException;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

import static com.fcp.common.domain.TransactionStatus.*;

/** 交易状态机：非法跳转一律拒绝（transaction-design.md 第二节）。 */
public final class TransactionStateMachine {
    private static final Map<TransactionStatus, Set<TransactionStatus>> ALLOWED =
            new EnumMap<>(TransactionStatus.class);

    static {
        ALLOWED.put(CREATED, EnumSet.of(VALIDATED, FAILED));
        ALLOWED.put(VALIDATED, EnumSet.of(PROCESSING));
        ALLOWED.put(PROCESSING, EnumSet.of(SUCCEEDED, FAILED, UNKNOWN));
        ALLOWED.put(UNKNOWN, EnumSet.of(RECOVERING));
        ALLOWED.put(RECOVERING, EnumSet.of(SUCCEEDED, FAILED, UNKNOWN));
        ALLOWED.put(SUCCEEDED, EnumSet.of(REVERSED));
        ALLOWED.put(FAILED, EnumSet.noneOf(TransactionStatus.class));
        ALLOWED.put(REVERSED, EnumSet.noneOf(TransactionStatus.class));
    }

    private TransactionStateMachine() {}

    public static boolean canTransition(TransactionStatus from, TransactionStatus to) {
        return ALLOWED.getOrDefault(from, Set.of()).contains(to);
    }

    public static void assertTransition(TransactionStatus from, TransactionStatus to) {
        if (!canTransition(from, to)) {
            throw new InvalidStateTransitionException(from, to);
        }
    }
}