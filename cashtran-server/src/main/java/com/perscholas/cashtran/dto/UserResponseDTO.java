package com.perscholas.cashtran.dto;

import com.perscholas.cashtran.model.User;

public class UserResponseDTO {

    private final long id;
    private final String username;

    public UserResponseDTO(User user) {
        this.id = user.getId();
        this.username = user.getUsername();
    }

    public long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }
}
