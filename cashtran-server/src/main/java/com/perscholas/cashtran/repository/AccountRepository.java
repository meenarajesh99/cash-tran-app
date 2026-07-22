package com.perscholas.cashtran.repository;

import com.perscholas.cashtran.model.Account;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface AccountRepository extends JpaRepository<Account, Long> {

    @Query("""
        SELECT a
        FROM Account a
        WHERE a.user.userId = :userId
    """)
    Optional<Account> findByUserId(
            @Param("userId") Long userId
    );
}