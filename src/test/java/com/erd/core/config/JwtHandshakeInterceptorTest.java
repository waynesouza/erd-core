package com.erd.core.config;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.socket.WebSocketHandler;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
class JwtHandshakeInterceptorTest {

    private static final String COOKIE_NAME = "erd-token-test";

    @Mock
    private WebSocketHandler webSocketHandler;

    private JwtHandshakeInterceptor jwtHandshakeInterceptor;
    private ServerHttpResponse response;
    private Map<String, Object> attributes;

    @BeforeEach
    void setUp() {
        jwtHandshakeInterceptor = new JwtHandshakeInterceptor();
        ReflectionTestUtils.setField(jwtHandshakeInterceptor, "cookieName", COOKIE_NAME);
        response = new ServletServerHttpResponse(new MockHttpServletResponse());
        attributes = new HashMap<>();
    }

    private ServerHttpRequest servletRequestWith(Cookie... cookies) {
        MockHttpServletRequest httpRequest = new MockHttpServletRequest("GET", "/ws");
        if (cookies.length > 0) {
            httpRequest.setCookies(cookies);
        }
        return new ServletServerHttpRequest(httpRequest);
    }

    @Test
    void testBeforeHandshake_copiesTheJwtCookieIntoTheSessionAttributes() {
        // Given
        ServerHttpRequest request = servletRequestWith(new Cookie(COOKIE_NAME, "the-token"));

        // When
        boolean proceed = jwtHandshakeInterceptor.beforeHandshake(request, response, webSocketHandler, attributes);

        // Then
        assertTrue(proceed);
        assertEquals("the-token", attributes.get(WebSocketAuthInterceptor.JWT_TOKEN_ATTR));
    }

    @Test
    void testBeforeHandshake_ignoresUnrelatedCookies() {
        // Given
        ServerHttpRequest request = servletRequestWith(new Cookie("some-other-cookie", "value"));

        // When
        boolean proceed = jwtHandshakeInterceptor.beforeHandshake(request, response, webSocketHandler, attributes);

        // Then
        assertTrue(proceed, "The handshake proceeds unauthenticated rather than being rejected here");
        assertFalse(attributes.containsKey(WebSocketAuthInterceptor.JWT_TOKEN_ATTR));
    }

    @Test
    void testBeforeHandshake_toleratesARequestWithoutCookies() {
        // Given
        ServerHttpRequest request = servletRequestWith();

        // When
        boolean proceed = jwtHandshakeInterceptor.beforeHandshake(request, response, webSocketHandler, attributes);

        // Then
        assertTrue(proceed);
        assertTrue(attributes.isEmpty());
    }

    @Test
    void testBeforeHandshake_toleratesANonServletRequest(@Mock ServerHttpRequest nonServletRequest) {
        // When
        boolean proceed =
                jwtHandshakeInterceptor.beforeHandshake(nonServletRequest, response, webSocketHandler, attributes);

        // Then
        assertTrue(proceed);
        assertTrue(attributes.isEmpty());
    }

    @Test
    void testAfterHandshake_isANoOp() {
        // Given
        ServerHttpRequest request = servletRequestWith();

        // When & Then - must not throw
        jwtHandshakeInterceptor.afterHandshake(request, response, webSocketHandler, null);
    }

}
