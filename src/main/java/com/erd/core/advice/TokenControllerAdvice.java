package com.erd.core.advice;

import com.erd.core.dto.error.ErrorMessageDTO;
import com.erd.core.exception.RefreshTokenException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import java.time.LocalDateTime;

import static org.springframework.http.HttpStatus.FORBIDDEN;

@RestControllerAdvice
public class TokenControllerAdvice {

    @ExceptionHandler(value = RefreshTokenException.class)
    @ResponseStatus(FORBIDDEN)
    public ErrorMessageDTO handleTokenRefreshException(RefreshTokenException ex, WebRequest request) {
        return new ErrorMessageDTO(FORBIDDEN.value(), LocalDateTime.now(), ex.getMessage(), request.getDescription(false));
    }

}
