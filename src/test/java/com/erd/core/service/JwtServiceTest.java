package com.erd.core.service;

import com.erd.core.enumeration.RoleEnum;
import com.erd.core.model.User;
import io.jsonwebtoken.Jwts;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseCookie;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link JwtService} performs real signing and parsing, so these tests use genuine JJWT operations
 * against a valid HS256 key instead of mocks. The {@code @Value} fields are populated directly
 * because the service is exercised outside of a Spring context.
 */
class JwtServiceTest {

    private static final String SECRET =
            "dGVzdC1zZWNyZXQta2V5LWZvci11bml0LXRlc3RzLW9ubHktbXVzdC1iZS1hdC1sZWFzdC0yNTYtYml0cw==";
    private static final String COOKIE_NAME = "erd-token-test";
    private static final String REFRESH_COOKIE_NAME = "erd-refresh-token-test";

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "secret", SECRET);
        ReflectionTestUtils.setField(jwtService, "expiration", 300000L);
        ReflectionTestUtils.setField(jwtService, "refreshExpiration", 2629746000L);
        ReflectionTestUtils.setField(jwtService, "cookieName", COOKIE_NAME);
        ReflectionTestUtils.setField(jwtService, "refreshCookieName", REFRESH_COOKIE_NAME);
    }

    private User sampleUser() {
        return new User("Ada", "Lovelace", "ada@erd.com", "secret", RoleEnum.USER);
    }

    // ------------------------------------------------------------------
    // Cookie generation
    // ------------------------------------------------------------------

    @Test
    void testGenerateTokenCookie_buildsASignedCookieScopedToTheApi() {
        // When
        ResponseCookie cookie = jwtService.generateTokenCookie(sampleUser());

        // Then
        assertEquals(COOKIE_NAME, cookie.getName());
        assertEquals("/api", cookie.getPath());
        assertTrue(cookie.isHttpOnly());
        assertFalse(cookie.isSecure());
        assertEquals("Lax", cookie.getSameSite());
        assertEquals("ada@erd.com", jwtService.getEmailFromToken(cookie.getValue()));
    }

    @Test
    void testGenerateRefreshTokenCookie_isScopedToTheRefreshEndpoint() {
        // When
        ResponseCookie cookie = jwtService.generateRefreshTokenCookie("refresh-token-value");

        // Then
        assertEquals(REFRESH_COOKIE_NAME, cookie.getName());
        assertEquals("refresh-token-value", cookie.getValue());
        assertEquals("/api/auth/refresh-token", cookie.getPath());
        assertTrue(cookie.isHttpOnly());
    }

    @Test
    void testDeleteTokenCookie_producesAnEmptyCookieOnTheSamePath() {
        // When
        ResponseCookie cookie = jwtService.deleteTokenCookie();

        // Then
        assertEquals(COOKIE_NAME, cookie.getName());
        assertEquals("", cookie.getValue());
        assertEquals("/api", cookie.getPath());
    }

    @Test
    void testDeleteRefreshTokenCookie_producesAnEmptyCookieOnTheSamePath() {
        // When
        ResponseCookie cookie = jwtService.deleteRefreshTokenCookie();

        // Then
        assertEquals(REFRESH_COOKIE_NAME, cookie.getName());
        assertEquals("", cookie.getValue());
        assertEquals("/api/auth/refresh-token", cookie.getPath());
    }

    // ------------------------------------------------------------------
    // Cookie reading
    // ------------------------------------------------------------------

    @Test
    void testGetTokenFromCookie_returnsTheValueWhenPresent() {
        // Given
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(new Cookie(COOKIE_NAME, "the-token"));

        // When & Then
        assertEquals("the-token", jwtService.getTokenFromCookie(request));
    }

    @Test
    void testGetTokenFromCookie_returnsNullWhenAbsent() {
        // Given
        MockHttpServletRequest request = new MockHttpServletRequest();

        // When & Then
        assertNull(jwtService.getTokenFromCookie(request));
    }

    @Test
    void testGetRefreshTokenFromCookie_returnsTheValueWhenPresent() {
        // Given
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(new Cookie(REFRESH_COOKIE_NAME, "the-refresh-token"));

        // When & Then
        assertEquals("the-refresh-token", jwtService.getRefreshTokenFromCookie(request));
    }

    @Test
    void testGetRefreshTokenFromCookie_returnsNullWhenADifferentCookieIsPresent() {
        // Given
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(new Cookie("some-other-cookie", "value"));

        // When & Then
        assertNull(jwtService.getRefreshTokenFromCookie(request));
    }

    // ------------------------------------------------------------------
    // Validation - one case per catch block in isTokenValid
    // ------------------------------------------------------------------

    @Test
    void testIsTokenValid_acceptsAFreshlyIssuedToken() {
        // Given
        String token = jwtService.generateTokenCookie(sampleUser()).getValue();

        // When & Then
        assertTrue(jwtService.isTokenValid(token));
        assertNotNull(jwtService.getEmailFromToken(token));
    }

    @Test
    void testIsTokenValid_rejectsAMalformedToken() {
        // When & Then
        assertFalse(jwtService.isTokenValid("this-is-not-a-jwt"));
    }

    @Test
    void testIsTokenValid_rejectsAnExpiredToken() {
        // Given - a negative lifetime yields a token that expired before it was issued
        ReflectionTestUtils.setField(jwtService, "expiration", -60000L);
        String expiredToken = jwtService.generateTokenCookie(sampleUser()).getValue();

        // When & Then
        assertFalse(jwtService.isTokenValid(expiredToken));
    }

    @Test
    void testIsTokenValid_rejectsAnUnsignedToken() {
        // Given - a JWT with "alg: none" cannot satisfy parseSignedClaims
        String unsignedToken = Jwts.builder().subject("ada@erd.com").compact();

        // When & Then
        assertFalse(jwtService.isTokenValid(unsignedToken));
    }

    @Test
    void testIsTokenValid_rejectsAnEmptyToken() {
        // When & Then
        assertFalse(jwtService.isTokenValid(""));
    }

}
