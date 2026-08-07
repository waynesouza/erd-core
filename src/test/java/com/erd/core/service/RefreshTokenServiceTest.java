package com.erd.core.service;

import com.erd.core.exception.RefreshTokenException;
import com.erd.core.model.RefreshToken;
import com.erd.core.model.User;
import com.erd.core.repository.RefreshTokenRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

    private static final long REFRESH_EXPIRATION = 2629746000L;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private UserService userService;

    @InjectMocks
    private RefreshTokenService refreshTokenService;

    private UUID userId;
    private User user;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(refreshTokenService, "expiration", REFRESH_EXPIRATION);
        userId = UUID.randomUUID();
        user = new User();
        user.setId(userId);
        user.setEmail("ada@erd.com");
    }

    private RefreshToken tokenExpiringAt(Instant expiration) {
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setId(UUID.randomUUID());
        refreshToken.setUser(user);
        refreshToken.setToken(UUID.randomUUID().toString());
        refreshToken.setExpiration(expiration);
        return refreshToken;
    }

    @Test
    void testFindByToken_delegatesToTheRepository() {
        // Given
        RefreshToken stored = tokenExpiringAt(Instant.now().plusSeconds(60));
        when(refreshTokenRepository.findByToken("abc")).thenReturn(Optional.of(stored));

        // When
        Optional<RefreshToken> result = refreshTokenService.findByToken("abc");

        // Then
        assertTrue(result.isPresent());
        assertSame(stored, result.get());
    }

    @Test
    void testFindByToken_returnsEmptyWhenUnknown() {
        // Given
        when(refreshTokenRepository.findByToken("missing")).thenReturn(Optional.empty());

        // When & Then
        assertTrue(refreshTokenService.findByToken("missing").isEmpty());
    }

    @Test
    void testFindOrCreate_createsANewTokenWhenTheUserHasNone() {
        // Given
        when(refreshTokenRepository.findByUserId(userId)).thenReturn(null);
        when(userService.findById(userId)).thenReturn(user);
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        RefreshToken created = refreshTokenService.findOrCreate(userId);

        // Then
        ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenRepository).save(captor.capture());
        assertSame(user, captor.getValue().getUser());
        assertNotNull(created.getToken());
        assertTrue(created.getExpiration().isAfter(Instant.now()),
                "A newly created refresh token must be in the future");
    }

    @Test
    void testFindOrCreate_rotatesAnExpiredToken() {
        // Given
        RefreshToken expired = tokenExpiringAt(Instant.now().minusSeconds(60));
        String previousToken = expired.getToken();
        when(refreshTokenRepository.findByUserId(userId)).thenReturn(expired);
        when(refreshTokenRepository.save(expired)).thenReturn(expired);

        // When
        RefreshToken rotated = refreshTokenService.findOrCreate(userId);

        // Then
        assertSame(expired, rotated);
        assertTrue(rotated.getExpiration().isAfter(Instant.now()));
        assertTrue(!previousToken.equals(rotated.getToken()), "An expired token must be replaced by a new value");
        verify(userService, never()).findById(any(UUID.class));
    }

    @Test
    void testFindOrCreate_reusesAValidToken() {
        // Given
        RefreshToken valid = tokenExpiringAt(Instant.now().plusSeconds(3600));
        String originalToken = valid.getToken();
        when(refreshTokenRepository.findByUserId(userId)).thenReturn(valid);

        // When
        RefreshToken result = refreshTokenService.findOrCreate(userId);

        // Then
        assertSame(valid, result);
        assertEquals(originalToken, result.getToken());
        verify(refreshTokenRepository, never()).save(any(RefreshToken.class));
    }

    @Test
    void testVerifyExpiration_returnsTheTokenWhenStillValid() {
        // Given
        RefreshToken valid = tokenExpiringAt(Instant.now().plusSeconds(3600));

        // When
        RefreshToken result = refreshTokenService.verifyExpiration(valid);

        // Then
        assertSame(valid, result);
        verify(refreshTokenRepository, never()).delete(any(RefreshToken.class));
    }

    @Test
    void testVerifyExpiration_deletesAndThrowsWhenExpired() {
        // Given
        RefreshToken expired = tokenExpiringAt(Instant.now().minusSeconds(1));

        // When
        RefreshTokenException exception =
                assertThrows(RefreshTokenException.class, () -> refreshTokenService.verifyExpiration(expired));

        // Then
        assertTrue(exception.getMessage().contains("Refresh token was expired"));
        verify(refreshTokenRepository).delete(expired);
    }

    @Test
    void testDeleteByUser_resolvesTheUserBeforeDeleting() {
        // Given
        when(userService.findById(userId)).thenReturn(user);

        // When
        refreshTokenService.deleteByUser(userId);

        // Then
        verify(refreshTokenRepository).deleteByUser(user);
    }

}
