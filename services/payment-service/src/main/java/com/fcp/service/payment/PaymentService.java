package com.fcp.service.payment;

import com.fcp.common.domain.Currency;
import com.fcp.common.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Optional;

/** 支付指令服务：外部渠道结果可能延迟/不确定，因此 UNKNOWN 不等于失败（README 4.3）。 */
@Service
public class PaymentService {
    private final PaymentRepository repository;

    public PaymentService(PaymentRepository repository) { this.repository = repository; }

    public long submit(long transactionId, long payer, long payee, long amountMinor,
                       Currency currency, String channel, String externalRef) {
        long id = repository.create(transactionId, payer, payee, amountMinor, currency, channel, externalRef);
        if (repository.casStatus(id, PaymentStatus.CREATED, PaymentStatus.SUBMITTED, 0L) != 1) {
            throw new IllegalStateException("payment state changed concurrently: " + id);
        }
        return id;
    }

    public PaymentInstruction accept(long id) {
        PaymentInstruction p = require(id);
        if (repository.casStatus(id, PaymentStatus.SUBMITTED, PaymentStatus.ACCEPTED, p.version()) != 1) {
            throw new IllegalStateException("cannot accept payment in state " + p.status());
        }
        return require(id);
    }

    public PaymentInstruction markUnknown(long id) {
        PaymentInstruction p = require(id);
        if (p.status() == PaymentStatus.SUBMITTED) {
            repository.casStatus(id, PaymentStatus.SUBMITTED, PaymentStatus.UNKNOWN, p.version());
        }
        return require(id);
    }

    public PaymentInstruction settle(long id) {
        PaymentInstruction p = require(id);
        if (p.status() != PaymentStatus.ACCEPTED) {
            throw new IllegalStateException("only ACCEPTED payment can settle, got " + p.status());
        }
        if (repository.casStatus(id, PaymentStatus.ACCEPTED, PaymentStatus.SETTLED, p.version()) != 1) {
            throw new IllegalStateException("payment state changed concurrently: " + id);
        }
        return require(id);
    }

    public Optional<PaymentInstruction> findById(long id) { return repository.findById(id); }

    private PaymentInstruction require(long id) {
        return repository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Payment", id));
    }
}