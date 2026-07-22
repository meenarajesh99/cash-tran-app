package com.perscholas.cashtran.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public class TransferDTO {

  @NotNull(message = "User ID is required")
  @Positive(message = "User ID must be greater than zero")
  private Long userId;

  @NotNull(message = "Amount is required")
  @Positive(message = "Amount must be greater than zero")
  private BigDecimal amount;

  public Long getUserId() {
    return userId;
  }

  public void setUserId(Long userId) {
    this.userId = userId;
  }

  public BigDecimal getAmount() {
    return amount;
  }

  public void setAmount(BigDecimal amount) {
    this.amount = amount;
  }
}
