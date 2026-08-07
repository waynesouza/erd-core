package com.erd.core.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.InsufficientAuthenticationException;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

class AuthenticationEntryPointJwtTest {

    private AuthenticationEntryPointJwt authenticationEntryPointJwt;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        authenticationEntryPointJwt = new AuthenticationEntryPointJwt();
        objectMapper = new ObjectMapper();
    }

    @Test
    void testCommence_writesAJsonUnauthorizedBody() throws Exception {
        // Given
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/project/123");
        request.setServletPath("/api/project/123");
        MockHttpServletResponse response = new MockHttpServletResponse();

        // When
        authenticationEntryPointJwt.commence(request, response,
                new InsufficientAuthenticationException("Full authentication is required"));

        // Then
        assertEquals(401, response.getStatus());
        assertEquals(APPLICATION_JSON_VALUE, response.getContentType());

        Map<?, ?> body = objectMapper.readValue(response.getContentAsByteArray(), Map.class);
        assertEquals(401, body.get("status"));
        assertEquals("Unauthorized", body.get("error"));
        assertEquals("Full authentication is required", body.get("message"));
        assertEquals("/api/project/123", body.get("path"));
    }

}
