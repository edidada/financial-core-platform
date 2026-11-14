package com.fcp.service.account;

import com.fcp.common.domain.AccountStatus;
import com.fcp.common.domain.Currency;
import com.fcp.common.utils.IdGenerator;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Repository
public class AccountRepository {
    private static final RowMapper<Account> ACCOUNT_MAPPER = (rs, i) -> new Account(
            rs.getLong("account_id"), rs.getLong("tenant_id"), rs.getString("account_no"),
            Currency.of(rs.getString("currency")), rs.getString("ledger_set"),
            AccountStatus.valueOf(rs.getString("status")), rs.getTimestamp("created_at"));

    private static final RowMapper<AccountBalance> BALANCE_MAPPER = (rs, i) -> new AccountBalance(
            rs.getLong("account_id"), rs.getLong("opening_balance"), rs.getLong("available_balance"),
            rs.getLong("frozen_balance"), rs.getLong("version"), rs.getTimestamp("updated_at"));

    private static final String BALANCE_COLUMNS =
            "account_id, opening_balance, available_balance, frozen_balance, version, updated_at";

    private final JdbcTemplate jdbc;
    private final IdGenerator ids;

    public AccountRepository(JdbcTemplate jdbc, IdGenerator ids) {
        this.jdbc = jdbc;
        this.ids = ids;
    }

    /** 开户：期初余额由应用层写入，不产生分录；I-2 以 opening_balance 为基线。 */
    @Transactional
    public long create(long tenantId, String accountNo, Currency currency, long openingBalanceMinor, Timestamp now) {
        long id = ids.nextId();
        jdbc.update("INSERT INTO account "
                        + "(account_id, tenant_id, account_no, currency, ledger_set, status, created_at) "
                        + "VALUES (?,?,?,?,?,?,?)",
                id, tenantId, accountNo, currency.code(), "DEFAULT", AccountStatus.ACTIVE.name(), now);
        jdbc.update("INSERT INTO account_balance (" + BALANCE_COLUMNS + ") VALUES (?,?,?,?,?,?)",
                id, openingBalanceMinor, openingBalanceMinor, 0L, 0L, now);
        return id;
    }

    public Optional<Account> findAccount(long accountId) {
        return jdbc.query("SELECT account_id, tenant_id, account_no, currency, ledger_set, status, created_at "
                + "FROM account WHERE account_id = ?", ACCOUNT_MAPPER, accountId).stream().findFirst();
    }

    public Optional<AccountBalance> findBalance(long accountId) {
        return jdbc.query("SELECT " + BALANCE_COLUMNS + " FROM account_balance WHERE account_id = ?",
                BALANCE_MAPPER, accountId).stream().findFirst();
    }

    /** 按 account_id 排序加锁，避免 A->B 与 B->A 并发转账死锁。 */
    public List<AccountBalance> lockForUpdate(List<Long> accountIds) {
        List<Long> sorted = new ArrayList<>(accountIds);
        Collections.sort(sorted);
        List<AccountBalance> result = new ArrayList<>();
        for (Long id : sorted) {
            jdbc.query("SELECT " + BALANCE_COLUMNS + " FROM account_balance WHERE account_id = ? FOR UPDATE",
                    BALANCE_MAPPER, id).stream().findFirst().ifPresent(result::add);
        }
        return result;
    }

    /** 条件更新：检查与扣减在同一原子语句 (I-4)。返回影响行数。 */
    public int conditionalDebit(long accountId, long amountMinor, Timestamp now) {
        return jdbc.update("UPDATE account_balance SET available_balance = available_balance - ?, "
                        + "version = version + 1, updated_at = ? "
                        + "WHERE account_id = ? AND available_balance >= ?",
                amountMinor, now, accountId, amountMinor);
    }

    public int credit(long accountId, long amountMinor, Timestamp now) {
        return jdbc.update("UPDATE account_balance SET available_balance = available_balance + ?, "
                        + "version = version + 1, updated_at = ? WHERE account_id = ?",
                amountMinor, now, accountId);
    }
}