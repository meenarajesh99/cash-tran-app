package com.perscholas.cashtran.service;

import com.perscholas.cashtran.model.Transfer;
import com.perscholas.cashtran.model.TransferStatus;
import com.perscholas.cashtran.repository.AccountRepository;
import com.perscholas.cashtran.repository.TransferRepository;
import com.perscholas.cashtran.repository.TransferStatusRepository;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
public class TransferService {

  private final TransferRepository transferRepository;
  private final AccountRepository accountRepository;
  private final TransferStatusRepository transferStatusRepository;

  public TransferService(
      TransferRepository transferRepository,
      AccountRepository accountRepository,
      TransferStatusRepository transferStatusRepository) {

    this.transferRepository = transferRepository;
    this.accountRepository = accountRepository;
    this.transferStatusRepository = transferStatusRepository;
  }

  public List<Transfer> getApprovedTransfers(Long accountId) {

    return transferRepository.findTransfersByStatusAndAccount("Approved", accountId);
  }

  public List<Transfer> getPendingTransfers(Long accountId) {

    return transferRepository.findPendingTransfers("Pending", accountId);
  }

  /*
   * Immediate transfer
   * Status = Approved
   */
  public Transfer createTransfer(Long fromAccountId, Long toAccountId, BigDecimal amount) {

    Transfer transfer = new Transfer();

    transfer.setAccountFrom(
        accountRepository
            .findById(fromAccountId)
            .orElseThrow(() -> new RuntimeException("Source account not found")));

    transfer.setAccountTo(
        accountRepository
            .findById(toAccountId)
            .orElseThrow(() -> new RuntimeException("Destination account not found")));

    transfer.setAmount(amount);

    TransferStatus approvedStatus =
        transferStatusRepository
            .findByTransferStatusDesc("Approved")
            .orElseThrow(() -> new RuntimeException("Approved status not found"));

    transfer.setTransferStatus(approvedStatus);

    return transferRepository.save(transfer);
  }

  /*
   * Transfer request
   * Status = Pending
   */
  public Transfer createRequest(Long fromAccountId, Long toAccountId, BigDecimal amount) {

    Transfer transfer = new Transfer();

    transfer.setAccountFrom(
        accountRepository
            .findById(fromAccountId)
            .orElseThrow(() -> new RuntimeException("Source account not found")));

    transfer.setAccountTo(
        accountRepository
            .findById(toAccountId)
            .orElseThrow(() -> new RuntimeException("Destination account not found")));

    transfer.setAmount(amount);

    TransferStatus pendingStatus =
        transferStatusRepository
            .findByTransferStatusDesc("Pending")
            .orElseThrow(() -> new RuntimeException("Pending status not found"));

    transfer.setTransferStatus(pendingStatus);

    return transferRepository.save(transfer);
  }

  /*
   * Accept pending transfer
   */
  public boolean acceptTransfer(Long transferId) {

    Transfer transfer =
        transferRepository
            .findById(transferId)
            .orElseThrow(() -> new RuntimeException("Transfer not found"));

    TransferStatus approvedStatus =
        transferStatusRepository
            .findByTransferStatusDesc("Approved")
            .orElseThrow(() -> new RuntimeException("Approved status not found"));

    transfer.setTransferStatus(approvedStatus);

    transferRepository.save(transfer);

    return true;
  }

  /*
   * Reject pending transfer
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
