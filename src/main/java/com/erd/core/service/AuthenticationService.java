package com.erd.core.service;

import com.erd.core.dto.LogoutDTO;
import com.erd.core.dto.RefreshTokenMessageDTO;
import com.erd.core.dto.request.AuthenticationRequestDTO;
import com.erd.core.dto.response.AuthenticationResponseDTO;
import com.erd.core.exception.RefreshTokenException;
import com.erd.core.model.RefreshToken;
import com.erd.core.model.User;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.Objects;

@Service
public class AuthenticationService {

    private static final Logger logger = LoggerFactory.getLogger(AuthenticationService.class);

    private final AuthenticationManager authenticationManager;
    private final RefreshTokenService refreshTokenService;
    private final JwtService jwtService;

    public AuthenticationService(AuthenticationManager authenticationManager, RefreshTokenService refreshTokenService, JwtService jwtService) {
        this.authenticationManager = authenticationManager;
        this.refreshTokenService = refreshTokenService;
        this.jwtService = jwtService;
    }

    public AuthenticationResponseDTO authenticate(AuthenticationRequestDTO authRequestDto) {
        logger.info("Authenticating user");
        var authentication = authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(authRequestDto.getEmail(), authRequestDto.getPassword()));
        SecurityContextHolder.getContext().setAuthentication(authentication);

        var userDetails = (User) authentication.getPrincipal();

        var tokenCookie = jwtService.generateTokenCookie(userDetails);
        var refreshToken = refreshTokenService.findOrCreate(userDetails.getId());
        var refreshTokenCookie = jwtService.generateRefreshTokenCookie(refreshToken.getToken());

        return new AuthenticationResponseDTO(tokenCookie.toString(), refreshTokenCookie.toString(), userDetails.getEmail(), userDetails.getFirstName() + " " + userDetails.getLastName());
    }

    public RefreshTokenMessageDTO refreshToken(HttpServletRequest request) {
        logger.info("Refreshing token");
        var refreshToken = jwtService.getRefreshTokenFromCookie(request);

        if (Objects.nonNull(refreshToken) && !refreshToken.isEmpty()) {
            return refreshTokenService.findByToken(refreshToken)
                    .map(refreshTokenService::verifyExpiration)
                    .map(RefreshToken::getUser)
                    .map(user -> {
                        var tokenCookie = jwtService.generateTokenCookie(user);
                        logger.info("Token refreshed successfully");
                        return new RefreshTokenMessageDTO(tokenCookie, "Token refreshed successfully");
                    })
                    .orElseThrow(() -> {
                        logger.error("Refresh token is not in database!");
                        return new RefreshTokenException(refreshToken, "Refresh token is not in database!");
                    });
        }

        logger.error("Refresh token is empty!");
        return new RefreshTokenMessageDTO(null, "Refresh token is empty!");
    }

    public LogoutDTO logout() {
        logger.info("Logging out user");
        var principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        if (!principal.toString().equals("anonymousUser")) {
            var userId = ((User) principal).getId();
            refreshTokenService.deleteByUser(userId);
        }

        var tokenCookie = jwtService.deleteTokenCookie();
        var refreshTokenCookie = jwtService.deleteRefreshTokenCookie();

        return new LogoutDTO(tokenCookie.toString(), refreshTokenCookie.toString());
    }

}
