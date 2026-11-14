package com.fcp.app;

import com.fcp.common.api.AccountResponse;
import com.fcp.common.api.ApiResult;
import com.fcp.common.api.CreateAccountRequest;
import com.fcp.common.domain.Currency;
import com.fcp.common.exception.ResourceNotFoundException;
import com.fcp.service.account.Account;
import com.fcp.service.account.AccountBalance;
import com.fcp.service.account.AccountRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.sql.Timestamp;
import java.time.Instant;

@RestController
@RequestMapping("/v1/accounts")
public class AccountController {
    private final AccountRepository accounts;

    public AccountController(AccountRepository accounts) {
        this.accounts = accounts;
    }

    @PostMapping
    public ApiResult<AccountResponse> create(@RequestBody CreateAccountRequest req) {
        long opening = req.initialBalanceMinor() == null ? 0L : req.initialBalanceMinor();
        if (opening < 0) throw new IllegalArgumentException("initialBalanceMinor must be >= 0");
        Currency currency = Currency.of(req.currency());
        long id = accounts.create(req.tenantId(), req.accountNo(), currency, opening,
                Timestamp.from(Instant.now()));
        return ApiResult.ok(load(id));
    }

    @GetMapping("/{id}")
    public ApiResult<AccountResponse> get(@PathVariable long id) {
        return ApiResult.ok(load(id));
    }

    private AccountResponse load(long id) {
        Account a = accounts.findAccount(id)
                .orElseThrow(() -> new ResourceNotFoundException("Account", id));
        AccountBalance b = accounts.findBalance(id)
                .orElseThrow(() -> new ResourceNotFoundException("Account balance", id));
        return new AccountResponse(a.accountId(), a.accountNo(), a.currency().code(),
                b.openingBalance(), b.availableBalance(), b.frozenBalance(), b.version());
    }
}