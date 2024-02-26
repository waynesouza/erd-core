package com.erd.core.exception;

import org.springframework.web.bind.annotation.ResponseStatus;

import java.io.Serial;

import static org.springframework.http.HttpStatus.FORBIDDEN;

@ResponseStatus(FORBIDDEN)
public class RefreshTokenException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    public RefreshTokenException(String token, String message) {
        super(String.format("Failed for [%s]: %s", token, message));
    }

}
