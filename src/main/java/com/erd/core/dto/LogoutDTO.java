package com.erd.core.dto;

public class LogoutDTO {

    private String tokenCookie;
    private String refreshTokenCookie;

    public LogoutDTO() { }

    public LogoutDTO(String tokenCookie, String refreshTokenCookie) {
        this.tokenCookie = tokenCookie;
        this.refreshTokenCookie = refreshTokenCookie;
    }

    public String getTokenCookie() {
        return tokenCookie;
    }

    public String getRefreshTokenCookie() {
        return refreshTokenCookie;
    }
}
