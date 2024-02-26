package com.erd.core.service;

import com.erd.core.model.User;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.logging.log4j.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;
import org.springframework.web.util.WebUtils;

import java.security.Key;
import java.util.Date;
import java.util.Objects;

@Service
public class JwtService {

    private static final Logger logger = LoggerFactory.getLogger(JwtService.class);

    @Value("${erd.app.jwt.secret}")
    private String secret;

    @Value("${erd.app.jwt.expiration}")
    private Long expiration;

    @Value("${erd.app.jwt.refresh-expiration}")
    private Long refreshExpiration;

    @Value("${erd.app.jwt.cookie-name}")
    private String cookieName;

    @Value("${erd.app.jwt.refresh-cookie-name}")
    private String refreshCookieName;

    public ResponseCookie generateTokenCookie(User user) {
        var token = createTokenFromEmail(user.getEmail());
        return generateCookie(cookieName, token, "/api");
    }

    public ResponseCookie generateRefreshTokenCookie(String refreshToken) {
        return generateCookie(refreshCookieName, refreshToken, "/api/auth/refresh-token");
    }

    public String getTokenFromCookie(HttpServletRequest request) {
        return getCookieValueByName(request, cookieName);
    }

    public String getRefreshTokenFromCookie(HttpServletRequest request) {
        return getCookieValueByName(request, refreshCookieName);
    }

    public ResponseCookie deleteTokenCookie() {
        return ResponseCookie.from(cookieName).path("/api").build();
    }

    public ResponseCookie deleteRefreshTokenCookie() {
        return ResponseCookie.from(refreshCookieName).path("/api/auth/refresh-token").build();
    }

    public String getEmailFromToken(String token) {
        return Jwts.parserBuilder().setSigningKey(getKey()).build().parseClaimsJws(token).getBody().getSubject();
    }

    public Boolean isTokenValid(String token) {
        try {
            Jwts.parserBuilder().setSigningKey(getKey()).build().parse(token);
            return Boolean.TRUE;
        } catch (MalformedJwtException e) {
            logger.error("Invalid JWT token: {}", e.getMessage());
        } catch (ExpiredJwtException e) {
            logger.error("JWT token is expired: {}", e.getMessage());
        } catch (UnsupportedJwtException e) {
            logger.error("JWT token is unsupported: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            logger.error("JWT claims string is empty: {}", e.getMessage());
        }

        return Boolean.FALSE;
    }

    private String createTokenFromEmail(String email) {
        return Jwts.builder()
                .setSubject(email)
                .setIssuedAt(new Date())
                .setExpiration(new Date((new Date()).getTime() + expiration))
                .signWith(getKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    private ResponseCookie generateCookie(String name, String value, String path) {
        return ResponseCookie.from(name, value).httpOnly(true).maxAge(24 * 60 * 60).path(path).build();
    }

    private String getCookieValueByName(HttpServletRequest request, String name) {
        var cookie = WebUtils.getCookie(request, name);
        return Objects.nonNull(cookie) ? cookie.getValue() : null;
    }

    private Key getKey() {
        return Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
    }

}
