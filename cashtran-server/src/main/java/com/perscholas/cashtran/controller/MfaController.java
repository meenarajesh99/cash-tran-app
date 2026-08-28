package com.perscholas.cashtran.controller;

import com.perscholas.cashtran.dto.MfaCodeRequest;
import com.perscholas.cashtran.dto.MfaSetupResponse;
import com.perscholas.cashtran.model.User;
import com.perscholas.cashtran.repository.UserRepository;
import com.perscholas.cashtran.service.MfaService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/mfa")
public class MfaController {

  private final UserRepository userRepository;
  private final MfaService mfaService;

  public MfaController(UserRepository userRepository, MfaService mfaService) {

    this.userRepository = userRepository;
    this.mfaService = mfaService;
  }

  @PostMapping("/setup")
  public ResponseEntity<MfaSetupResponse> setupMfa(Authentication authentication) {

    User user =
        userRepository
            .findByUsername(authentication.getName())
            .orElseThrow(() -> new RuntimeException("User not found"));

    String secret = mfaService.generateSecret();

    user.setMfaSecret(secret);
    user.setMfaEnabled(false);

    userRepository.save(user);

    String username = URLEncoder.encode(user.getUsername(), StandardCharsets.UTF_8);

    String otpAuthUrl = mfaService.generateOtpAuthUrl(username, secret);

    return ResponseEntity.ok(new MfaSetupResponse(otpAuthUrl));
  }

  @PostMapping("/verify-setup")
  public ResponseEntity<?> verifySetup(
      Authentication authentication, @Valid @RequestBody MfaCodeRequest request) {

    User user =
        userRepository
            .findByUsername(authentication.getName())
            .orElseThrow(() -> new RuntimeException("User not found"));

    if (user.getMfaSecret() == null) {
      return ResponseEntity.badRequest().body("MFA setup has not been started");
    }

    boolean valid = mfaService.verifyCode(user.getMfaSecret(), request.getCode());

    if (!valid) {
      return ResponseEntity.badRequest().body("Invalid verification code");
    }

    user.setMfaEnabled(true);
    userRepository.save(user);

    return ResponseEntity.ok("MFA enabled successfully");
  }
}
