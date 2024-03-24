package com.erd.core.dto;

import org.springframework.http.ResponseCookie;

import java.io.Serializable;

public class RefreshTokenMessageDTO implements Serializable {

    private ResponseCookie token;
    private ResponseCookie refreshToken;
    private String message;

    public RefreshTokenMessageDTO() { }

    public RefreshTokenMessageDTO(ResponseCookie token, ResponseCookie refreshToken, String message) {
        this.token = token;
        this.refreshToken = refreshToken;
        this.message = message;
    }

    public ResponseCookie getToken() {
        return token;
    }

    public void setToken(ResponseCookie token) {
        this.token = token;
    }

    public ResponseCookie getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(ResponseCookie refreshToken) {
        this.refreshToken = refreshToken;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

}
