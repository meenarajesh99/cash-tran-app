package com.perscholas.cashtran.service;

import com.perscholas.cashtran.dto.LoginDTO;
import com.perscholas.cashtran.security.jwt.TokenProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {
  @Mock AuthenticationManager authenticationManager;
  @Mock TokenProvider tokenProvider;
  @Mock Authentication authentication;
  @InjectMocks AuthService authService;

  @Test
  void loginAuthenticatesCredentialsAndReturnsIssuedToken() {
    LoginDTO login = new LoginDTO();
    login.setUsername("alice");
    login.setPassword("password");
    when(authenticationManager.authenticate(any())).thenReturn(authentication);
    when(tokenProvider.createToken(authentication)).thenReturn("jwt");
    assertEquals("jwt", authService.login(login));
    verify(authenticationManager)
        .authenticate(
            argThat(
                token ->
                    token.getName().equals("alice") && token.getCredentials().equals("password")));
    verify(tokenProvider).createToken(authentication);
  }
}
