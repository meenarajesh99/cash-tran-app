package com.perscholas.cashtran.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public class MfaLoginRequest {

  @NotBlank private String mfaToken;

  @NotBlank
  @Pattern(regexp = "\\d{6}", message = "MFA code must be 6 digits")
  private String code;

  public String getMfaToken() {
    return mfaToken;
  }

  public void setMfaToken(String mfaToken) {
    this.mfaToken = mfaToken;
  }

  public String getCode() {
    return code;
  }

  public void setCode(String code) {
    this.code = code;
  }
}
