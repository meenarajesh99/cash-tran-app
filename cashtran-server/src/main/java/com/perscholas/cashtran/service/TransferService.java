package com.perscholas.cashtran.service;

import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.perscholas.cashtran.model.*;
import com.perscholas.cashtran.repository.*;
import jakarta.mail.MessagingException;
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
  private final EmailService emailService;

  public TransferService(
      UserRepository userRepository,
      TransferRepository transferRepository,
      AccountRepository accountRepository,
      TransferStatusRepository transferStatusRepository,
      TransferTypeRepository transferTypeRepository,
      EmailService emailService) {

    this.userRepository = userRepository;
    this.transferRepository = transferRepository;
    this.accountRepository = accountRepository;
    this.transferStatusRepository = transferStatusRepository;
    this.transferTypeRepository = transferTypeRepository;
    this.emailService = emailService;
  }

  public List<Transfer> getApprovedTransfers(Long accountId) {

    // Return both immediate sends (Completed) and accepted requests (Approved)
    // so the dashboard shows all finalized transfers for the account.
    return transferRepository.findCompletedOrApprovedTransfersByAccount(accountId);
  }

  /**
   * Pending requests where this account is the payer.
   *
   * <p>Example: Bob owes Alice money request. Bob sees this and can Approve/Reject.
   */
  public List<Transfer> getPendingRequestsReceived(Long accountId) {
    // Include both Pending and Rejected so that requesters and payers can see
    // the lifecycle of a request (for example, a rejected request should still
    // be visible in dashboards with status "Rejected").
    List<Transfer> pending = transferRepository.findPendingReceivedTransfers("Pending", accountId);
    List<Transfer> rejected = transferRepository.findPendingReceivedTransfers("Rejected", accountId);
    pending.addAll(rejected);
    // Optionally sort by createdAt descending so newest items appear first
    pending.sort((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()));
    return pending;
  }

  /**
   * Pending requests created by this account.
   *
   * <p>Example: Alice requested money from Bob. Alice sees this as waiting.
   */
  public List<Transfer> getPendingRequestsSent(Long accountId) {
    // Include both Pending and Rejected so the requester can see if their
    // request was subsequently rejected.
    List<Transfer> pending = transferRepository.findPendingSentTransfers("Pending", accountId);
    List<Transfer> rejected = transferRepository.findPendingSentTransfers("Rejected", accountId);
    pending.addAll(rejected);
    pending.sort((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()));
    return pending;
  }

  /*
   * Immediate Transfer - send money to someone immediately.
   */
  public Transfer createTransfer(Long fromAccountId, Long toAccountId, BigDecimal amount) {

    if (amount.compareTo(BigDecimal.ZERO) <= 0) {
      throw new org.springframework.web.server.ResponseStatusException(
          org.springframework.http.HttpStatus.BAD_REQUEST, "Transfer amount must be greater than zero.");
    }

    Account fromAccount =
        accountRepository
            .findById(fromAccountId)
            .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                org.springframework.http.HttpStatus.NOT_FOUND, "Source account not found"));

    Account toAccount =
        accountRepository
            .findById(toAccountId)
            .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                org.springframework.http.HttpStatus.NOT_FOUND, "Destination account not found"));

    if (fromAccount.getAccountId().equals(toAccount.getAccountId())) {
      throw new org.springframework.web.server.ResponseStatusException(
          org.springframework.http.HttpStatus.BAD_REQUEST, "Cannot transfer money to the same account.");
    }

    if (fromAccount.getBalance().compareTo(amount) < 0) {
      throw new org.springframework.web.server.ResponseStatusException(
          org.springframework.http.HttpStatus.UNPROCESSABLE_ENTITY, "Insufficient funds.");
    }

    // Move money.
    fromAccount.setBalance(fromAccount.getBalance().subtract(amount));
    toAccount.setBalance(toAccount.getBalance().add(amount));

    accountRepository.save(fromAccount);
    accountRepository.save(toAccount);

    TransferStatus completed =
        transferStatusRepository
            .findByTransferStatusDesc("Completed")
            .orElseThrow(
                () ->
                    new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR, "Completed status not found"));

    TransferType sendType =
        transferTypeRepository
            .findByTransferTypeDesc("Send")
            .orElseThrow(
                () ->
                    new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR, "Transfer type not found"));

    Transfer transfer = new Transfer();

    transfer.setAccountFrom(fromAccount);
    transfer.setAccountTo(toAccount);
    transfer.setAmount(amount);
    transfer.setTransferStatus(completed);
    transfer.setTransferType(sendType);

    Transfer savedTransfer = transferRepository.save(transfer);

    /*
     * Send notification to recipient.
     *
     * Email failure should NOT cause the successful transfer to fail.
     */
    sendTransferEmail(
        toAccount.getUser().getEmail(),
        toAccount.getUser().getUsername(),
        fromAccount.getUser().getUsername(),
        amount,
        "Send",
        "Completed");

    return savedTransfer;
  }

  /*
   * Create Request - ask someone to send you money.
   *
   * fromAccount = requester
   * toAccount   = person being asked to pay
   *
   * No money moves until the request is approved.
   */
  public Transfer createRequest(Long fromAccountId, Long toAccountId, BigDecimal amount) {

    if (amount.compareTo(BigDecimal.ZERO) <= 0) {
      throw new org.springframework.web.server.ResponseStatusException(
          org.springframework.http.HttpStatus.BAD_REQUEST, "Request amount must be greater than zero.");
    }

    Account fromAccount =
        accountRepository
            .findById(fromAccountId)
            .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                org.springframework.http.HttpStatus.NOT_FOUND, "Source account not found"));

    Account toAccount =
        accountRepository
            .findById(toAccountId)
            .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                org.springframework.http.HttpStatus.NOT_FOUND, "Destination account not found"));

    if (fromAccount.getAccountId().equals(toAccount.getAccountId())) {
      throw new org.springframework.web.server.ResponseStatusException(
          org.springframework.http.HttpStatus.BAD_REQUEST, "Cannot request money from the same account.");
    }

    TransferStatus pendingStatus =
        transferStatusRepository
            .findByTransferStatusDesc("Pending")
            .orElseThrow(
                () ->
                    new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR, "Pending status not found"));

    TransferType requestType =
        transferTypeRepository
            .findByTransferTypeDesc("Request")
            .orElseThrow(
                () ->
                    new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR, "Request transfer type not found"));

    Transfer transfer = new Transfer();

    transfer.setAccountFrom(fromAccount);
    transfer.setAccountTo(toAccount);
    transfer.setAmount(amount);
    transfer.setTransferStatus(pendingStatus);
    transfer.setTransferType(requestType);

    Transfer savedTransfer = transferRepository.save(transfer);

    /*
     * Notify the person being asked to pay.
     */
    sendTransferEmail(
        toAccount.getUser().getEmail(),
        toAccount.getUser().getUsername(),
        fromAccount.getUser().getUsername(),
        amount,
        "Request",
        "Pending");

    return savedTransfer;
  }

  /*
   * Approve Request.
   */
  public boolean approveTransfer(Long transferId, Long accountId) {

    Transfer transfer =
        transferRepository
            .findById(transferId)
            .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                org.springframework.http.HttpStatus.NOT_FOUND, "Transfer not found"));

    if (!transfer.getAccountTo().getAccountId().equals(accountId)) {
      throw new org.springframework.web.server.ResponseStatusException(
          org.springframework.http.HttpStatus.FORBIDDEN, "Only the requested user can approve this transfer");
    }

    if ("Approved".equalsIgnoreCase(transfer.getTransferStatus().getTransferStatusDesc())) {

      throw new org.springframework.web.server.ResponseStatusException(
          org.springframework.http.HttpStatus.CONFLICT, "Transfer already approved.");
    }

    Account requester = transfer.getAccountFrom();
    Account payer = transfer.getAccountTo();

    if (payer.getBalance().compareTo(transfer.getAmount()) < 0) {
      throw new org.springframework.web.server.ResponseStatusException(
          org.springframework.http.HttpStatus.UNPROCESSABLE_ENTITY, "Insufficient funds.");
    }

    // Move money from payer to requester.
    payer.setBalance(payer.getBalance().subtract(transfer.getAmount()));

    requester.setBalance(requester.getBalance().add(transfer.getAmount()));

    accountRepository.save(payer);
    accountRepository.save(requester);

    TransferStatus approvedStatus =
        transferStatusRepository
            .findByTransferStatusDesc("Approved")
            .orElseThrow(
                () ->
                    new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR, "Approved status not found"));

    transfer.setTransferStatus(approvedStatus);

    transferRepository.save(transfer);

    /*
     * Notify the person who requested the money.
     */
    sendTransferEmail(
        requester.getUser().getEmail(),
        requester.getUser().getUsername(),
        payer.getUser().getUsername(),
        transfer.getAmount(),
        "Request",
        "Approved");

    return true;
  }

  /*
   * Reject Request.
   */
  public boolean rejectTransfer(Long transferId, Long accountId) {

    Transfer transfer =
        transferRepository
            .findById(transferId)
            .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                org.springframework.http.HttpStatus.NOT_FOUND, "Transfer not found"));

    if (!transfer.getAccountTo().getAccountId().equals(accountId)) {
      throw new org.springframework.web.server.ResponseStatusException(
          org.springframework.http.HttpStatus.FORBIDDEN, "Only the requested user can reject this transfer");
    }

    TransferStatus rejectedStatus =
        transferStatusRepository
            .findByTransferStatusDesc("Rejected")
            .orElseThrow(
                () ->
                    new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR, "Rejected status not found"));

    transfer.setTransferStatus(rejectedStatus);

    transferRepository.save(transfer);

    Account requester = transfer.getAccountFrom();
    Account payer = transfer.getAccountTo();

    /*
     * Notify the person who requested the money.
     */
    sendTransferEmail(
        requester.getUser().getEmail(),
        requester.getUser().getUsername(),
        payer.getUser().getUsername(),
        transfer.getAmount(),
        "Request",
        "Rejected");

    return true;
  }

  /*
   * Send transfer-related email.
   *
   * Email problems are intentionally caught here so that an SMTP problem
   * does not cause a successful financial transaction to fail.
   */
  private void sendTransferEmail(
      String to,
      String username,
      String otherUsername,
      BigDecimal amount,
      String transferType,
      String status) {

    try {

      emailService.sendTransferNotification(
          to, username, otherUsername, amount, transferType, status);

    } catch (MessagingException e) {

      System.err.println(
          "Failed to send CashTran transfer notification to " + to + ": " + e.getMessage());
    }
  }

  public Transfer getTransferById(Long transferId) {

    return transferRepository
        .findById(transferId)
        .orElseThrow(
            () -> new org.springframework.web.server.ResponseStatusException(
                org.springframework.http.HttpStatus.NOT_FOUND, "Transfer not found"));
  }

  @Transactional(readOnly = true)
  public byte[] generateStatement(String username) {

    User user =
        userRepository
            .findByUsernameWithAccount(username)
            .orElseThrow(
                () ->
                    new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.NOT_FOUND, "User not found"));

    Account account =
        accountRepository
            .findByUserId(user.getUserId())
            .orElseThrow(
                () ->
                    new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.NOT_FOUND, "Account not found"));

    // Include both Completed (immediate sends) and Approved (accepted requests)
    // so the PDF statement reflects all finalized transfers for the account.
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

      throw new org.springframework.web.server.ResponseStatusException(
          org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR, "Unable to generate PDF statement", ex);
    }
  }
}
