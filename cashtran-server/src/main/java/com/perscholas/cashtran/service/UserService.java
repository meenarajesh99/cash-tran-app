package com.perscholas.cashtran.service;

import com.perscholas.cashtran.model.Account;
import com.perscholas.cashtran.model.Authority;
import com.perscholas.cashtran.model.User;
import com.perscholas.cashtran.repository.AccountRepository;
import com.perscholas.cashtran.repository.AuthorityRepository;
import com.perscholas.cashtran.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;

@Service
public class UserService {

  private final EmailService emailService;
  private final UserRepository userRepository;
  private final AuthorityRepository authorityRepository;
  private final AccountRepository accountRepository;
  private final PasswordEncoder passwordEncoder;

  private static final BigDecimal INITIAL_ACCOUNT_BALANCE = new BigDecimal("500.00");

  public UserService(
      EmailService emailService,
      UserRepository userRepository,
      AuthorityRepository authorityRepository,
      AccountRepository accountRepository,
      PasswordEncoder passwordEncoder) {

    this.emailService = emailService;
    this.userRepository = userRepository;
    this.authorityRepository = authorityRepository;
    this.accountRepository = accountRepository;
    this.passwordEncoder = passwordEncoder;
  }

  public User createUser(User user) {
    if (userRepository.existsByEmail(user.getEmail())) {
      throw new RuntimeException("Email already registered");
    }

    if (userRepository.existsByUsername(user.getUsername())) {
      throw new RuntimeException("Username already taken");
    }
    user.setPassword(passwordEncoder.encode(user.getPassword()));

    user.setActivated(true);

    Authority userRole =
        authorityRepository
            .findByAuthorityName("ROLE_USER")
            .orElseThrow(() -> new RuntimeException("ROLE_USER authority not found"));

    Set<Authority> authorities = new HashSet<>();
    authorities.add(userRole);
    user.setAuthorities(authorities);

    User savedUser = userRepository.save(user);

    // Create account for new user
    Account account = new Account();
    account.setUser(savedUser);
    account.setBalance(INITIAL_ACCOUNT_BALANCE);

    accountRepository.save(account);

    try {
      System.out.println("Preparing to send email to: " + savedUser.getEmail());
      emailService.sendEmail(savedUser.getEmail(), savedUser.getUsername());
      System.out.println("Welcome email sent successfully to: " + savedUser.getEmail());

    } catch (Exception e) {
      System.err.println("Unable to send welcome email: " + e.getMessage());
    }

    return savedUser;
  }

  @Transactional
  public User updateEmail(String username, String newEmail) {

    User user =
        userRepository
            .findByUsername(username)
            .orElseThrow(() -> new RuntimeException("User not found"));

    if (user.getEmail().equalsIgnoreCase(newEmail)) {
      throw new RuntimeException("New email is the same as current email");
    }

    userRepository
        .findByEmail(newEmail)
        .ifPresent(
            existingUser -> {
              if (!existingUser.getUserId().equals(user.getUserId())) {
                throw new RuntimeException("Email already registered");
              }
            });

    user.setEmail(newEmail);

    return userRepository.save(user);
  }

  public User getUserByUsername(String username) {
    return userRepository
        .findByUsername(username)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
  }
}
