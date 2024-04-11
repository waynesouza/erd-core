package com.erd.core.service;

import com.erd.core.model.User;
import io.jsonwebtoken.Jwts;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseCookie;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class JwtServiceTest {

    @Mock
    private HttpServletRequest request;

    @Mock
    private Jwts jwts;

    @InjectMocks
    private JwtService jwtService;

    @BeforeEach
    public void setup() {
        ReflectionTestUtils.setField(jwtService, "secret", "1b4e92769871538c645e4145c964173046e015f55fb706a5c49b8f0c2c183daf");
        ReflectionTestUtils.setField(jwtService, "expiration", 3600000L);
        ReflectionTestUtils.setField(jwtService, "refreshExpiration", 3600000L);
        ReflectionTestUtils.setField(jwtService, "cookieName", "cookieName");
        ReflectionTestUtils.setField(jwtService, "refreshCookieName", "refreshCookieName");
    }

    @Test
    public void testGenerateTokenCookie() {
        User user = new User();
        user.setEmail("test@test.com");

        ResponseCookie responseCookie = jwtService.generateTokenCookie(user);

        assertNotNull(responseCookie);
        assertEquals("cookieName", responseCookie.getName());
    }

    @Test
    public void testGenerateRefreshTokenCookie() {
        ResponseCookie cookie = jwtService.generateRefreshTokenCookie("refreshToken");

        assertNotNull(cookie);
        assertEquals("refreshCookieName", cookie.getName());
    }

    @Test
    public void testGetTokenFromCookie() {
        when(request.getCookies()).thenReturn(new Cookie[] { new Cookie("cookieName", "token") });

        String token = jwtService.getTokenFromCookie(request);

        assertEquals("token", token);
    }

    @Test
    public void testGetRefreshTokenFromCookie() {
        when(request.getCookies()).thenReturn(new Cookie[] { new Cookie("refreshCookieName", "refreshToken") });

        String refreshToken = jwtService.getRefreshTokenFromCookie(request);

        assertEquals("refreshToken", refreshToken);
    }

    @Test
    public void testDeleteTokenCookie() {
        ResponseCookie cookie = jwtService.deleteTokenCookie();

        assertNotNull(cookie);
        assertEquals("cookieName", cookie.getName());
    }

    @Test
    public void testDeleteRefreshTokenCookie() {
        ResponseCookie cookie = jwtService.deleteRefreshTokenCookie();
        assertNotNull(cookie);
        assertEquals("refreshCookieName", cookie.getName());
    }

    @Test
    public void testGetEmailFromToken() {
        User user = new User();
        user.setEmail("test@test.com");

        ResponseCookie responseCookie = jwtService.generateTokenCookie(user);
        String token = responseCookie.getValue();
        String email = jwtService.getEmailFromToken(token);

        assertNotNull(responseCookie);
        assertEquals("cookieName", responseCookie.getName());
        assertEquals(user.getEmail(), email);
    }

    @Test
    public void testIsTokenValid() {
        User user = new User();
        user.setEmail("test@test.com");

        ResponseCookie responseCookie = jwtService.generateTokenCookie(user);
        String token = responseCookie.getValue();
        Boolean isValid = jwtService.isTokenValid(token);

        assertTrue(isValid);
    }

}
