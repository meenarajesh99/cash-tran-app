package com.perscholas.cashtran.controller;

import com.perscholas.cashtran.exception.EmailAlreadyRegisteredException;
import com.perscholas.cashtran.model.*;
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

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;

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
  void updateEmailReturnsConflictWhenEmailAlreadyRegistered() throws Exception {

    when(userService.updateEmail(anyString(), anyString()))
        .thenThrow(new EmailAlreadyRegisteredException());

    mockMvc
        .perform(
            put("/api/account/email")
                .principal(() -> "payer")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                                        {
                                            "email": "existing@example.com"
                                        }
                                        """))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.message").value("Email already registered"));

    verify(userService).updateEmail("payer", "existing@example.com");
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

  @Test
  void getAccountBalanceReturnsBalanceForAuthenticatedUser() throws Exception {
    User user = new User("alice", "pwd", "alice@example.com", true);
    user.setUserId(2L);

    when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));

    Account account = new Account();
    account.setAccountId(5L);
    account.setBalance(new BigDecimal("123.45"));

    when(accountRepository.findByUserId(2L)).thenReturn(Optional.of(account));

    mockMvc
        .perform(get("/api/balance").principal(() -> "alice"))
        .andExpect(status().isOk())
        .andExpect(content().string("123.45"));
  }

  @Test
  void listTransfersReturnsApprovedTransfersForUser() throws Exception {
    User alice = new User("alice", "pwd", "alice@example.com", true);
    alice.setUserId(2L);

    Account accountA = new Account();
    accountA.setAccountId(10L);
    accountA.setUser(alice);

    User bob = new User("bob", "pwd", "bob@example.com", true);
    Account accountB = new Account();
    accountB.setAccountId(11L);
    accountB.setUser(bob);

    when(userRepository.findByUsernameWithAccount("alice")).thenReturn(Optional.of(alice));
    when(accountRepository.findByUserId(2L)).thenReturn(Optional.of(accountA));

    Transfer t =
        new Transfer(
            accountA,
            accountB,
            new BigDecimal("50.00"),
            new TransferType("SEND"),
            new TransferStatus("APPROVED"));
    t.setTransferId(77L);

    when(transferService.getApprovedTransfers(10L)).thenReturn(List.of(t));

    mockMvc
        .perform(get("/api/transfers").principal(() -> "alice"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].transferId").value(77))
        .andExpect(jsonPath("$[0].amount").value(50.00))
        .andExpect(jsonPath("$[0].accountFromUsername").value("alice"));
  }

  @Test
  void usernameEndpointReturnsUsernameForAccountId() throws Exception {
    User bob = new User("bob", "pwd", "bob@example.com", true);
    Account account = new Account();
    account.setAccountId(9L);
    account.setUser(bob);

    when(accountRepository.findById(9L)).thenReturn(Optional.of(account));

    mockMvc
        .perform(get("/api/username/9"))
        .andExpect(status().isOk())
        .andExpect(content().string("bob"));
  }

  @Test
  void downloadStatementReturnsPdfBytesAndHeaders() throws Exception {
    byte[] pdf = new byte[] {1, 2, 3, 4};

    when(transferService.generateStatement("alice")).thenReturn(pdf);

    mockMvc
        .perform(get("/api/transfers/statement").principal(() -> "alice"))
        .andExpect(status().isOk())
        .andExpect(
            header()
                .string(
                    "Content-Disposition", "attachment; filename=CashTran_Transaction_History.pdf"))
        .andExpect(content().contentType("application/pdf"))
        .andExpect(content().bytes(pdf));
  }

  @Test
  void sendMoneyCreatesImmediateTransfer() throws Exception {
    User alice = new User("alice", "pwd", "alice@example.com", true);
    alice.setUserId(2L);
    Account senderAccount = new Account();
    senderAccount.setAccountId(20L);
    alice.setAccount(senderAccount);

    Account receiverAccount = new Account();
    receiverAccount.setAccountId(30L);
    User receiver = new User("rob", "pwd", "rob@example.com", true);
    receiver.setUserId(3L);
    receiverAccount.setUser(receiver);

    when(userRepository.findByUsernameWithAccount("alice")).thenReturn(Optional.of(alice));
    when(accountRepository.findByUserId(3L)).thenReturn(Optional.of(receiverAccount));

    Transfer created =
        new Transfer(
            senderAccount,
            receiverAccount,
            new BigDecimal("100.00"),
            new TransferType("SEND"),
            new TransferStatus("APPROVED"));
    created.setTransferId(555L);

    when(transferService.createTransfer(20L, 30L, new BigDecimal("100.00"))).thenReturn(created);

    mockMvc
        .perform(
            post("/api/transfers/send")
                .principal(() -> "alice")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\n  \"userId\": 3,\n  \"amount\": 100.00\n}\n"))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.transferId").value(555))
        .andExpect(jsonPath("$.amount").value(100.00));
  }

  @Test
  void approveTransferUsesAuthenticatedUsersAccount() throws Exception {
    User approver = new User("approver", "encoded", "approver@example.com", true);
    Account approverAccount = new Account();
    approverAccount.setAccountId(2L);
    approver.setAccount(approverAccount);

    when(userRepository.findByUsernameWithAccount("approver")).thenReturn(Optional.of(approver));
    when(transferService.approveTransfer(9L, 2L)).thenReturn(true);

    mockMvc
        .perform(put("/api/transfers/9/approve").principal(() -> "approver"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$").value(true));

    verify(transferService).approveTransfer(9L, 2L);
  }

  @Test
  void pendingReceivedReturnsPendingRequests() throws Exception {
    User user = new User("recv", "pwd", "recv@example.com", true);
    Account account = new Account();
    account.setAccountId(42L);
    account.setUser(user);

    when(accountRepository.findByUsername("recv")).thenReturn(Optional.of(account));

    Transfer t =
        new Transfer(
            null,
            account,
            new BigDecimal("25.00"),
            new TransferType("REQUEST"),
            new TransferStatus("PENDING"));
    t.setTransferId(101L);

    when(transferService.getPendingRequestsReceived(42L)).thenReturn(List.of(t));

    mockMvc
        .perform(get("/api/transfers/pending/received").principal(() -> "recv"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].transferId").value(101))
        .andExpect(jsonPath("$[0].amount").value(25.00))
        .andExpect(jsonPath("$[0].accountToUsername").value("recv"));
  }

  @Test
  void pendingReceivedIncludesRejectedStatus() throws Exception {
    User user = new User("recv2", "pwd", "recv2@example.com", true);
    Account account = new Account();
    account.setAccountId(43L);
    account.setUser(user);

    when(accountRepository.findByUsername("recv2")).thenReturn(Optional.of(account));

    Transfer t =
        new Transfer(
            null,
            account,
            new BigDecimal("30.00"),
            new TransferType("REQUEST"),
            new TransferStatus("Rejected"));
    t.setTransferId(111L);

    when(transferService.getPendingRequestsReceived(43L)).thenReturn(List.of(t));

    mockMvc
        .perform(get("/api/transfers/pending/received").principal(() -> "recv2"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].transferId").value(111))
        .andExpect(jsonPath("$[0].transferStatusDesc").value("Rejected"));
  }

  @Test
  void pendingSentReturnsPendingRequestsSent() throws Exception {
    User user = new User("sender", "pwd", "sender@example.com", true);
    Account account = new Account();
    account.setAccountId(55L);
    account.setUser(user);

    when(accountRepository.findByUsername("sender")).thenReturn(Optional.of(account));

    User target = new User("target", "pwd", "target@example.com", true);
    Account targetAccount = new Account();
    targetAccount.setAccountId(99L);
    targetAccount.setUser(target);

    Transfer t =
        new Transfer(
            account,
            targetAccount,
            new BigDecimal("12.34"),
            new TransferType("REQUEST"),
            new TransferStatus("PENDING"));
    t.setTransferId(202L);

    when(transferService.getPendingRequestsSent(55L)).thenReturn(List.of(t));

    mockMvc
        .perform(get("/api/transfers/pending/sent").principal(() -> "sender"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].transferId").value(202))
        .andExpect(jsonPath("$[0].amount").value(12.34))
        .andExpect(jsonPath("$[0].accountFromUsername").value("sender"));
  }

  @Test
  void pendingSentIncludesRejectedStatus() throws Exception {
    User user = new User("sender2", "pwd", "sender2@example.com", true);
    Account account = new Account();
    account.setAccountId(56L);
    account.setUser(user);

    when(accountRepository.findByUsername("sender2")).thenReturn(Optional.of(account));

    User target = new User("target3", "pwd", "target3@example.com", true);
    Account targetAccount = new Account();
    targetAccount.setAccountId(100L);
    targetAccount.setUser(target);

    Transfer t =
        new Transfer(
            account,
            targetAccount,
            new BigDecimal("5.00"),
            new TransferType("REQUEST"),
            new TransferStatus("Rejected"));
    t.setTransferId(303L);

    when(transferService.getPendingRequestsSent(56L)).thenReturn(List.of(t));

    mockMvc
        .perform(get("/api/transfers/pending/sent").principal(() -> "sender2"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].transferId").value(303))
        .andExpect(jsonPath("$[0].transferStatusDesc").value("Rejected"));
  }

  @Test
  void transferDetailsReturnsTransferDto() throws Exception {
    User from = new User("from", "pwd", "from@example.com", true);
    Account fromAccount = new Account();
    fromAccount.setAccountId(1L);
    from.setAccount(fromAccount);

    User to = new User("to", "pwd", "to@example.com", true);
    Account toAccount = new Account();
    toAccount.setAccountId(2L);
    to.setAccount(toAccount);

    Transfer t =
        new Transfer(
            fromAccount,
            toAccount,
            new BigDecimal("7.00"),
            new TransferType("SEND"),
            new TransferStatus("APPROVED"));
    t.setTransferId(303L);

    when(transferService.getTransferById(303L)).thenReturn(t);

    mockMvc.perform(get("/api/transfers/303")).andExpect(status().isOk()).andExpect(jsonPath("$.transferId").value(303)).andExpect(jsonPath("$.amount").value(7.00)).andExpect(jsonPath("$.accountFrom").value(1)).andExpect(jsonPath("$.accountTo").value(2));
  }

  @Test
  void createTransferCreatesTransferRequest() throws Exception {
    User alice = new User("alice", "pwd", "alice@example.com", true);
    alice.setUserId(2L);
    Account senderAccount = new Account();
    senderAccount.setAccountId(20L);
    alice.setAccount(senderAccount);

    Account receiverAccount = new Account();
    receiverAccount.setAccountId(30L);
    User receiver = new User("rob", "pwd", "rob@example.com", true);
    receiver.setUserId(3L);
    receiverAccount.setUser(receiver);

    when(userRepository.findByUsernameWithAccount("alice")).thenReturn(Optional.of(alice));
    when(accountRepository.findByUserId(3L)).thenReturn(Optional.of(receiverAccount));

    Transfer created =
        new Transfer(
            senderAccount,
            receiverAccount,
            new BigDecimal("45.00"),
            new TransferType("SEND"),
            new TransferStatus("PENDING"));
    created.setTransferId(777L);

    when(transferService.createTransfer(20L, 30L, new BigDecimal("45.00"))).thenReturn(created);

    mockMvc
        .perform(
            post("/api/transfers")
                .principal(() -> "alice")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\n  \"userId\": 3,\n  \"amount\": 45.00\n}\n"))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.transferId").value(777))
        .andExpect(jsonPath("$.amount").value(45.00));
  }

  @Test
  void requestTransferCreatesRequest() throws Exception {
    User requester = new User("req", "pwd", "req@example.com", true);
    requester.setUserId(6L);
    Account requesterAccount = new Account();
    requesterAccount.setAccountId(60L);
    requester.setAccount(requesterAccount);

    Account requestedAccount = new Account();
    requestedAccount.setAccountId(70L);
    User requestedUser = new User("target2", "pwd", "t2@example.com", true);
    requestedUser.setUserId(8L);
    requestedAccount.setUser(requestedUser);

    when(userRepository.findByUsernameWithAccount("req")).thenReturn(Optional.of(requester));
    when(accountRepository.findByUserId(8L)).thenReturn(Optional.of(requestedAccount));

    Transfer created =
        new Transfer(
            requesterAccount,
            requestedAccount,
            new BigDecimal("3.50"),
            new TransferType("REQUEST"),
            new TransferStatus("PENDING"));
    created.setTransferId(888L);

    when(transferService.createRequest(60L, 70L, new BigDecimal("3.50"))).thenReturn(created);

    mockMvc
        .perform(
            post("/api/requests")
                .principal(() -> "req")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\n  \"userId\": 8,\n  \"amount\": 3.50\n}\n"))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.transferId").value(888))
        .andExpect(jsonPath("$.amount").value(3.50));
  }
}
