package com.perscholas.cashtran.dto;

public class LoginResponseDTO {

    private final String token;
    private final UserResponseDTO user;

    public LoginResponseDTO(
            String token,
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
