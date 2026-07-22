package com.perscholas.cashtran.model;

import jakarta.persistence.*;

import java.util.Objects;

@Entity
@Table(name = "transfer_status")
public class TransferStatus {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "transfer_status_id")
  private Long transferStatusId;

  @Column(name = "transfer_status_desc", nullable = false, unique = true)
  private String transferStatusDesc;

  public TransferStatus() {}

  public TransferStatus(String transferStatusDesc) {
    this.transferStatusDesc = transferStatusDesc;
  }

  public Long getTransferStatusId() {
    return transferStatusId;
  }

  public void setTransferStatusId(Long transferStatusId) {
    this.transferStatusId = transferStatusId;
  }

  public String getTransferStatusDesc() {
    return transferStatusDesc;
  }

  public void setTransferStatusDesc(String transferStatusDesc) {
    this.transferStatusDesc = transferStatusDesc;
  }

  @Override
  public boolean equals(Object o) {

    if (this == o) return true;

    if (!(o instanceof TransferStatus)) return false;

    TransferStatus that = (TransferStatus) o;

    return Objects.equals(transferStatusId, that.transferStatusId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(transferStatusId);
  }
}
