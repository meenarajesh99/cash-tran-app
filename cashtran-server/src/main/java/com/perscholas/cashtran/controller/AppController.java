package com.perscholas.cashtran.controller;

import com.perscholas.cashtran.dto.TransferDTO;
import com.perscholas.cashtran.dto.TransferResponseDTO;
import com.perscholas.cashtran.dto.UserResponseDTO;
import com.perscholas.cashtran.model.Account;
import com.perscholas.cashtran.model.Transfer;
import com.perscholas.cashtran.model.User;
import com.perscholas.cashtran.repository.AccountRepository;
import com.perscholas.cashtran.repository.UserRepository;
import com.perscholas.cashtran.service.TransferService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api")
@PreAuthorize("isAuthenticated()")
public class AppController {

  private final AccountRepository accountRepository;
  private final UserRepository userRepository;
  private final TransferService transferService;

  public AppController(
      AccountRepository accountRepository,
      UserRepository userRepository,
      TransferService transferService) {

    this.accountRepository = accountRepository;
    this.userRepository = userRepository;
    this.transferService = transferService;
  }

  /*
   * Get logged-in user's balance
   */
  @GetMapping("/balance")
  public BigDecimal getAccountBalance(Principal principal) {

    User user =
        userRepository
            .findByUsername(principal.getName())
            .orElseThrow(() -> new RuntimeException("User not found"));

    Account account =
        accountRepository
            .findByUserId(user.getUserId())
            .orElseThrow(() -> new RuntimeException("Account not found"));

    return account.getBalance();
  }

  /*
   * Get account by user id
   */
  @GetMapping("/account/{id}")
  public Account getAccountByUserId(@PathVariable Long id) {

    return accountRepository
        .findByUserId(id)
        .orElseThrow(() -> new RuntimeException("Account not found"));
  }

  /*
   * Get all users
   */
  @GetMapping("/users")
  public List<UserResponseDTO> getAllUsers() {
    return userRepository.findAll().stream().map(UserResponseDTO::from).toList();
  }

  /*
   * Get approved transfers
   */
  @GetMapping("/transfers")
  public List<TransferResponseDTO> listTransfers(Principal principal) {
    User user = userRepository.findByUsernameWithAccount(principal.getName()).orElseThrow();
    Account account = accountRepository.findByUserId(user.getUserId()).orElseThrow();
    return transferService.getApprovedTransfers(account.getAccountId()).stream()
        .map(TransferResponseDTO::from)
        .toList();
  }

  /*
   * Get pending transfers
   */
  @GetMapping("/transfers/pending/received")
  public List<TransferResponseDTO> pendingReceived(Principal principal) {

    Account account =
        accountRepository
            .findByUsername(principal.getName())
            .orElseThrow(() -> new RuntimeException("Account not found"));

    return transferService.getPendingRequestsReceived(account.getAccountId()).stream()
        .map(TransferResponseDTO::from)
        .toList();
  }

  @GetMapping("/transfers/pending/sent")
  public List<TransferResponseDTO> pendingSent(Principal principal) {

    Account account =
        accountRepository
            .findByUsername(principal.getName())
            .orElseThrow(() -> new RuntimeException("Account not found"));

    return transferService.getPendingRequestsSent(account.getAccountId()).stream()
        .map(TransferResponseDTO::from)
        .toList();
  }

  /*
   * Get transfer details
   */
  @GetMapping("/transfers/{transferId}")
  public TransferResponseDTO transferDetails(@PathVariable Long transferId) {

    return TransferResponseDTO.from(transferService.getTransferById(transferId));
  }

  /*
   * Send money immediately
   */
  @PostMapping("/transfers/send")
  @ResponseStatus(HttpStatus.CREATED)
  public TransferResponseDTO sendMoney(
      Principal principal, @Valid @RequestBody TransferDTO transferDTO) {
    User sender = userRepository.findByUsernameWithAccount(principal.getName()).orElseThrow();
    Transfer transfer =
        transferService.createTransfer(
            sender.getAccount().getAccountId(),
            accountRepository.findByUserId(transferDTO.getUserId()).orElseThrow().getAccountId(),
            transferDTO.getAmount());

    return TransferResponseDTO.from(transfer);
  }

  /*
   * Create transfer request
   */
  @PostMapping("/transfers")
  @ResponseStatus(HttpStatus.CREATED)
  public TransferResponseDTO sendTransfer(
      Principal principal, @Valid @RequestBody TransferDTO transferDTO) {

    User sender = userRepository.findByUsernameWithAccount(principal.getName()).orElseThrow();

    Account receiverAccount =
        accountRepository
            .findByUserId(transferDTO.getUserId())
            .orElseThrow(() -> new RuntimeException("Receiver account not found"));

    Transfer transfer =
        transferService.createTransfer(
            sender.getAccount().getAccountId(),
            receiverAccount.getAccountId(),
            transferDTO.getAmount());

    return TransferResponseDTO.from(transfer);
  }

  /*
   * Request money
   */
  @PostMapping("/requests")
  @ResponseStatus(HttpStatus.CREATED)
  public TransferResponseDTO requestTransfer(
      Principal principal, @Valid @RequestBody TransferDTO transferDTO) {

    User requester = userRepository.findByUsernameWithAccount(principal.getName()).orElseThrow();

    Account requestedUserAccount =
        accountRepository
            .findByUserId(transferDTO.getUserId())
            .orElseThrow(() -> new RuntimeException("Requested user account not found"));

    Transfer transfer =
        transferService.createRequest(
            requester.getAccount().getAccountId(),
            requestedUserAccount.getAccountId(),
            transferDTO.getAmount());

    return TransferResponseDTO.from(transfer);
  }

  /*
   * Accept transfer request
   */
  @PutMapping("/transfers/{transferId}/approve")
  public boolean acceptTransfer(Principal principal, @PathVariable Long transferId) {
      User user =
              userRepository.findByUsernameWithAccount(principal.getName())
                      .orElseThrow();

      return transferService.approveTransfer(
              transferId,
              user.getAccount().getAccountId()
      );
  }

  /*
   * Reject transfer request
   */
  @PutMapping("/transfers/{transferId}/reject")
  public boolean rejectTransfer(@PathVariable Long transferId) {
    return transferService.rejectTransfer(transferId);
  }

  /*
   * Find username from account id
   */
  @GetMapping("/username/{accountId}")
  public String username(@PathVariable Long accountId) {
    Account account = accountRepository.findById(accountId).orElseThrow();
    return account.getUser().getUsername();
  }
}
