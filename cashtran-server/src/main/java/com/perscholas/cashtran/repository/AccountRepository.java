package com.perscholas.cashtran.repository;
import com.perscholas.cashtran.model.Account;
import java.math.BigDecimal;

public interface AccountRepository {

    BigDecimal getBalance(long id);

    Account getAnAccountByUserId(long userId);

    void addBalance(BigDecimal amount, long accountId);

    boolean subtractBalance(BigDecimal amount, long accountId);
}

