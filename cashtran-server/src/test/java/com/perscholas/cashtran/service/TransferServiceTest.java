package com.perscholas.cashtran.service;

import com.perscholas.cashtran.model.*;
import com.perscholas.cashtran.repository.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigDecimal;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransferServiceTest {
  @Mock UserRepository userRepository;
  @Mock TransferRepository transferRepository;
  @Mock AccountRepository accountRepository;
  @Mock TransferStatusRepository transferStatusRepository;
  @Mock TransferTypeRepository transferTypeRepository;
  @InjectMocks TransferService transferService;

  @Test
  void createTransferMovesFundsAndCreatesCompletedSend() {
    Account from = account(1L, "100.00"), to = account(2L, "30.00");
    when(accountRepository.findById(1L)).thenReturn(Optional.of(from));
    when(accountRepository.findById(2L)).thenReturn(Optional.of(to));
    when(transferStatusRepository.findByTransferStatusDesc("Completed"))
        .thenReturn(Optional.of(new TransferStatus("Completed")));
    when(transferTypeRepository.findByTransferTypeDesc("Send"))
        .thenReturn(Optional.of(new TransferType("Send")));
    when(transferRepository.save(any())).thenAnswer(i -> i.getArgument(0));

    Transfer transfer = transferService.createTransfer(1L, 2L, new BigDecimal("25.00"));

    assertEquals(new BigDecimal("75.00"), from.getBalance());
    assertEquals(new BigDecimal("55.00"), to.getBalance());
    assertEquals("Completed", transfer.getTransferStatus().getTransferStatusDesc());
    assertEquals("Send", transfer.getTransferType().getTransferTypeDesc());
    verify(accountRepository).save(from);
    verify(accountRepository).save(to);
    verify(transferRepository).save(transfer);
  }

  @Test
  void createTransferRejectsNonPositiveAndInsufficientAmounts() {
    assertEquals(
        "Transfer amount must be greater than zero.",
        assertThrows(
                RuntimeException.class,
                () -> transferService.createTransfer(1L, 2L, BigDecimal.ZERO))
            .getMessage());
    Account from = account(1L, "10.00"), to = account(2L, "30.00");
    when(accountRepository.findById(1L)).thenReturn(Optional.of(from));
    when(accountRepository.findById(2L)).thenReturn(Optional.of(to));
    assertEquals(
        "Insufficient funds.",
        assertThrows(
                RuntimeException.class,
                () -> transferService.createTransfer(1L, 2L, new BigDecimal("10.01")))
            .getMessage());
    verify(accountRepository, never()).save(any());
  }

  @Test
  void approveTransferRequiresPayerAndMovesFunds() {
    Account requester = account(1L, "10.00"), payer = account(2L, "50.00");
    Transfer transfer = new Transfer();
    transfer.setAccountFrom(requester);
    transfer.setAccountTo(payer);
    transfer.setAmount(new BigDecimal("20.00"));
    transfer.setTransferStatus(new TransferStatus("Pending"));
    when(transferRepository.findById(9L)).thenReturn(Optional.of(transfer));
    when(transferStatusRepository.findByTransferStatusDesc("Approved"))
        .thenReturn(Optional.of(new TransferStatus("Approved")));

    assertTrue(transferService.approveTransfer(9L, 2L));
    assertEquals(new BigDecimal("30.00"), payer.getBalance());
    assertEquals(new BigDecimal("30.00"), requester.getBalance());
    assertEquals("Approved", transfer.getTransferStatus().getTransferStatusDesc());
    verify(accountRepository).save(payer);
    verify(accountRepository).save(requester);
    verify(transferRepository).save(transfer);
  }

  @Test
  void approveTransferRejectsDifferentAccount() {
    Transfer transfer = new Transfer();
    transfer.setAccountTo(account(2L, "50.00"));
    when(transferRepository.findById(9L)).thenReturn(Optional.of(transfer));
    assertEquals(
        "Only the requested user can approve this transfer",
        assertThrows(RuntimeException.class, () -> transferService.approveTransfer(9L, 1L))
            .getMessage());
    verifyNoInteractions(accountRepository, transferStatusRepository);
  }

  @Test
  void createRequestCreatesPendingRequestWithoutMovingFunds() {
    Account requester = account(1L, "100.00"), payer = account(2L, "30.00");
    when(accountRepository.findById(1L)).thenReturn(Optional.of(requester));
    when(accountRepository.findById(2L)).thenReturn(Optional.of(payer));
    when(transferStatusRepository.findByTransferStatusDesc("Pending"))
        .thenReturn(Optional.of(new TransferStatus("Pending")));
    when(transferTypeRepository.findByTransferTypeDesc("Request"))
        .thenReturn(Optional.of(new TransferType("Request")));
    when(transferRepository.save(any())).thenAnswer(i -> i.getArgument(0));

    Transfer transfer = transferService.createRequest(1L, 2L, new BigDecimal("25.00"));

    assertEquals(new BigDecimal("100.00"), requester.getBalance());
    assertEquals(new BigDecimal("30.00"), payer.getBalance());
    assertEquals("Pending", transfer.getTransferStatus().getTransferStatusDesc());
    assertEquals("Request", transfer.getTransferType().getTransferTypeDesc());
    verify(accountRepository, never()).save(any());
    verify(transferRepository).save(transfer);
  }

  @Test
  void approveTransferRejectsInsufficientPayerFundsWithoutChangingBalances() {
    Account requester = account(1L, "10.00"), payer = account(2L, "19.99");
    Transfer transfer = new Transfer();
    transfer.setAccountFrom(requester);
    transfer.setAccountTo(payer);
    transfer.setAmount(new BigDecimal("20.00"));
    transfer.setTransferStatus(new TransferStatus("Pending"));
    when(transferRepository.findById(9L)).thenReturn(Optional.of(transfer));

    assertEquals(
        "Insufficient funds.",
        assertThrows(RuntimeException.class, () -> transferService.approveTransfer(9L, 2L))
            .getMessage());

    assertEquals(new BigDecimal("10.00"), requester.getBalance());
    assertEquals(new BigDecimal("19.99"), payer.getBalance());
    verify(accountRepository, never()).save(any());
    verify(transferRepository, never()).save(transfer);
    verifyNoInteractions(transferStatusRepository);
  }

  @Test
  void rejectTransferMarksRequestRejectedWithoutMovingFunds() {
    Account requester = account(1L, "10.00"), payer = account(2L, "50.00");
    Transfer transfer = new Transfer();
    transfer.setAccountFrom(requester);
    transfer.setAccountTo(payer);
    transfer.setAmount(new BigDecimal("20.00"));
    transfer.setTransferStatus(new TransferStatus("Pending"));
    when(transferRepository.findById(9L)).thenReturn(Optional.of(transfer));
    when(transferStatusRepository.findByTransferStatusDesc("Rejected"))
        .thenReturn(Optional.of(new TransferStatus("Rejected")));

    assertTrue(transferService.rejectTransfer(9L, 2L));

    assertEquals(new BigDecimal("10.00"), requester.getBalance());
    assertEquals(new BigDecimal("50.00"), payer.getBalance());
    assertEquals("Rejected", transfer.getTransferStatus().getTransferStatusDesc());
    verify(accountRepository, never()).save(any());
    verify(transferRepository).save(transfer);
  }

  @Test
  void rejectTransferRejectsDifferentAccount() {
    Transfer transfer = new Transfer();
    transfer.setAccountTo(account(2L, "50.00"));
    when(transferRepository.findById(9L)).thenReturn(Optional.of(transfer));

    assertEquals(
        "Only the requested user can reject this transfer",
        assertThrows(RuntimeException.class, () -> transferService.rejectTransfer(9L, 1L))
            .getMessage());

    verifyNoInteractions(accountRepository, transferStatusRepository);
    verify(transferRepository, never()).save(transfer);
  }

  private Account account(Long id, String balance) {
    Account account = new Account();
    account.setAccountId(id);
    account.setBalance(new BigDecimal(balance));
    return account;
  }
}
