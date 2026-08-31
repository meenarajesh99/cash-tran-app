package com.perscholas.cashtran.service;

import com.perscholas.cashtran.dto.LoginDTO;
import com.perscholas.cashtran.dto.LoginResultDTO;
import com.perscholas.cashtran.model.User;
import com.perscholas.cashtran.repository.UserRepository;
import com.perscholas.cashtran.security.jwt.TokenProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

  @Mock private AuthenticationManager authenticationManager;
  @Mock private TokenProvider tokenProvider;
  @Mock private UserRepository userRepository;
  @Mock private Authentication authentication;
  @InjectMocks private AuthService authService;

  @Test
  void loginAuthenticatesCredentialsAndReturnsIssuedToken() {

    // Arrange
    LoginDTO login = new LoginDTO();
    login.setUsername("alice");
    login.setPassword("password");

    User user = new User();
    user.setUsername("alice");
    user.setMfaEnabled(false);

    when(authenticationManager.authenticate(any())).thenReturn(authentication);
    when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));
    when(tokenProvider.createToken(authentication)).thenReturn("jwt");

    // Act
    LoginResultDTO result = authService.login(login);

    // Assert
    assertEquals("jwt", result.getToken());
    assertFalse(result.isMfaRequired());
    assertEquals("alice", result.getUsername());

    verify(authenticationManager)
        .authenticate(
            argThat(
                token ->
                    token.getName().equals("alice") && token.getCredentials().equals("password")));

    verify(userRepository).findByUsername("alice");
    verify(tokenProvider).createToken(authentication);
    verify(tokenProvider, never()).createMfaChallengeToken(any());
  }

  @Test
  void loginReturnsMfaChallengeWhenMfaEnabled() {

    // Arrange
    LoginDTO login = new LoginDTO();
    login.setUsername("alice");
    login.setPassword("password");

    User user = new User();
    user.setUsername("alice");
    user.setMfaEnabled(true);

    when(authenticationManager.authenticate(any())).thenReturn(authentication);
    when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));
    when(tokenProvider.createMfaChallengeToken("alice")).thenReturn("mfa-challenge-token");

    // Act
    LoginResultDTO result = authService.login(login);

    // Assert
    assertTrue(result.isMfaRequired());
    assertEquals("alice", result.getUsername());
    assertEquals("mfa-challenge-token", result.getToken());

    verify(tokenProvider).createMfaChallengeToken("alice");
    verify(tokenProvider, never()).createToken(any());
  }
}
