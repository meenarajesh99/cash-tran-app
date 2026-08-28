package com.perscholas.cashtran.dto;

public class LoginResultDTO {

  private final boolean mfaRequired;
  private final String username;
  private final String token;

  public LoginResultDTO(boolean mfaRequired, String username, String token) {

    this.mfaRequired = mfaRequired;
    this.username = username;
    this.token = token;
  }

  public boolean isMfaRequired() {
    return mfaRequired;
  }

  public String getUsername() {
    return username;
  }

  public String getToken() {
    return token;
  }
}
