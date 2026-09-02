package com.perscholas.cashtran.controller;

import com.perscholas.cashtran.dto.LoginDTO;
import com.perscholas.cashtran.dto.LoginResultDTO;
import com.perscholas.cashtran.model.User;
import com.perscholas.cashtran.repository.UserRepository;
import com.perscholas.cashtran.security.jwt.TokenProvider;
import com.perscholas.cashtran.service.AuthService;
import com.perscholas.cashtran.service.MfaService;
import com.perscholas.cashtran.service.PasswordResetService;
import com.perscholas.cashtran.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthenticationController.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthenticationControllerTest {

  @Autowired MockMvc mockMvc;

  @MockitoBean AuthService authService;

  @MockitoBean UserRepository userRepository;

  @MockitoBean UserService userService;

  @MockitoBean PasswordResetService passwordResetService;

  @MockitoBean MfaService mfaService;

  @MockitoBean TokenProvider tokenProvider;

  @Test
  void loginReturnsTokenAndPublicUserDetails() throws Exception {

    User user = new User("alice", "encoded", "alice@example.com", true);

    user.setUserId(7L);

    /*
     * AuthService now returns LoginResultDTO
     * instead of just the JWT String.
     *
     * false = MFA is not required
     * alice = username
     * jwt-token = real JWT
     */
    LoginResultDTO loginResult = new LoginResultDTO(false, "alice", "jwt-token");

    when(authService.login(any(LoginDTO.class))).thenReturn(loginResult);

    when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));

    mockMvc
        .perform(
            post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                                        {
                                            "username": "alice",
                                            "password": "password"
                                        }
                                        """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.token").value("jwt-token"))
        .andExpect(jsonPath("$.user.id").value(7))
        .andExpect(jsonPath("$.user.username").value("alice"))
        .andExpect(jsonPath("$.user.email").value("alice@example.com"))
        .andExpect(jsonPath("$.user.activated").value(true))
        .andExpect(jsonPath("$.user.password").doesNotExist());
  }

  @Test
  void loginReturnsMfaChallengeWhenMfaEnabled() throws Exception {

    User user = new User("alice", "encoded", "alice@example.com", true);
    user.setUserId(7L);
    user.setMfaEnabled(true);

    /*
     * The token here is NOT the normal JWT.
     *
     * It is the temporary MFA challenge token.
     */
    LoginResultDTO loginResult = new LoginResultDTO(true, "alice", "mfa-challenge-token");
    when(authService.login(any(LoginDTO.class))).thenReturn(loginResult);
    when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));

    mockMvc
        .perform(
            post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                                        {
                                            "username": "alice",
                                            "password": "password"
                                        }
                                        """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.mfaRequired").value(true))
        .andExpect(jsonPath("$.token").doesNotExist())
        .andExpect(jsonPath("$.mfaToken").value("mfa-challenge-token"))
        .andExpect(jsonPath("$.user").doesNotExist());
  }

  @Test
  void registerCreatesUserAndRejectsInvalidEmail() throws Exception {

    User saved = new User("alice", "encoded", "alice@example.com", true);

    saved.setUserId(7L);
    
    // Mock the Account on the saved User
    com.perscholas.cashtran.model.Account mockAccount = 
        new com.perscholas.cashtran.model.Account();
    mockAccount.setAccountId(1L);
    saved.setAccount(mockAccount);

    when(userService.createUser(any())).thenReturn(saved);

    mockMvc
        .perform(
            post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                                        {
                                            "username": "alice",
                                            "password": "password",
                                            "email": "alice@example.com"
                                        }
                                        """))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.username").value("alice"))
        .andExpect(jsonPath("$.email").value("alice@example.com"))
        .andExpect(jsonPath("$.activated").value(true));

    mockMvc
        .perform(
            post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                                        {
                                            "username": "alice",
                                            "password": "password",
                                            "email": "not-an-email"
                                        }
                                        """))
        .andExpect(status().isBadRequest());
  }
}
