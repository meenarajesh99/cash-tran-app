package com.perscholas.cashtran.controller;

import com.perscholas.cashtran.dto.TransferDTO;
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
  public List<User> getAllUsers() {

    return userRepository.findAll();
  }

  /*
   * Get approved transfers
   */
  @GetMapping("/transfers")
  public List<Transfer> listTransfers(Principal principal) {

    User user = userRepository.findByUsername(principal.getName()).orElseThrow();

    Account account = accountRepository.findByUserId(user.getUserId()).orElseThrow();

    return transferService.getApprovedTransfers(account.getAccountId());
  }

  /*
   * Get pending transfers
   */
  @GetMapping("/transfers/pending")
  public List<Transfer> listPendingTransfers(Principal principal) {

    User user = userRepository.findByUsername(principal.getName()).orElseThrow();

    Account account = accountRepository.findByUserId(user.getUserId()).orElseThrow();

    return transferService.getPendingTransfers(account.getAccountId());
  }

  /*
   * Get transfer details
   */
  @GetMapping("/transfers/{transferId}")
  public Transfer transferDetails(@PathVariable Long transferId) {

    return transferService.getTransferById(transferId);
  }

  /*
   * Send money immediately
   */
  @PostMapping("/transfers/send")
  @ResponseStatus(HttpStatus.CREATED)
  public Transfer sendMoney(Principal principal, @Valid @RequestBody TransferDTO transferDTO) {

    User sender = userRepository.findByUsername(principal.getName()).orElseThrow();

    return transferService.createTransfer(
        sender.getUserId(), transferDTO.getUserId(), transferDTO.getAmount());
  }

  /*
   * Create transfer request
   */
  @PostMapping("/transfers")
  @ResponseStatus(HttpStatus.CREATED)
  public Transfer startTransfer(Principal principal, @Valid @RequestBody TransferDTO transferDTO) {

    User sender = userRepository.findByUsername(principal.getName()).orElseThrow();

    return transferService.createTransfer(
        sender.getUserId(), transferDTO.getUserId(), transferDTO.getAmount());
  }

  /*
   * Request money
   */
  @PostMapping("/requests")
  @ResponseStatus(HttpStatus.CREATED)
  public Transfer requestTransfer(
      Principal principal, @Valid @RequestBody TransferDTO transferDTO) {

    User requester = userRepository.findByUsername(principal.getName()).orElseThrow();

    return transferService.createRequest(
        transferDTO.getUserId(), requester.getUserId(), transferDTO.getAmount());
  }

  /*
   * Accept transfer request
   */
  @PutMapping("/transfer/{transferId}/accept")
  public boolean acceptTransfer(Principal principal, @PathVariable Long transferId) {

    return transferService.acceptTransfer(transferId);
  }

  /*
   * Reject transfer request
   */
  @PutMapping("/transfer/{transferId}/reject")
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
