package com.perscholas.cashtran.repository;

import com.perscholas.cashtran.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    Optional<User> findByUserId(Long userId);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);
    @Query("""
    SELECT u
    FROM User u
    LEFT JOIN FETCH u.account
    WHERE u.username = :username
""")
    Optional<User> findByUsernameWithAccount(
            @Param("username") String username
    );

}