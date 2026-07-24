package com.perscholas.cashtran.dto;

import com.perscholas.cashtran.model.Transfer;

import java.math.BigDecimal;

public class TransferResponseDTO {

  private Long transferId;
  private Long accountFrom;
  private Long accountTo;
  private BigDecimal amount;
  private String transferStatusDesc;
  private String transferTypeDesc;
  private String accountFromUsername;
  private String accountToUsername;

  public TransferResponseDTO() {}

  public static TransferResponseDTO from(Transfer transfer) {

    TransferResponseDTO dto = new TransferResponseDTO();

    dto.setTransferId(transfer.getTransferId());

    if (transfer.getAccountFrom() != null) {

      dto.setAccountFrom(transfer.getAccountFrom().getAccountId());

      dto.setAccountFromUsername(transfer.getAccountFrom().getUser().getUsername());
    }

    if (transfer.getAccountTo() != null) {

      dto.setAccountTo(transfer.getAccountTo().getAccountId());

      dto.setAccountToUsername(transfer.getAccountTo().getUser().getUsername());
    }

    dto.setAmount(transfer.getAmount());

    dto.setTransferStatusDesc(transfer.getTransferStatus().getTransferStatusDesc());

    dto.setTransferTypeDesc(transfer.getTransferType().getTransferTypeDesc());

    return dto;
  }

  public String getAccountFromUsername() {
    return accountFromUsername;
  }

  public void setAccountFromUsername(String username) {
    this.accountFromUsername = username;
  }

  public String getAccountToUsername() {
    return accountToUsername;
  }

  public void setAccountToUsername(String username) {
    this.accountToUsername = username;
  }

  public Long getTransferId() {
    return transferId;
  }

  public void setTransferId(Long transferId) {
    this.transferId = transferId;
  }

  public Long getAccountFrom() {
    return accountFrom;
  }

  public void setAccountFrom(Long accountFrom) {
    this.accountFrom = accountFrom;
  }

  public Long getAccountTo() {
    return accountTo;
  }

  public void setAccountTo(Long accountTo) {
    this.accountTo = accountTo;
  }

  public BigDecimal getAmount() {
    return amount;
  }

  public void setAmount(BigDecimal amount) {
    this.amount = amount;
  }

  public String getTransferStatusDesc() {
    return transferStatusDesc;
  }

  public void setTransferStatusDesc(String transferStatusDesc) {
    this.transferStatusDesc = transferStatusDesc;
  }

  public String getTransferTypeDesc() {
    return transferTypeDesc;
  }

  public void setTransferTypeDesc(String transferTypeDesc) {
    this.transferTypeDesc = transferTypeDesc;
  }
}
