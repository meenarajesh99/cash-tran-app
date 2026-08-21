package com.perscholas.cashtran.controller;

import com.perscholas.cashtran.dto.*;
import com.perscholas.cashtran.model.User;
import com.perscholas.cashtran.repository.UserRepository;
import com.perscholas.cashtran.service.AuthService;
import com.perscholas.cashtran.service.PasswordResetService;
import com.perscholas.cashtran.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.mail.MessagingException;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthenticationController {

  private final UserRepository userRepository;

  private final AuthService authService;
  private final UserService userService;
  private final PasswordResetService passwordResetService;

  public AuthenticationController(
      AuthService authService,
      UserRepository userRepository,
      UserService userService,
      PasswordResetService passwordResetService) {

    this.authService = authService;
    this.userRepository = userRepository;
    this.userService = userService;
    this.passwordResetService = passwordResetService;
  }

    @PostMapping("/login")
    public ResponseEntity<LoginResponseDTO> login(
            @Valid @RequestBody LoginDTO loginDTO) {

    String token = authService.login(loginDTO);

    User user =
        userRepository
            .findByUsername(loginDTO.getUsername())
            .orElseThrow(() -> new RuntimeException("User not found"));

    UserResponseDTO userResponse = UserResponseDTO.from(user);

    return ResponseEntity.ok(new LoginResponseDTO(token, userResponse));
  }

  @Operation(summary = "Register a new user")
  @PostMapping("/register")
  @ResponseStatus(HttpStatus.CREATED)
  public UserResponseDTO register(@Valid @RequestBody RegisterUserDTO newUser)
      throws MessagingException {

    User user = new User();

    user.setUsername(newUser.getUsername());

    user.setPassword(newUser.getPassword());

    user.setEmail(newUser.getEmail());

    User savedUser = userService.createUser(user);

    return UserResponseDTO.from(savedUser);
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
}
