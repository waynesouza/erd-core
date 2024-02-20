package com.erd.core.dto.response;

import java.io.Serializable;

public class AuthenticationResponseDTO implements Serializable {

    private final String token;
    private final String email;
    private final String fullName;

    public AuthenticationResponseDTO(String token, String email, String fullName) {
        this.token = token;
        this.email = email;
        this.fullName = fullName;
    }

    public String getToken() {
        return token;
    }

    public String getEmail() {
        return email;
    }

    public String getFullName() {
        return fullName;
    }
}
