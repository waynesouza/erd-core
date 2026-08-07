package com.erd.core.exception;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class RefreshTokenExceptionTest {

    @Test
    void testConstructor_formatsTheTokenIntoTheMessage() {
        // When
        RefreshTokenException exception = new RefreshTokenException("abc-123", "Refresh token is not in database!");

        // Then
        assertEquals("Failed for [abc-123]: Refresh token is not in database!", exception.getMessage());
        assertInstanceOf(RuntimeException.class, exception);
    }

    @Test
    void testAnnotation_mapsTheExceptionToForbidden() {
        // When
        ResponseStatus responseStatus = RefreshTokenException.class.getAnnotation(ResponseStatus.class);

        // Then
        assertNotNull(responseStatus, "RefreshTokenException must carry a @ResponseStatus");
        assertEquals(HttpStatus.FORBIDDEN, responseStatus.value());
    }

}
