package com.perscholas.cashtran.service;

import com.perscholas.cashtran.model.Account;
import com.perscholas.cashtran.model.Authority;
import com.perscholas.cashtran.model.User;
import com.perscholas.cashtran.repository.AccountRepository;
import com.perscholas.cashtran.repository.AuthorityRepository;
import com.perscholas.cashtran.repository.UserRepository;
import jakarta.mail.MessagingException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {
  @Mock EmailService emailService;
  @Mock UserRepository userRepository;
  @Mock AuthorityRepository authorityRepository;
  @Mock AccountRepository accountRepository;
  @Mock PasswordEncoder passwordEncoder;
  @Mock MfaService mfaService;
  @InjectMocks UserService userService;

  @Test
  void createUserEncodesPasswordAssignsRoleAndCreatesFundedAccount() throws Exception {
    User user = new User();
    user.setUsername("alice");
    user.setEmail("alice@example.com");
    user.setPassword("plain");
    Authority role = new Authority("ROLE_USER");
    when(authorityRepository.findByAuthorityName("ROLE_USER")).thenReturn(Optional.of(role));
    when(passwordEncoder.encode("plain")).thenReturn("encoded");
    when(mfaService.generateSecret()).thenReturn("test-secret-123");
    when(userRepository.save(user)).thenReturn(user);

    assertSame(user, userService.createUser(user));

    assertTrue(user.isActivated());
    assertEquals("encoded", user.getPassword());
    assertTrue(user.getAuthorities().contains(role));
    ArgumentCaptor<com.perscholas.cashtran.model.Account> account =
        ArgumentCaptor.forClass(com.perscholas.cashtran.model.Account.class);
    verify(accountRepository).save(account.capture());
    assertSame(user, account.getValue().getUser());
    assertEquals(new BigDecimal("500.00"), account.getValue().getBalance());
    verify(emailService).sendEmail("alice@example.com", "alice");
  }

  @Test
  void createUserRejectsDuplicateEmailBeforeAnyWrite() {
    User user = new User();
    user.setEmail("alice@example.com");
    when(userRepository.existsByEmail(user.getEmail())).thenReturn(true);

    RuntimeException error =
        assertThrows(RuntimeException.class, () -> userService.createUser(user));

    assertEquals("Email already registered", error.getMessage());
    verifyNoMoreInteractions(
        userRepository, authorityRepository, accountRepository, passwordEncoder, emailService);
  }

  @Test
  void createUserRejectsDuplicateUsernameBeforeAnyWrite() {
    User user = new User();
    user.setUsername("alice");
    user.setEmail("alice@example.com");
    user.setPassword("plain");

    when(userRepository.existsByEmail("alice@example.com")).thenReturn(false);
    when(userRepository.existsByUsername("alice")).thenReturn(true);

    RuntimeException error =
        assertThrows(RuntimeException.class, () -> userService.createUser(user));

    assertEquals("Username already taken", error.getMessage());

    verify(userRepository).existsByEmail("alice@example.com");
    verify(userRepository).existsByUsername("alice");

    verify(passwordEncoder, never()).encode(anyString());
    verify(authorityRepository, never()).findByAuthorityName(anyString());
    verify(userRepository, never()).save(any(User.class));
    verify(accountRepository, never()).save(any(Account.class));
    verifyNoInteractions(emailService);
  }

  @Test
  void createUserRejectsWhenUserRoleDoesNotExist() {
    User user = new User();
    user.setUsername("alice");
    user.setEmail("alice@example.com");
    user.setPassword("plain");

    when(userRepository.existsByEmail("alice@example.com")).thenReturn(false);
    when(userRepository.existsByUsername("alice")).thenReturn(false);
    when(passwordEncoder.encode("plain")).thenReturn("encoded");

    when(authorityRepository.findByAuthorityName("ROLE_USER")).thenReturn(Optional.empty());

    RuntimeException error =
        assertThrows(RuntimeException.class, () -> userService.createUser(user));

    assertEquals("ROLE_USER authority not found", error.getMessage());

    verify(passwordEncoder).encode("plain");
    verify(authorityRepository).findByAuthorityName("ROLE_USER");

    verify(userRepository, never()).save(any(User.class));
    verify(accountRepository, never()).save(any(Account.class));
    verifyNoInteractions(emailService);
  }

  @Test
  void createUserStillSucceedsWhenWelcomeEmailFails() throws MessagingException {
    User user = new User();
    user.setUsername("alice");
    user.setEmail("alice@example.com");
    user.setPassword("plain");

    Authority role = new Authority("ROLE_USER");

    when(userRepository.existsByEmail("alice@example.com")).thenReturn(false);
    when(userRepository.existsByUsername("alice")).thenReturn(false);
    when(passwordEncoder.encode("plain")).thenReturn("encoded");
    when(authorityRepository.findByAuthorityName("ROLE_USER")).thenReturn(Optional.of(role));
    when(mfaService.generateSecret()).thenReturn("test-secret-456");
    when(userRepository.save(user)).thenReturn(user);

    doThrow(new RuntimeException("SMTP server unavailable"))
        .when(emailService)
        .sendEmail("alice@example.com", "alice");

    User result = userService.createUser(user);

    assertSame(user, result);
    assertTrue(result.isActivated());
    assertEquals("encoded", result.getPassword());
    assertTrue(result.getAuthorities().contains(role));

    verify(userRepository).save(user);
    verify(accountRepository).save(any(Account.class));
    verify(emailService).sendEmail("alice@example.com", "alice");
  }

  @Test
  void updateEmailRejectsUnknownUser() {
    when(userRepository.findByUsername("alice")).thenReturn(Optional.empty());

    RuntimeException error =
        assertThrows(
            RuntimeException.class, () -> userService.updateEmail("alice", "new@example.com"));

    assertEquals("User not found", error.getMessage());

    verify(userRepository).findByUsername("alice");
    verify(userRepository, never()).findByEmail(anyString());
    verify(userRepository, never()).save(any(User.class));
  }

  @Test
  void updateEmailRejectsSameEmailIgnoringCase() {
    User user = new User();
    user.setUserId(1L);
    user.setUsername("alice");
    user.setEmail("Alice@example.com");

    when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));

    org.springframework.web.server.ResponseStatusException error =
        assertThrows(
            org.springframework.web.server.ResponseStatusException.class,
            () -> userService.updateEmail("alice", "ALICE@EXAMPLE.COM"));

    assertEquals("New email is the same as current email", error.getReason());

    assertEquals("Alice@example.com", user.getEmail());

    verify(userRepository).findByUsername("alice");
    verify(userRepository, never()).findByEmail(anyString());
    verify(userRepository, never()).save(any(User.class));
  }

  @Test
  void updateEmailRejectsEmailAlreadyUsedByAnotherUser() {
    User currentUser = new User();
    currentUser.setUserId(1L);
    currentUser.setUsername("alice");
    currentUser.setEmail("alice@example.com");

    User existingUser = new User();
    existingUser.setUserId(2L);
    existingUser.setUsername("bob");
    existingUser.setEmail("bob@example.com");

    when(userRepository.findByUsername("alice")).thenReturn(Optional.of(currentUser));

    when(userRepository.findByEmail("bob@example.com")).thenReturn(Optional.of(existingUser));

    RuntimeException error =
        assertThrows(
            RuntimeException.class, () -> userService.updateEmail("alice", "bob@example.com"));

    assertEquals("Email already registered", error.getMessage());

    assertEquals("alice@example.com", currentUser.getEmail());

    verify(userRepository).findByUsername("alice");
    verify(userRepository).findByEmail("bob@example.com");
    verify(userRepository, never()).save(any(User.class));
  }

  @Test
  void updateEmailUpdatesEmailWhenNewEmailIsAvailable() {
    User user = new User();
    user.setUserId(1L);
    user.setUsername("alice");
    user.setEmail("alice@example.com");

    when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));

    when(userRepository.findByEmail("new@example.com")).thenReturn(Optional.empty());

    when(userRepository.save(user)).thenReturn(user);

    User result = userService.updateEmail("alice", "new@example.com");

    assertSame(user, result);
    assertEquals("new@example.com", user.getEmail());

    verify(userRepository).findByUsername("alice");
    verify(userRepository).findByEmail("new@example.com");
    verify(userRepository).save(user);
  }

  @Test
  void updateEmailAllowsEmailWhenExistingRecordBelongsToSameUser() {
    User user = new User();
    user.setUserId(1L);
    user.setUsername("alice");
    user.setEmail("old@example.com");

    User existingUser = new User();
    existingUser.setUserId(1L);
    existingUser.setUsername("alice");
    existingUser.setEmail("new@example.com");

    when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));

    when(userRepository.findByEmail("new@example.com")).thenReturn(Optional.of(existingUser));

    when(userRepository.save(user)).thenReturn(user);

    User result = userService.updateEmail("alice", "new@example.com");

    assertSame(user, result);
    assertEquals("new@example.com", user.getEmail());

    verify(userRepository).findByUsername("alice");
    verify(userRepository).findByEmail("new@example.com");
    verify(userRepository).save(user);
  }
}
