package com.perscholas.cashtran.model;

import jakarta.persistence.*;

import java.util.Objects;

@Entity
@Table(name = "transfer_type")
public class TransferType {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "transfer_type_id")
  private Long transferTypeId;

  @Column(name = "transfer_type_desc", nullable = false, unique = true)
  private String transferTypeDesc;

  public TransferType() {}

  public TransferType(String transferTypeDesc) {
    this.transferTypeDesc = transferTypeDesc;
  }

  public Long getTransferTypeId() {
    return transferTypeId;
  }

  public void setTransferTypeId(Long transferTypeId) {
    this.transferTypeId = transferTypeId;
  }

  public String getTransferTypeDesc() {
    return transferTypeDesc;
  }

  public void setTransferTypeDesc(String transferTypeDesc) {
    this.transferTypeDesc = transferTypeDesc;
  }

  @Override
  public boolean equals(Object o) {

    if (this == o) return true;

    if (!(o instanceof TransferType)) return false;

    TransferType that = (TransferType) o;

    return Objects.equals(transferTypeId, that.transferTypeId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(transferTypeId);
  }
}
