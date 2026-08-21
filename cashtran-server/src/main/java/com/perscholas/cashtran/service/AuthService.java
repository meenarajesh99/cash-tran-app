package com.perscholas.cashtran.service;

import com.perscholas.cashtran.dto.LoginDTO;
import com.perscholas.cashtran.security.jwt.TokenProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

  private static final Logger log = LoggerFactory.getLogger(AuthService.class);
  private final AuthenticationManager authenticationManager;
  private final TokenProvider tokenProvider;

  public AuthService(AuthenticationManager authenticationManager, TokenProvider tokenProvider) {

    this.authenticationManager = authenticationManager;
    this.tokenProvider = tokenProvider;
  }

  public String login(LoginDTO loginDTO) {
    log.debug("Authenticating user '{}'", loginDTO.getUsername());
    Authentication authentication =
        authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(
                loginDTO.getUsername(), loginDTO.getPassword()));

    return tokenProvider.createToken(authentication);
  }
}
