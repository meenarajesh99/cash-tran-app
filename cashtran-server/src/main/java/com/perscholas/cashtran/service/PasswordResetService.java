package com.perscholas.cashtran.service;

import com.perscholas.cashtran.model.PasswordResetToken;
import com.perscholas.cashtran.model.User;
import com.perscholas.cashtran.repository.PasswordResetTokenRepository;
import com.perscholas.cashtran.repository.UserRepository;
import jakarta.mail.MessagingException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;

@Service
public class PasswordResetService {

  private final UserRepository userRepository;
  private final PasswordResetTokenRepository tokenRepository;
  private final EmailService emailService;
  private final PasswordEncoder passwordEncoder;

  private final String frontendUrl;

  private final SecureRandom secureRandom = new SecureRandom();

  public PasswordResetService(
      UserRepository userRepository,
      PasswordResetTokenRepository tokenRepository,
      EmailService emailService,
      PasswordEncoder passwordEncoder,
      @Value("${cashtran.frontend.url}") String frontendUrl) {

    this.userRepository = userRepository;
    this.tokenRepository = tokenRepository;
    this.emailService = emailService;
    this.passwordEncoder = passwordEncoder;
    this.frontendUrl = frontendUrl;
  }

  @Transactional
  public void requestPasswordReset(String email) throws MessagingException {

    User user = userRepository.findByEmail(email).orElse(null);

    /*
     * Don't reveal whether the email exists.
     */
    if (user == null) {
      return;
    }

    tokenRepository.deleteByUser_UserId(user.getUserId());

    byte[] randomBytes = new byte[32];
    secureRandom.nextBytes(randomBytes);

    String rawToken = Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);

    String tokenHash = hashToken(rawToken);

    PasswordResetToken resetToken =
        new PasswordResetToken(tokenHash, user, LocalDateTime.now().plusMinutes(30));

    tokenRepository.save(resetToken);

    String resetLink = frontendUrl + "/reset-password?token=" + rawToken;

    emailService.sendPasswordResetEmail(user.getEmail(), user.getUsername(), resetLink);
  }

  @Transactional
  public void resetPassword(String rawToken, String newPassword) {

    String tokenHash = hashToken(rawToken);

    PasswordResetToken resetToken =
        tokenRepository
            .findByTokenHash(tokenHash)
            .orElseThrow(
                () -> new IllegalArgumentException("Invalid or expired password reset token"));

    if (resetToken.isExpired()) {
      tokenRepository.delete(resetToken);

      throw new IllegalArgumentException("Invalid or expired password reset token");
    }

    User user = resetToken.getUser();

    user.setPassword(passwordEncoder.encode(newPassword));

    userRepository.save(user);

    /*
     * Reset tokens are single-use.
     */
    tokenRepository.delete(resetToken);
  }

  private String hashToken(String token) {

    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");

      byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));

      return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);

    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("Unable to hash password reset token", e);
    }
  }
}
