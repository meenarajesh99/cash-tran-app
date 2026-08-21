package com.perscholas.cashtran.controller;

import com.perscholas.cashtran.model.Account;
import com.perscholas.cashtran.model.User;
import com.perscholas.cashtran.repository.AccountRepository;
import com.perscholas.cashtran.repository.UserRepository;
import com.perscholas.cashtran.security.jwt.TokenProvider;
import com.perscholas.cashtran.service.TransferService;
import com.perscholas.cashtran.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AppController.class)
@AutoConfigureMockMvc(addFilters = false)
class AppControllerTest {

  @Autowired MockMvc mockMvc;
  @MockitoBean AccountRepository accountRepository;
  @MockitoBean UserRepository userRepository;
  @MockitoBean UserService userService;
  @MockitoBean TransferService transferService;
  @MockitoBean TokenProvider tokenProvider;

  @Test
  void rejectTransferUsesAuthenticatedUsersAccount() throws Exception {
    User payer = new User("payer", "encoded", "payer@example.com", true);
    Account payerAccount = new Account();
    payerAccount.setAccountId(2L);
    payer.setAccount(payerAccount);
    when(userRepository.findByUsernameWithAccount("payer")).thenReturn(Optional.of(payer));
    when(transferService.rejectTransfer(9L, 2L)).thenReturn(true);

    mockMvc
        .perform(put("/api/transfers/9/reject").principal(() -> "payer"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$").value(true));

    verify(transferService).rejectTransfer(9L, 2L);
  }

  @Test
  void getMyAccountReturnsAuthenticatedUser() throws Exception {

    User user = new User("payer", "encoded", "payer@example.com", true);

    Account account = new Account();
    account.setAccountId(2L);

    user.setAccount(account);

    when(userService.getUserByUsername("payer")).thenReturn(user);

    mockMvc
        .perform(get("/api/account").principal(() -> "payer"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(user.getUserId()))
        .andExpect(jsonPath("$.accountId").value(2L))
        .andExpect(jsonPath("$.username").value("payer"))
        .andExpect(jsonPath("$.email").value("payer@example.com"))
        .andExpect(jsonPath("$.activated").value(true));

    verify(userService).getUserByUsername("payer");
  }

  @Test
  void updateEmailUsesAuthenticatedUser() throws Exception {

    User updatedUser = new User("payer", "encoded", "newemail@example.com", true);

    Account account = new Account();
    account.setAccountId(2L);
    updatedUser.setAccount(account);

    when(userService.updateEmail("payer", "newemail@example.com")).thenReturn(updatedUser);

    mockMvc
        .perform(
            put("/api/account/email")
                .principal(() -> "payer")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                  {
                    "email": "newemail@example.com"
                  }
                  """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.username").value("payer"))
        .andExpect(jsonPath("$.email").value("newemail@example.com"))
        .andExpect(jsonPath("$.accountId").value(2L));

    verify(userService).updateEmail("payer", "newemail@example.com");
  }

  @Test
  void getMyAccountReturnsErrorWhenUserDoesNotExist() throws Exception {

    when(userService.getUserByUsername("unknown"))
        .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

    mockMvc
        .perform(get("/api/account").principal(() -> "unknown"))
        .andExpect(status().isNotFound());

    verify(userService).getUserByUsername("unknown");
  }

  @Test
  void updateEmailRejectsInvalidEmail() throws Exception {

    mockMvc
        .perform(
            put("/api/account/email")
                .principal(() -> "payer")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                  {
                    "email": "not-an-email"
                  }
                  """))
        .andExpect(status().isBadRequest());

    verify(userService, never())
        .updateEmail(
            org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyString());
  }

  @Test
  void updateEmailRejectsBlankEmail() throws Exception {

    mockMvc
        .perform(
            put("/api/account/email")
                .principal(() -> "payer")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                  {
                    "email": ""
                  }
                  """))
        .andExpect(status().isBadRequest());

    verify(userService, never()).updateEmail(anyString(), anyString());
  }

  @Test
  void updateEmailUsesAuthenticatedUserNotRequestUserId() throws Exception {

    User updatedUser = new User("payer", "encoded", "newemail@example.com", true);

    when(userService.updateEmail("payer", "newemail@example.com")).thenReturn(updatedUser);

    mockMvc
        .perform(
            put("/api/account/email")
                .principal(() -> "payer")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                  {
                    "userId": 999,
                    "email": "newemail@example.com"
                  }
                  """))
        .andExpect(status().isOk());

    verify(userService).updateEmail("payer", "newemail@example.com");
  }
}
