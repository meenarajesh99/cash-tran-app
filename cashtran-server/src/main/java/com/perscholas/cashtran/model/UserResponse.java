package com.perscholas.cashtran.model;

public class UserResponse {

    private final long id;
    private final String username;

    public UserResponse(User user) {
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
