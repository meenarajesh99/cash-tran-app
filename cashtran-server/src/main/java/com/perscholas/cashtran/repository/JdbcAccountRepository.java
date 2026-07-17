package com.perscholas.cashtran.repository;

import com.perscholas.cashtran.model.Account;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.rowset.SqlRowSet;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.math.BigDecimal;

@Component
public class JdbcAccountRepository implements AccountRepository {

    private JdbcTemplate jdbcTemplate;

    public JdbcAccountRepository(DataSource ds) {
        this.jdbcTemplate = new JdbcTemplate(ds);
    }

    // SqlRowSet Object Mapper :)
    private Account accountObjectMapper(SqlRowSet results) {
        Account account = new Account();
        account.setAccountId(results.getLong("account_id"));
        account.setBalance(results.getBigDecimal("balance"));

        return account;
    }


    // override methods

    //gets user balance
    @Override
    public BigDecimal getBalance(long userId) {

        String sql = "SELECT balance FROM account WHERE user_id = ?;";
        return jdbcTemplate.queryForObject(sql, BigDecimal.class, userId);
    }

    //gets user ID
    @Override
    public Account getAnAccountByUserId(long userId) {
        String sql = "SELECT * FROM account WHERE user_id = ?";
        SqlRowSet results = this.jdbcTemplate.queryForRowSet(sql, userId);

        Account account = null;
        if (results.next()) {
            account = accountObjectMapper(results);
        }

        return account;
    }

    //updates user balance after deposit transaction
    @Override
    public void addBalance(BigDecimal amount, long userId) {

        String sql = "UPDATE account SET balance = balance + ? WHERE user_id = ?";
        jdbcTemplate.update(sql, amount, userId);

    }

    //updates user balance after withdrawal transaction
    @Override
    public boolean subtractBalance(BigDecimal amount, long userId) {
        Account account = getAnAccountByUserId(userId);
        int res = account.getBalance().compareTo(amount);

        if (res == 1 || res == 0) {
            String sql = "UPDATE account SET balance = balance - ? WHERE user_id = ?";
            jdbcTemplate.update(sql, amount, userId);

            return true;
        } else {
            return false;
        }


    }
}

