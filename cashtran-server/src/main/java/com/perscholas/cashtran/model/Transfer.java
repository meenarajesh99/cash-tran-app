package com.perscholas.cashtran.model;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "transfer")
public class Transfer {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "transfer_id")
  private Long transferId;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "account_from", nullable = false)
  private Account accountFrom;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "account_to", nullable = false)
  private Account accountTo;

  @Column(nullable = false, precision = 13, scale = 2)
  private BigDecimal amount;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "transfer_type_id", nullable = false)
  private TransferType transferType;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "transfer_status_id", nullable = false)
  private TransferStatus transferStatus;

  @Column(name = "created_at", nullable = false)
  private LocalDateTime createdAt;

  @PrePersist
  public void prePersist() {
    createdAt = LocalDateTime.now();
  }

  public Transfer() {}

  public Transfer(
      Account accountFrom,
      Account accountTo,
      BigDecimal amount,
      TransferType transferType,
      TransferStatus transferStatus) {

    this.accountFrom = accountFrom;
    this.accountTo = accountTo;
    this.amount = amount;
    this.transferType = transferType;
    this.transferStatus = transferStatus;
  }

  public Long getTransferId() {
    return transferId;
  }

  public void setTransferId(Long transferId) {
    this.transferId = transferId;
  }

  public Account getAccountFrom() {
    return accountFrom;
  }

  public void setAccountFrom(Account accountFrom) {
    this.accountFrom = accountFrom;
  }

  public Account getAccountTo() {
    return accountTo;
  }

  public void setAccountTo(Account accountTo) {
    this.accountTo = accountTo;
  }

  public BigDecimal getAmount() {
    return amount;
  }

  public void setAmount(BigDecimal amount) {
    this.amount = amount;
  }

  public TransferType getTransferType() {
    return transferType;
  }

  public void setTransferType(TransferType transferType) {
    this.transferType = transferType;
  }

  public TransferStatus getTransferStatus() {
    return transferStatus;
  }

  public void setTransferStatus(TransferStatus transferStatus) {
    this.transferStatus = transferStatus;
  }

  public LocalDateTime getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(LocalDateTime createdAt) {
    this.createdAt = createdAt;
  }
}
