package com.perscholas.cashtran.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public class UpdateEmailDTO {

  @NotBlank(message = "Email is required")
  @Email(message = "Please provide a valid email address")
  private String email;

  public UpdateEmailDTO() {}

  public String getEmail() {
    return email;
  }

  public void setEmail(String email) {
    this.email = email;
  }
}
