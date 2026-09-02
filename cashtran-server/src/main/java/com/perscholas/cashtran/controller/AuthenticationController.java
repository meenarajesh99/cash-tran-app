package com.perscholas.cashtran.controller;

import com.perscholas.cashtran.dto.*;
import com.perscholas.cashtran.model.User;
import com.perscholas.cashtran.repository.UserRepository;
import com.perscholas.cashtran.security.jwt.TokenProvider;
import com.perscholas.cashtran.service.AuthService;
import com.perscholas.cashtran.service.MfaService;
import com.perscholas.cashtran.service.PasswordResetService;
import com.perscholas.cashtran.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.mail.MessagingException;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.web.bind.annotation.*;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/auth")
public class AuthenticationController {

  private final UserRepository userRepository;

  private final AuthService authService;
  private final UserService userService;
  private final PasswordResetService passwordResetService;
  private final MfaService mfaService;
  private final TokenProvider tokenProvider;

  public AuthenticationController(
      AuthService authService,
      UserRepository userRepository,
      UserService userService,
      PasswordResetService passwordResetService,
      MfaService mfaService,
      TokenProvider tokenProvider) {

    this.authService = authService;
    this.userRepository = userRepository;
    this.userService = userService;
    this.passwordResetService = passwordResetService;
    this.mfaService = mfaService;
    this.tokenProvider = tokenProvider;
  }

  @PostMapping("/login")
  public ResponseEntity<LoginResponseDTO> login(@Valid @RequestBody LoginDTO loginDTO) {

    LoginResultDTO result = authService.login(loginDTO);
    User user =
        userRepository
            .findByUsername(loginDTO.getUsername())
            .orElseThrow(() -> new RuntimeException("User not found"));

    /*
     * MFA required.
     *
     * Do NOT return the user's normal information
     * or JWT yet.
     */
    if (result.isMfaRequired()) {
      return ResponseEntity.ok(new LoginResponseDTO(null, null, true, result.getToken()));
    }

    /*
     * Normal login.
     */
    UserResponseDTO userResponse = UserResponseDTO.from(user);
    return ResponseEntity.ok(new LoginResponseDTO(result.getToken(), userResponse, false, null));
  }

  @Operation(summary = "Register a new user")
  @PostMapping("/register")
  @ResponseStatus(HttpStatus.CREATED)
  public RegistrationResponseDTO register(@Valid @RequestBody RegisterUserDTO newUser) {

    User user = new User();
    user.setUsername(newUser.getUsername());
    user.setPassword(newUser.getPassword());
    user.setEmail(newUser.getEmail());
    User savedUser = userService.createUser(user);
    String username = URLEncoder.encode(savedUser.getUsername(), StandardCharsets.UTF_8);

    String mfaSetupUrl = mfaService.generateOtpAuthUrl(username, savedUser.getMfaSecret());
    String enrollmentToken = tokenProvider.createMfaEnrollmentToken(savedUser.getUsername());

    return new RegistrationResponseDTO(
        savedUser.getUserId(),
        savedUser.getAccount().getAccountId(),
        savedUser.getUsername(),
        savedUser.getEmail(),
        savedUser.isActivated(),
        true,
        mfaSetupUrl,
        enrollmentToken);
  }

  @PostMapping("/forgot-password")
  @ResponseStatus(HttpStatus.OK)
  public void forgotPassword(@Valid @RequestBody ForgotPasswordRequestDTO request)
      throws MessagingException {

    passwordResetService.requestPasswordReset(request.getEmail().trim());
  }

  @PostMapping("/reset-password")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void resetPassword(@Valid @RequestBody ResetPasswordRequestDTO request) {

    passwordResetService.resetPassword(request.getToken(), request.getPassword());
  }

  @PostMapping("/mfa/enroll")
  public ResponseEntity<?> completeMfaEnrollment(@Valid @RequestBody MfaEnrollmentRequest request) {

    /*
     * 1. Validate the temporary enrollment token.
     */
    if (!tokenProvider.validateMfaEnrollment(request.getEnrollmentToken())) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
          .body("Invalid or expired MFA enrollment token");
    }

    /*
     * 2. Extract username from the enrollment token.
     *
     * Do NOT trust a username supplied by the frontend.
     */
    String username = tokenProvider.getUsernameFromMfaEnrollment(request.getEnrollmentToken());

    /*
     * 3. Find the user.
     */
    User user =
        userRepository
            .findByUsername(username)
            .orElseThrow(() -> new RuntimeException("User not found"));

    /*
     * 4. Prevent enrollment from being completed twice.
     */
    if (user.isMfaEnabled()) {
      return ResponseEntity.badRequest().body("MFA is already enabled");
    }

    /*
     * 5. Verify the first Google Authenticator code.
     */
    boolean valid = mfaService.verifyCode(user.getMfaSecret(), request.getCode());
    if (!valid) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid MFA code");
    }

    /*
     * 6. MFA setup succeeded.
     */
    user.setMfaEnabled(true);
    userRepository.save(user);
    return ResponseEntity.ok(java.util.Map.of("message", "MFA enabled successfully"));
  }

  @PostMapping("/mfa/login")
  public ResponseEntity<?> verifyMfaLogin(@Valid @RequestBody MfaLoginRequest request) {

    /*
     * First make sure this is a valid,
     * unexpired MFA challenge.
     */
    if (!tokenProvider.validateMfaChallenge(request.getMfaToken())) {

      return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
          .body("Invalid or expired MFA challenge");
    }

    /*
     * Extract the username from the
     * MFA challenge rather than trusting
     * a username supplied by the client.
     */
    String username = tokenProvider.getUsernameFromMfaChallenge(request.getMfaToken());

    User user =
        userRepository
            .findByUsername(username)
            .orElseThrow(() -> new RuntimeException("User not found"));

    if (!user.isMfaEnabled()) {

      return ResponseEntity.badRequest().body("MFA is not enabled");
    }

    /*
     * Verify the six-digit TOTP code.
     */
    boolean valid = mfaService.verifyCode(user.getMfaSecret(), request.getCode());

    if (!valid) {

      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid MFA code");
    }

    /*
     * MFA succeeded.
     *
     * Reconstruct an authenticated principal
     * using the user's authorities.
     */
    var authorities =
        user.getAuthorities().stream()
            .map(authority -> new SimpleGrantedAuthority(authority.getAuthorityName()))
            .collect(Collectors.toList());

    Authentication authentication =
        new UsernamePasswordAuthenticationToken(user.getUsername(), null, authorities);

    /*
     * NOW issue the real CashTran JWT.
     */
    String token = tokenProvider.createToken(authentication);

    UserResponseDTO userResponse = UserResponseDTO.from(user);

    return ResponseEntity.ok(new LoginResponseDTO(token, userResponse, false, null));
  }
}
