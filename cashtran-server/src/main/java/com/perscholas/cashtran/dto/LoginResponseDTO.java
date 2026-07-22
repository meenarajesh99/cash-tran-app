package com.perscholas.cashtran.dto;

import jakarta.validation.constraints.NotBlank;

public class LoginResponseDTO {

    private final String token;
    private final UserResponseDTO user;

    public LoginResponseDTO(
            @NotBlank String token,
            UserResponseDTO user) {

        this.token = token;
        this.user = user;
    }

    public String getToken() {
        return token;
    }

    public UserResponseDTO getUser() {
        return user;
    }
}
