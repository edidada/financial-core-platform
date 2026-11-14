package com.fcp.service.transaction;

import com.fcp.common.domain.TransactionStatus;
import com.fcp.common.exception.InvalidStateTransitionException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TransactionStateMachineTest {

    @Test
    void legalChainPasses() {
        assertDoesNotThrow(() -> {
            TransactionStateMachine.assertTransition(TransactionStatus.CREATED, TransactionStatus.VALIDATED);
            TransactionStateMachine.assertTransition(TransactionStatus.VALIDATED, TransactionStatus.PROCESSING);
            TransactionStateMachine.assertTransition(TransactionStatus.PROCESSING, TransactionStatus.SUCCEEDED);
            TransactionStateMachine.assertTransition(TransactionStatus.SUCCEEDED, TransactionStatus.REVERSED);
        });
    }

    @Test
    void failedCannotBecomeSucceeded() {
        assertThrows(InvalidStateTransitionException.class, () -> TransactionStateMachine
                .assertTransition(TransactionStatus.FAILED, TransactionStatus.SUCCEEDED));
    }

    @Test
    void doubleReversalRejected() {
        assertThrows(InvalidStateTransitionException.class, () -> TransactionStateMachine
                .assertTransition(TransactionStatus.REVERSED, TransactionStatus.REVERSED));
    }
}