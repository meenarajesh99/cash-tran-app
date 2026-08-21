package com.perscholas.cashtran.service;

import com.perscholas.cashtran.model.PasswordResetToken;
import com.perscholas.cashtran.model.User;
import com.perscholas.cashtran.repository.PasswordResetTokenRepository;
import com.perscholas.cashtran.repository.UserRepository;
import jakarta.mail.MessagingException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PasswordResetServiceTest {

  @Mock private UserRepository userRepository;

  @Mock private PasswordResetTokenRepository tokenRepository;

  @Mock private EmailService emailService;

  @Mock private PasswordEncoder passwordEncoder;

  private PasswordResetService passwordResetService;

  @BeforeEach
  void setUp() {
    passwordResetService =
        new PasswordResetService(
            userRepository,
            tokenRepository,
            emailService,
            passwordEncoder,
            "http://localhost:5173");
  }

  @Test
  void forgotPasswordSendsEmailForExistingUser() throws MessagingException {

    User user = createUser();

    when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));

    passwordResetService.requestPasswordReset("user@example.com");

    verify(tokenRepository).deleteByUser_UserId(user.getUserId());

    verify(tokenRepository).save(any(PasswordResetToken.class));

    verify(emailService)
        .sendPasswordResetEmail(
            eq("user@example.com"),
            eq("testuser"),
            argThat(link -> link.startsWith("http://localhost:5173/reset-password?token=")));
  }

  @Test
  void forgotPasswordDoesNotRevealUnknownEmail() throws MessagingException {

    when(userRepository.findByEmail("unknown@example.com")).thenReturn(Optional.empty());

    passwordResetService.requestPasswordReset("unknown@example.com");

    verify(tokenRepository, never()).deleteByUser_UserId(anyLong());

    verify(tokenRepository, never()).save(any(PasswordResetToken.class));

    verify(emailService, never()).sendPasswordResetEmail(anyString(), anyString(), anyString());
  }

  @Test
  void forgotPasswordCreatesExpiringToken() throws MessagingException {

    User user = createUser();

    when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));

    ArgumentCaptor<PasswordResetToken> tokenCaptor =
        ArgumentCaptor.forClass(PasswordResetToken.class);

    LocalDateTime before = LocalDateTime.now();

    passwordResetService.requestPasswordReset("user@example.com");

    verify(tokenRepository).save(tokenCaptor.capture());

    PasswordResetToken savedToken = tokenCaptor.getValue();

    assertNotNull(savedToken);

    assertEquals(user, savedToken.getUser());

    assertNotNull(savedToken.getTokenHash());

    assertFalse(savedToken.getTokenHash().isBlank());

    assertNotNull(savedToken.getExpiresAt());

    assertTrue(savedToken.getExpiresAt().isAfter(before.plusMinutes(29)));

    assertTrue(savedToken.getExpiresAt().isBefore(LocalDateTime.now().plusMinutes(31)));
  }

  @Test
  void resetPasswordRejectsInvalidToken() {

    when(tokenRepository.findByTokenHash(anyString())).thenReturn(Optional.empty());

    IllegalArgumentException exception =
        assertThrows(
            IllegalArgumentException.class,
            () -> passwordResetService.resetPassword("invalid-token", "NewPassword123"));

    assertEquals("Invalid or expired password reset token", exception.getMessage());

    verify(passwordEncoder, never()).encode(anyString());

    verify(userRepository, never()).save(any(User.class));

    verify(tokenRepository, never()).delete(any(PasswordResetToken.class));
  }

  @Test
  void resetPasswordRejectsExpiredToken() {

    User user = createUser();

    PasswordResetToken expiredToken =
        new PasswordResetToken("hashed-token", user, LocalDateTime.now().minusMinutes(1));

    when(tokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(expiredToken));

    IllegalArgumentException exception =
        assertThrows(
            IllegalArgumentException.class,
            () -> passwordResetService.resetPassword("expired-token", "NewPassword123"));

    assertEquals("Invalid or expired password reset token", exception.getMessage());

    verify(tokenRepository).delete(expiredToken);

    verify(passwordEncoder, never()).encode(anyString());

    verify(userRepository, never()).save(any(User.class));
  }

  @Test
  void resetPasswordUpdatesPassword() {

    User user = createUser();

    PasswordResetToken resetToken =
        new PasswordResetToken("hashed-token", user, LocalDateTime.now().plusMinutes(30));

    when(tokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(resetToken));

    when(passwordEncoder.encode("NewPassword123")).thenReturn("encoded-new-password");

    passwordResetService.resetPassword("valid-token", "NewPassword123");

    assertEquals("encoded-new-password", user.getPassword());

    verify(passwordEncoder).encode("NewPassword123");

    verify(userRepository).save(user);
  }

  @Test
  void resetPasswordDeletesTokenAfterSuccessfulReset() {

    User user = createUser();

    PasswordResetToken resetToken =
        new PasswordResetToken("hashed-token", user, LocalDateTime.now().plusMinutes(30));

    when(tokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(resetToken));

    when(passwordEncoder.encode("NewPassword123")).thenReturn("encoded-new-password");

    passwordResetService.resetPassword("valid-token", "NewPassword123");

    verify(tokenRepository).delete(resetToken);
  }

  private User createUser() {

    User user = new User("testuser", "old-password", "user@example.com", true);

    user.setUserId(1L);

    return user;
  }
}
