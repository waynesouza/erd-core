package com.erd.core.dto.response;

import java.io.Serializable;

public class AuthenticationResponseDTO implements Serializable {

    private String token;
    private String refreshToken;
    private String email;
    private String fullName;

    public AuthenticationResponseDTO(String token, String refreshToken, String email, String fullName) {
        this.token = token;
        this.refreshToken = refreshToken;
        this.email = email;
        this.fullName = fullName;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

}
