package com.erd.core.advice;

import com.erd.core.dto.error.ErrorMessageDTO;
import com.erd.core.exception.RefreshTokenException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.context.request.ServletWebRequest;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TokenControllerAdviceTest {

    private TokenControllerAdvice tokenControllerAdvice;

    @BeforeEach
    void setUp() {
        tokenControllerAdvice = new TokenControllerAdvice();
    }

    @Test
    void testHandleTokenRefreshException_buildsTheErrorPayload() {
        // Given
        MockHttpServletRequest httpRequest = new MockHttpServletRequest("POST", "/api/auth/refresh-token");
        ServletWebRequest webRequest = new ServletWebRequest(httpRequest);
        RefreshTokenException exception = new RefreshTokenException("abc-123", "Refresh token is not in database!");

        // When
        ErrorMessageDTO error = tokenControllerAdvice.handleTokenRefreshException(exception, webRequest);

        // Then
        assertEquals(HttpStatus.FORBIDDEN.value(), error.getStatusCode());
        assertEquals(exception.getMessage(), error.getMessage());
        assertNotNull(error.getTimestamp());
        assertTrue(error.getDescription().contains("/api/auth/refresh-token"));
    }

    @Test
    void testHandler_isAnnotatedWithForbidden() throws Exception {
        // When
        Method handler = TokenControllerAdvice.class
                .getMethod("handleTokenRefreshException", RefreshTokenException.class,
                        org.springframework.web.context.request.WebRequest.class);
        ResponseStatus responseStatus = handler.getAnnotation(ResponseStatus.class);

        // Then
        assertNotNull(responseStatus);
        assertEquals(HttpStatus.FORBIDDEN, responseStatus.value());
    }

}
