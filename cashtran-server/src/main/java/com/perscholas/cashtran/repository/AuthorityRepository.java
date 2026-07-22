package com.perscholas.cashtran.repository;

import com.perscholas.cashtran.model.Authority;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AuthorityRepository
        extends JpaRepository<Authority, String> {
    Optional<Authority> findByAuthorityName(String authorityName);

}