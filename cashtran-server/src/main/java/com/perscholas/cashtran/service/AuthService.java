package com.perscholas.cashtran.service;

import com.perscholas.cashtran.dto.LoginDTO;
import com.perscholas.cashtran.dto.LoginResultDTO;
import com.perscholas.cashtran.model.User;
import com.perscholas.cashtran.repository.UserRepository;
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
  private final UserRepository userRepository;

  public AuthService(
      AuthenticationManager authenticationManager,
      TokenProvider tokenProvider,
      UserRepository userRepository) {

    this.authenticationManager = authenticationManager;
    this.tokenProvider = tokenProvider;
    this.userRepository = userRepository;
  }

  public LoginResultDTO login(LoginDTO loginDTO) {

    log.debug("Authenticating user '{}'", loginDTO.getUsername());

    /*
     * 1. Authenticate username + password.
     */
    Authentication authentication =
        authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(
                loginDTO.getUsername(), loginDTO.getPassword()));

    /*
     * 2. Load CashTran user.
     */
    User user =
        userRepository
            .findByUsername(loginDTO.getUsername())
            .orElseThrow(() -> new RuntimeException("User not found"));

    /*
     * 3. MFA enabled?
     *
     * Do NOT issue the normal JWT.
     */
    if (user.isMfaEnabled()) {

      log.info("MFA required for user '{}'", user.getUsername());

      String mfaToken = tokenProvider.createMfaChallengeToken(user.getUsername());

      return new LoginResultDTO(true, user.getUsername(), mfaToken);
    }

    /*
     * 4. MFA disabled.
     *
     * Issue normal JWT.
     */
    String token = tokenProvider.createToken(authentication);

    return new LoginResultDTO(false, user.getUsername(), token);
  }
}
