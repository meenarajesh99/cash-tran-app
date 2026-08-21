package com.perscholas.cashtran.service;

import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.perscholas.cashtran.model.*;
import com.perscholas.cashtran.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@Transactional
public class TransferService {

  private final UserRepository userRepository;
  private final TransferRepository transferRepository;
  private final AccountRepository accountRepository;
  private final TransferStatusRepository transferStatusRepository;
  private final TransferTypeRepository transferTypeRepository;

  public TransferService(
      UserRepository userRepository,
      TransferRepository transferRepository,
      AccountRepository accountRepository,
      TransferStatusRepository transferStatusRepository,
      TransferTypeRepository transferTypeRepository) {
    this.userRepository = userRepository;
    this.transferRepository = transferRepository;
    this.accountRepository = accountRepository;
    this.transferStatusRepository = transferStatusRepository;
    this.transferTypeRepository = transferTypeRepository;
  }

  public List<Transfer> getApprovedTransfers(Long accountId) {
    // Return both immediate sends (Completed) and accepted requests (Approved)
    // so the dashboard shows all finalized transfers for the account.
    return transferRepository.findCompletedOrApprovedTransfersByAccount(accountId);
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

    TransferStatus completed =
        transferStatusRepository
            .findByTransferStatusDesc("Completed")
            .orElseThrow(() -> new RuntimeException("Completed status not found"));

    TransferType sendType =
        transferTypeRepository
            .findByTransferTypeDesc("Send")
            .orElseThrow(() -> new RuntimeException("Transfer type not found"));

    Transfer transfer = new Transfer();
    transfer.setAccountFrom(fromAccount);
    transfer.setAccountTo(toAccount);
    transfer.setAmount(amount);
    transfer.setTransferStatus(completed);
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
  public boolean rejectTransfer(Long transferId, Long accountId) {

    Transfer transfer =
        transferRepository
            .findById(transferId)
            .orElseThrow(() -> new RuntimeException("Transfer not found"));

    if (!transfer.getAccountTo().getAccountId().equals(accountId)) {
      throw new RuntimeException("Only the requested user can reject this transfer");
    }

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

  @Transactional(readOnly = true)
  public byte[] generateStatement(String username) {

    User user =
        userRepository
            .findByUsernameWithAccount(username)
            .orElseThrow(() -> new RuntimeException("User not found"));

    Account account =
        accountRepository
            .findByUserId(user.getUserId())
            .orElseThrow(() -> new RuntimeException("Account not found"));

    // Include both Completed (immediate sends) and Approved (accepted requests) so
    // the PDF statement reflects all finalized transfers for the account.
    List<Transfer> transfers =
        transferRepository.findCompletedOrApprovedTransfersByAccount(account.getAccountId());

    try {

      ByteArrayOutputStream output = new ByteArrayOutputStream();

      Document document = new Document(PageSize.LETTER);

      PdfWriter.getInstance(document, output);

      document.open();

      Font titleFont = new Font(Font.HELVETICA, 18, Font.BOLD);
      Font headerFont = new Font(Font.HELVETICA, 12, Font.BOLD);
      document.add(new Paragraph("CashTran Transaction Statement", titleFont));
      document.add(new Paragraph(" "));
      document.add(new Paragraph("Customer: " + username));
      document.add(new Paragraph(" "));
      document.add(new Paragraph("Generated: " + java.time.LocalDate.now()));
      document.add(new Paragraph(" "));

      PdfPTable table = new PdfPTable(7);
      table.setWidthPercentage(100);
      table.setWidths(new float[] {2.2f, 0.8f, 1.2f, 1.5f, 1.5f, 1.2f, 1.2f});
      table.addCell(new Phrase("Date", headerFont));
      table.addCell(new Phrase("ID", headerFont));
      table.addCell(new Phrase("Type", headerFont));
      table.addCell(new Phrase("From", headerFont));
      table.addCell(new Phrase("To", headerFont));
      table.addCell(new Phrase("Amount", headerFont));
      table.addCell(new Phrase("Status", headerFont));

      DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM/dd/yyyy HH:mm");

      for (Transfer transfer : transfers) {
        String displayStatus;

        if (transfer.getTransferType().getTransferTypeDesc().equalsIgnoreCase("Send")) {

          displayStatus = "Completed";

        } else {

          displayStatus = transfer.getTransferStatus().getTransferStatusDesc();
        }

        table.addCell(transfer.getCreatedAt().format(formatter));

        table.addCell(String.valueOf(transfer.getTransferId()));

        table.addCell(transfer.getTransferType().getTransferTypeDesc());

        table.addCell(transfer.getAccountFrom().getUser().getUsername());

        table.addCell(transfer.getAccountTo().getUser().getUsername());

        table.addCell("$" + transfer.getAmount().setScale(2, RoundingMode.HALF_UP));

        table.addCell(displayStatus);
      }

      document.add(table);

      document.close();

      return output.toByteArray();

    } catch (Exception ex) {
      throw new RuntimeException("Unable to generate PDF statement", ex);
    }
  }
}
