package com.perscholas.cashtran.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "account")
public class Account {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "account_id")
  private Long accountId;

  /*
   * Account owns User relationship
   *
   * account table:
   *
   * account_id
   * user_id  <-- FK
   * balance
   */
  @OneToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false, unique = true)
  private User user;

  @Column(nullable = false, precision = 13, scale = 2)
  private BigDecimal balance = BigDecimal.ZERO;

  /*
   * Transfers sent from this account
   */
  @OneToMany(mappedBy = "accountFrom")
  @JsonIgnore
  private List<Transfer> outgoingTransfers = new ArrayList<>();

  /*
   * Transfers received by this account
   */
  @OneToMany(mappedBy = "accountTo")
  @JsonIgnore
  private List<Transfer> incomingTransfers = new ArrayList<>();

  public Account() {}

  public Account(User user, BigDecimal balance) {

    this.user = user;
    this.balance = balance;
  }

  public Long getAccountId() {
    return accountId;
  }

  public void setAccountId(Long accountId) {
    this.accountId = accountId;
  }

  public User getUser() {
    return user;
  }

  public void setUser(User user) {

    this.user = user;

    if (user != null && user.getAccount() != this) {

      user.setAccount(this);
    }
  }

  public BigDecimal getBalance() {
    return balance;
  }

  public void setBalance(BigDecimal balance) {
    this.balance = balance;
  }

  public List<Transfer> getOutgoingTransfers() {
    return outgoingTransfers;
  }

  public void setOutgoingTransfers(List<Transfer> outgoingTransfers) {

    this.outgoingTransfers = outgoingTransfers;
  }

  public List<Transfer> getIncomingTransfers() {
    return incomingTransfers;
  }

  public void setIncomingTransfers(List<Transfer> incomingTransfers) {

    this.incomingTransfers = incomingTransfers;
  }

  /*
   * Convenience method for transfers sent
   */
  public void addOutgoingTransfer(Transfer transfer) {

    outgoingTransfers.add(transfer);
    transfer.setAccountFrom(this);
  }

  /*
   * Convenience method for transfers received
   */
  public void addIncomingTransfer(Transfer transfer) {

    incomingTransfers.add(transfer);
    transfer.setAccountTo(this);
  }
}
