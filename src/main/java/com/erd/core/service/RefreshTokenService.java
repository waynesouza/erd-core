package com.erd.core.service;

import com.erd.core.exception.RefreshTokenException;
import com.erd.core.model.RefreshToken;
import com.erd.core.repository.RefreshTokenRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Service
public class RefreshTokenService {

    @Value("${erd.app.jwt.refresh-expiration}")
    private Long expiration;

    private static final Logger logger = LoggerFactory.getLogger(RefreshTokenService.class);

    private final RefreshTokenRepository refreshTokenRepository;
    private final UserService userService;

    public RefreshTokenService(RefreshTokenRepository refreshTokenRepository, UserService userService) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.userService = userService;
    }

    public Optional<RefreshToken> findByToken(String token) {
        logger.info("Finding refresh token by token");
        return refreshTokenRepository.findByToken(token);
    }

    @Transactional
    public RefreshToken findOrCreate(UUID userId) {
        logger.info("Finding refresh token");
        var refreshTokenFound = refreshTokenRepository.findByUserId(userId);

        if (Objects.nonNull(refreshTokenFound) && isExpired(refreshTokenFound)) {
            logger.info("Deleting expired refresh token");
            refreshTokenRepository.delete(refreshTokenFound);
        }

        var refreshToken =  new RefreshToken();

        refreshToken.setUser(userService.findById(userId));
        refreshToken.setToken(UUID.randomUUID().toString());
        refreshToken.setExpiration(Instant.now().plusMillis(expiration));

        logger.info("Creating refresh token");
        return refreshTokenRepository.save(refreshToken);
    }

    public RefreshToken verifyExpiration(RefreshToken refreshToken) {
        logger.info("Verifying refresh token expiration");
        if (refreshToken.getExpiration().compareTo(Instant.now()) < 0) {
            refreshTokenRepository.delete(refreshToken);
            throw new RefreshTokenException(refreshToken.getToken(), "Refresh token was expired. Please make a new login request");
        }
        return refreshToken;
    }

    @Transactional
    public void deleteByUser(UUID userId) {
        logger.info("Deleting refresh token by user");
        refreshTokenRepository.deleteByUser(userService.findById(userId));
    }

    private Boolean isExpired(RefreshToken refreshToken) {
        return refreshToken.getExpiration().compareTo(Instant.now()) < 0;
    }

}
