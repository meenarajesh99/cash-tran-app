package com.perscholas.cashtran.dto;

public class MfaSetupResponse {

  private String otpAuthUrl;

  public MfaSetupResponse(String otpAuthUrl) {
    this.otpAuthUrl = otpAuthUrl;
  }

  public String getOtpAuthUrl() {
    return otpAuthUrl;
  }
}
