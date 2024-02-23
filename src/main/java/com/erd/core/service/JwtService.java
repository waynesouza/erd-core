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
    private Integer expiration;

    @Value("${erd.app.jwt.cookie-name}")
    private String cookieName;

    public String getTokenFromCookie(HttpServletRequest request) {
        var cookie = WebUtils.getCookie(request, cookieName);
        return Objects.nonNull(cookie) ? cookie.getValue() : null;
    }

    public ResponseCookie generateTokenCookie(User user) {
        var token = createToken(user.getEmail());
        return ResponseCookie.from(cookieName, token).httpOnly(true).maxAge(24 * 60 * 60).path("/api")
                .build();
    }

    public ResponseCookie deleteTokenCookie() {
        return ResponseCookie.from(cookieName, Strings.EMPTY).path("/api").build();
    }

    public String getEmailFromToken(String token) {
        return Jwts.parserBuilder().setSigningKey(getSigningKey()).build().parseClaimsJws(token).getBody().getSubject();
    }

    public Boolean isTokenValid(String token) {
        try {
            Jwts.parserBuilder().setSigningKey(getSigningKey()).build().parse(token);
            return Boolean.TRUE;
        } catch (MalformedJwtException | ExpiredJwtException | UnsupportedJwtException | IllegalArgumentException e) {
            logger.error("{} token: {}", e instanceof IllegalArgumentException ? "Empty" : e.getMessage().split(" ")[0], e.getMessage());
            return Boolean.FALSE;
        }
    }

    private String createToken(String email) {
        return Jwts.builder()
                .setSubject(email)
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    private Key getSigningKey() {
        return Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
    }

}
