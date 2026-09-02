package com.perscholas.cashtran.dto;

import com.perscholas.cashtran.model.Account;
import com.perscholas.cashtran.model.User;

public class UserResponseDTO {

  private Long id;
  private Long accountId;
  private String username;
  private String email;
  private boolean activated;

  public static UserResponseDTO from(User user) {

    UserResponseDTO dto = new UserResponseDTO();

    dto.setId(user.getUserId());
    dto.setUsername(user.getUsername());
    dto.setEmail(user.getEmail());
    dto.setActivated(user.isActivated());

    Account account = user.getAccount();

    if (account != null) {
      dto.setAccountId(account.getAccountId());
    }

    return dto;
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public Long getAccountId() {
    return accountId;
  }

  public void setAccountId(Long accountId) {
    this.accountId = accountId;
  }

  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  public String getEmail() {
    return email;
  }

  public void setEmail(String email) {
    this.email = email;
  }

  public boolean isActivated() {
    return activated;
  }

  public void setActivated(boolean activated) {
    this.activated = activated;
  }
}
