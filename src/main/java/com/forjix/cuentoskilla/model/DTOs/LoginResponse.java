package com.forjix.cuentoskilla.model.DTOs;

import com.forjix.cuentoskilla.model.User;

public class LoginResponse {
    private String token;
    private User user;



    public LoginResponse(String token, User user) {
        this.token = token;
        this.user = user;
    }

    // Getters y setters
    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }    
}
