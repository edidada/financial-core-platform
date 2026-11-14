package com.fcp.app;

import com.fcp.common.api.ApiResult;
import com.fcp.common.api.ReverseRequest;
import com.fcp.common.api.TransactionResponse;
import com.fcp.common.api.TransferRequest;
import com.fcp.service.transaction.TransactionQueryService;
import com.fcp.service.transaction.TransferService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/transactions")
public class TransactionController {
    private final TransferService transfers;
    private final TransactionQueryService queries;

    public TransactionController(TransferService transfers, TransactionQueryService queries) {
        this.transfers = transfers;
        this.queries = queries;
    }

    @PostMapping
    public ApiResult<TransactionResponse> transfer(
            @RequestHeader("Idempotency-Key") String key,
            @RequestBody TransferRequest body) {
        TransferRequest req = new TransferRequest(body.tenantId(), key, body.payerAccountId(),
                body.payeeAccountId(), body.amountMinor(), body.currency());
        return ApiResult.ok(transfers.transfer(req));
    }

    @GetMapping("/{id}")
    public ApiResult<TransactionResponse> byId(@PathVariable long id) {
        return ApiResult.ok(queries.byId(id));
    }

    @GetMapping("/byKey/{key}")
    public ApiResult<TransactionResponse> byKey(
            @RequestParam long tenantId, @PathVariable String key) {
        return ApiResult.ok(queries.byKey(tenantId, key));
    }

    @PostMapping({"/{id}:reverse", "/{id}/reverse"})
    public ApiResult<TransactionResponse> reverse(
            @RequestHeader("Idempotency-Key") String key,
            @PathVariable long id,
            @RequestParam long tenantId,
            @RequestParam(required = false, defaultValue = "") String reason) {
        return ApiResult.ok(transfers.reverse(new ReverseRequest(tenantId, key, id, reason)));
    }
}