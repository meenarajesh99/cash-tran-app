package com.perscholas.cashtran.service;

import com.perscholas.cashtran.model.Account;
import com.perscholas.cashtran.model.Transfer;
import com.perscholas.cashtran.model.TransferStatus;
import com.perscholas.cashtran.model.TransferType;
import com.perscholas.cashtran.repository.AccountRepository;
import com.perscholas.cashtran.repository.TransferRepository;
import com.perscholas.cashtran.repository.TransferStatusRepository;
import com.perscholas.cashtran.repository.TransferTypeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@Transactional
public class TransferService {

  private final TransferRepository transferRepository;
  private final AccountRepository accountRepository;
  private final TransferStatusRepository transferStatusRepository;
  private final TransferTypeRepository transferTypeRepository;

  public TransferService(
      TransferRepository transferRepository,
      AccountRepository accountRepository,
      TransferStatusRepository transferStatusRepository,
      TransferTypeRepository transferTypeRepository) {

    this.transferRepository = transferRepository;
    this.accountRepository = accountRepository;
    this.transferStatusRepository = transferStatusRepository;
    this.transferTypeRepository = transferTypeRepository;
  }

  public List<Transfer> getApprovedTransfers(Long accountId) {
    return transferRepository.findTransfersByStatusAndAccount("Approved", accountId);
  }

  /**
   * Pending requests where this account is the payer. Example: Bob owes Alice money request. Bob
   * sees this and can Approve/Reject.
   */
  public List<Transfer> getPendingRequestsReceived(Long accountId) {

    return transferRepository.findPendingReceivedTransfers("Pending", accountId);
  }

  /**
   * Pending requests created by this account. Example: Alice requested money from Bob. Alice sees
   * this as waiting.
   */
  public List<Transfer> getPendingRequestsSent(Long accountId) {

    return transferRepository.findPendingSentTransfers("Pending", accountId);
  }

  /*
   * Immediate Transfer - send money to someone immediately
   */
  public Transfer createTransfer(Long fromAccountId, Long toAccountId, BigDecimal amount) {

    if (amount.compareTo(BigDecimal.ZERO) <= 0) {
      throw new RuntimeException("Transfer amount must be greater than zero.");
    }

    Account fromAccount =
        accountRepository
            .findById(fromAccountId)
            .orElseThrow(() -> new RuntimeException("Source account not found"));

    Account toAccount =
        accountRepository
            .findById(toAccountId)
            .orElseThrow(() -> new RuntimeException("Destination account not found"));

    if (fromAccount.getAccountId().equals(toAccount.getAccountId())) {
      throw new RuntimeException("Cannot transfer money to the same account.");
    }

    if (fromAccount.getBalance().compareTo(amount) < 0) {
      throw new RuntimeException("Insufficient funds.");
    }

    fromAccount.setBalance(fromAccount.getBalance().subtract(amount));

    toAccount.setBalance(toAccount.getBalance().add(amount));

    accountRepository.save(fromAccount);
    accountRepository.save(toAccount);

    TransferStatus approvedStatus =
        transferStatusRepository
            .findByTransferStatusDesc("Approved")
            .orElseThrow(() -> new RuntimeException("Approved status not found"));

    TransferType sendType =
        transferTypeRepository
            .findByTransferTypeDesc("Send")
            .orElseThrow(() -> new RuntimeException("Transfer type not found"));

    Transfer transfer = new Transfer();
    transfer.setAccountFrom(fromAccount);
    transfer.setAccountTo(toAccount);
    transfer.setAmount(amount);
    transfer.setTransferStatus(approvedStatus);
    transfer.setTransferType(sendType);

    return transferRepository.save(transfer);
  }

  /*
   * Create Request - ask someone to send you money - a transfer request is created with a "Pending" status and a "Request" type.
   * The actual transfer of funds will occur when the request is accepted.
   */
  public Transfer createRequest(Long fromAccountId, Long toAccountId, BigDecimal amount) {

    if (amount.compareTo(BigDecimal.ZERO) <= 0) {
      throw new RuntimeException("Request amount must be greater than zero.");
    }

    Account fromAccount =
        accountRepository
            .findById(fromAccountId)
            .orElseThrow(() -> new RuntimeException("Source account not found"));

    Account toAccount =
        accountRepository
            .findById(toAccountId)
            .orElseThrow(() -> new RuntimeException("Destination account not found"));

    TransferStatus pendingStatus =
        transferStatusRepository
            .findByTransferStatusDesc("Pending")
            .orElseThrow(() -> new RuntimeException("Pending status not found"));

    TransferType requestType =
        transferTypeRepository
            .findByTransferTypeDesc("Request")
            .orElseThrow(() -> new RuntimeException("Request transfer type not found"));

    Transfer transfer = new Transfer();
    transfer.setAccountFrom(fromAccount);
    transfer.setAccountTo(toAccount);
    transfer.setAmount(amount);
    transfer.setTransferStatus(pendingStatus);
    transfer.setTransferType(requestType);

    return transferRepository.save(transfer);
  }

  /*
   * Approve Request
   */
  public boolean approveTransfer(Long transferId, Long accountId) {

    Transfer transfer =
        transferRepository
            .findById(transferId)
            .orElseThrow(() -> new RuntimeException("Transfer not found"));
    if (!transfer.getAccountTo().getAccountId().equals(accountId)) {

      throw new RuntimeException("Only the requested user can approve this transfer");
    }

    if ("Approved".equalsIgnoreCase(transfer.getTransferStatus().getTransferStatusDesc())) {

      throw new RuntimeException("Transfer already approved.");
    }

    Account requester = transfer.getAccountFrom();
    Account payer = transfer.getAccountTo();

    if (payer.getBalance().compareTo(transfer.getAmount()) < 0) {
      throw new RuntimeException("Insufficient funds.");
    }

    payer.setBalance(payer.getBalance().subtract(transfer.getAmount()));

    requester.setBalance(requester.getBalance().add(transfer.getAmount()));

    accountRepository.save(payer);
    accountRepository.save(requester);

    TransferStatus approvedStatus =
        transferStatusRepository
            .findByTransferStatusDesc("Approved")
            .orElseThrow(() -> new RuntimeException("Approved status not found"));

    transfer.setTransferStatus(approvedStatus);

    transferRepository.save(transfer);

    return true;
  }

  /*
   * Reject Request
   */
  public boolean rejectTransfer(Long transferId) {

    Transfer transfer =
        transferRepository
            .findById(transferId)
            .orElseThrow(() -> new RuntimeException("Transfer not found"));

    TransferStatus rejectedStatus =
        transferStatusRepository
            .findByTransferStatusDesc("Rejected")
            .orElseThrow(() -> new RuntimeException("Rejected status not found"));

    transfer.setTransferStatus(rejectedStatus);

    transferRepository.save(transfer);

    return true;
  }

  public Transfer getTransferById(Long transferId) {
    return transferRepository
        .findById(transferId)
        .orElseThrow(() -> new RuntimeException("Transfer not found"));
  }
}
