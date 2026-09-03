package com.perscholas.cashtran.service;

import com.perscholas.cashtran.exception.EmailAlreadyRegisteredException;
import com.perscholas.cashtran.exception.UsernameAlreadyTakenException;
import com.perscholas.cashtran.model.Account;
import com.perscholas.cashtran.model.Authority;
import com.perscholas.cashtran.model.User;
import com.perscholas.cashtran.repository.AccountRepository;
import com.perscholas.cashtran.repository.AuthorityRepository;
import com.perscholas.cashtran.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;

@Service
public class UserService {

  private static final Logger log = LoggerFactory.getLogger(UserService.class);
  private final EmailService emailService;
  private final UserRepository userRepository;
  private final AuthorityRepository authorityRepository;
  private final AccountRepository accountRepository;
  private final PasswordEncoder passwordEncoder;
  private final MfaService mfaService;

  private static final BigDecimal INITIAL_ACCOUNT_BALANCE = new BigDecimal("500.00");

  public UserService(
      EmailService emailService,
      UserRepository userRepository,
      AuthorityRepository authorityRepository,
      AccountRepository accountRepository,
      PasswordEncoder passwordEncoder,
      MfaService mfaService) {

    this.emailService = emailService;
    this.userRepository = userRepository;
    this.authorityRepository = authorityRepository;
    this.accountRepository = accountRepository;
    this.passwordEncoder = passwordEncoder;
    this.mfaService = mfaService;
  }

  @Transactional
  public User createUser(User user) {
    if (userRepository.existsByEmail(user.getEmail())) {
      throw new EmailAlreadyRegisteredException();
    }

    if (userRepository.existsByUsername(user.getUsername())) {
      throw new UsernameAlreadyTakenException();
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
    /*
     * Automatically create a unique MFA secret
     * for every newly registered user.
     */

    String mfaSecret = mfaService.generateSecret();
    user.setMfaSecret(mfaSecret);
    user.setMfaEnabled(false);

    User savedUser = userRepository.save(user);

    // Create account for new user
    Account account = new Account();
    account.setUser(savedUser);
    account.setBalance(INITIAL_ACCOUNT_BALANCE);

    accountRepository.save(account);

    try {
      log.info("Preparing to send email to: " + savedUser.getEmail());
      emailService.sendEmail(savedUser.getEmail(), savedUser.getUsername());
      log.info("Welcome email sent successfully to: " + savedUser.getEmail());

    } catch (Exception e) {
      log.error("Unable to send welcome email: " + savedUser.getEmail() + ": " + e.getMessage());
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
      throw new org.springframework.web.server.ResponseStatusException(
          org.springframework.http.HttpStatus.BAD_REQUEST,
          "New email is the same as current email");
    }

    userRepository
        .findByEmail(newEmail)
        .ifPresent(
            existingUser -> {
              if (!existingUser.getUserId().equals(user.getUserId())) {
                throw new EmailAlreadyRegisteredException();
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
