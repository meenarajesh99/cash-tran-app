package com.perscholas.cashtran.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public class MfaEnrollmentRequest {

  @NotBlank private String enrollmentToken;

  @NotBlank
  @Pattern(regexp = "\\d{6}", message = "MFA code must be 6 digits")
  private String code;

  public String getEnrollmentToken() {
    return enrollmentToken;
  }

  public void setEnrollmentToken(String enrollmentToken) {
    this.enrollmentToken = enrollmentToken;
  }

  public String getCode() {
    return code;
  }

  public void setCode(String code) {
    this.code = code;
  }
}
