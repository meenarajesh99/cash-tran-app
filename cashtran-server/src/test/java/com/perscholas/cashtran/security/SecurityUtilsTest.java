package com.perscholas.cashtran.security;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class SecurityUtilsTest {

    @AfterEach
    void cleanup() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void returnsEmptyWhenNoAuthentication() {
        SecurityContextHolder.clearContext();
        Optional<String> username = SecurityUtils.getCurrentUsername();
        assertTrue(username.isEmpty());
    }

    @Test
    void returnsPrincipalString() {
        var auth = new UsernamePasswordAuthenticationToken("bob", "credentials");
        SecurityContextHolder.getContext().setAuthentication(auth);

        Optional<String> username = SecurityUtils.getCurrentUsername();
        assertTrue(username.isPresent());
        assertEquals("bob", username.get());
    }

    @Test
    void returnsUserDetailsUsername() {
        User user = new User("alice", "pwd", java.util.Collections.emptyList());
        var auth = new UsernamePasswordAuthenticationToken(user, "pwd");
        SecurityContextHolder.getContext().setAuthentication(auth);

        Optional<String> username = SecurityUtils.getCurrentUsername();
        assertTrue(username.isPresent());
        assertEquals("alice", username.get());
    }
}

