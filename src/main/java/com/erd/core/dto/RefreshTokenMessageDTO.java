package com.erd.core.dto;

import org.springframework.http.ResponseCookie;

import java.io.Serializable;

public class RefreshTokenMessageDTO implements Serializable {

    private ResponseCookie token;
    private String message;

    public RefreshTokenMessageDTO() { }

    public RefreshTokenMessageDTO(ResponseCookie token, String message) {
        this.token = token;
        this.message = message;
    }

    public ResponseCookie getToken() {
        return token;
    }

    public void setToken(ResponseCookie token) {
        this.token = token;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

}
