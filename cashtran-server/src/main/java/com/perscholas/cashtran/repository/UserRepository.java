package com.perscholas.cashtran.repository;

import com.perscholas.cashtran.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    Optional<User> findByUserId(Long userId);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

}