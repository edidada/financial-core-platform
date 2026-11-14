package com.fcp.app;

import com.fcp.common.api.ApiResult;
import com.fcp.common.api.InvariantReport;
import com.fcp.service.ledger.LedgerInvariantChecker;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/invariants")
public class InvariantController {
    private final LedgerInvariantChecker checker;

    public InvariantController(LedgerInvariantChecker checker) {
        this.checker = checker;
    }

    /** 资金不变量 I-1~I-4 离线校验；violations 全为 0 才 healthy。 */
    @GetMapping
    public ApiResult<InvariantReport> check() {
        return ApiResult.ok(checker.check());
    }
}