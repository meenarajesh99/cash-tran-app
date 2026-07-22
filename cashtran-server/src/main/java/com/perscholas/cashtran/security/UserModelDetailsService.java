package com.perscholas.cashtran.security;

import com.perscholas.cashtran.exception.UserNotActivatedException;
import com.perscholas.cashtran.model.User;
import com.perscholas.cashtran.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * UserDetailsService implementation - Loads user details from database
 * Implements Spring Security's UserDetailsService for authentication
 *
 * Database Schema:
 * - cashtran_user table: user_id (PK), username (UNIQUE), password_hash
 * - Uses default USER role for all users (extensible for role-based access)
 */
@Component("userDetailsService")
public class UserModelDetailsService implements UserDetailsService {

    private static final Logger log = LoggerFactory.getLogger(UserModelDetailsService.class);
    private static final String DEFAULT_ROLE = "ROLE_USER";

    private final UserRepository userRepository;

    public UserModelDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Load user details by username for authentication
     * @param username the username provided during login
     * @return UserDetails object for Spring Security
     * @throws UsernameNotFoundException if user not found or not activated
     */
    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) {
        log.debug("Authenticating user '{}'", username);

        Optional<User> user = userRepository.findByUsername(username);

        if (!user.isPresent()) {
            throw new UsernameNotFoundException("User not found: " + username);
        }

        return createSpringSecurityUser(user.get());
    }

    /**
     * Converts domain User object to Spring Security UserDetails
     * @param user domain User object from database
     * @return Spring Security User (UserDetails)
     */
    private org.springframework.security.core.userdetails.User createSpringSecurityUser(User user) {

        // Verify user is activated
        if (!user.isActivated()) {
            throw new UserNotActivatedException("User " + user.getUsername() + " is not activated");
        }

        // Map authorities from domain model to Spring Security authorities
        Set<GrantedAuthority> authorities = new HashSet<>();

        if (user.getAuthorities() != null && !user.getAuthorities().isEmpty()) {
            authorities.addAll(
                    user.getAuthorities().stream()
                            .map(auth -> new SimpleGrantedAuthority(auth.getAuthorityName()))
                            .collect(Collectors.toList())
            );
        } else {
            // Default role if none assigned
            authorities.add(new SimpleGrantedAuthority(DEFAULT_ROLE));
        }

        log.debug("Created Spring Security user '{}' with {} authorities",
                user.getUsername(), authorities.size());

        return (org.springframework.security.core.userdetails.User) org.springframework.security.core.userdetails.User.builder()
                .username(user.getUsername())
                .password(user.getPassword())
                .authorities(authorities)
                .accountExpired(false)
                .accountLocked(false)
                .credentialsExpired(false)
                .disabled(!user.isActivated())
                .build();
    }
}