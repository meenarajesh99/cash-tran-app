package com.perscholas.cashtran.dto;

public class LoginResponseDTO {

  private final String token;
  private final UserResponseDTO user;
  private final boolean mfaRequired;
  private final String mfaToken;

  public LoginResponseDTO(
      String token, UserResponseDTO user, boolean mfaRequired, String mfaToken) {

    this.token = token;
    this.user = user;
    this.mfaRequired = mfaRequired;
    this.mfaToken = mfaToken;
  }

  public String getToken() {
    return token;
  }

  public UserResponseDTO getUser() {
    return user;
  }

  public boolean isMfaRequired() {
    return mfaRequired;
  }

  public String getMfaToken() {
    return mfaToken;
  }
}
