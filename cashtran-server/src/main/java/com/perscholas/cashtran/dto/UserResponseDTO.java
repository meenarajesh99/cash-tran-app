package com.perscholas.cashtran.dto;

import com.perscholas.cashtran.model.User;

public class UserResponseDTO {

  private Long id;
  private String username;
  private String email;
  private boolean activated;

  public UserResponseDTO(Long id, String username, String email, boolean activated) {
    this.id = id;
    this.username = username;
    this.email = email;
    this.activated = activated;
  }

  public static UserResponseDTO from(User user) {

    return new UserResponseDTO(
        user.getUserId(), user.getUsername(), user.getEmail(), user.isActivated());
  }

  public Long getId() {
    return id;
  }

  public String getUsername() {
    return username;
  }

  public String getEmail() {
    return email;
  }

  public boolean isActivated() {
    return activated;
  }
}
