package com.erd.core.service;

import com.erd.core.dto.LogoutDTO;
import com.erd.core.dto.RefreshTokenMessageDTO;
import com.erd.core.dto.request.AuthenticationRequestDTO;
import com.erd.core.dto.response.AuthenticationResponseDTO;
import com.erd.core.enumeration.RoleEnum;
import com.erd.core.exception.RefreshTokenException;
import com.erd.core.model.RefreshToken;
import com.erd.core.model.User;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseCookie;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthenticationServiceTest {

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private RefreshTokenService refreshTokenService;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private AuthenticationService authenticationService;

    private User user;
    private ResponseCookie tokenCookie;
    private ResponseCookie refreshCookie;

    @BeforeEach
    void setUp() {
        user = new User("Ada", "Lovelace", "ada@erd.com", "encoded", RoleEnum.USER);
        user.setId(UUID.randomUUID());
        tokenCookie = ResponseCookie.from("erd-token", "token-value").path("/api").build();
        refreshCookie = ResponseCookie.from("erd-refresh-token", "refresh-value").path("/api/auth/refresh-token").build();
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    private AuthenticationRequestDTO loginRequest() {
        AuthenticationRequestDTO dto = new AuthenticationRequestDTO();
        ReflectionTestUtils.setField(dto, "email", "ada@erd.com");
        ReflectionTestUtils.setField(dto, "password", "plain-text");
        return dto;
    }

    private RefreshToken refreshToken() {
        RefreshToken token = new RefreshToken();
        token.setUser(user);
        token.setToken("refresh-value");
        token.setExpiration(Instant.now().plusSeconds(3600));
        return token;
    }

    // ------------------------------------------------------------------
    // authenticate
    // ------------------------------------------------------------------

    @Test
    void testAuthenticate_returnsBothCookiesAndTheUserIdentity() {
        // Given
        Authentication authentication = new UsernamePasswordAuthenticationToken(user, null, List.of());
        when(authenticationManager.authenticate(any(Authentication.class))).thenReturn(authentication);
        when(jwtService.generateTokenCookie(user)).thenReturn(tokenCookie);
        when(refreshTokenService.findOrCreate(user.getId())).thenReturn(refreshToken());
        when(jwtService.generateRefreshTokenCookie("refresh-value")).thenReturn(refreshCookie);

        // When
        AuthenticationResponseDTO response = authenticationService.authenticate(loginRequest());

        // Then
        assertEquals(tokenCookie.toString(), response.getToken());
        assertEquals(refreshCookie.toString(), response.getRefreshToken());
        assertEquals("ada@erd.com", response.getEmail());
        assertEquals("Ada Lovelace", response.getFullName());
        assertNotNull(SecurityContextHolder.getContext().getAuthentication(),
                "A successful login must populate the security context");
    }

    @Test
    void testAuthenticate_propagatesBadCredentials() {
        // Given
        when(authenticationManager.authenticate(any(Authentication.class)))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        // When & Then
        assertThrows(BadCredentialsException.class, () -> authenticationService.authenticate(loginRequest()));
        verify(jwtService, never()).generateTokenCookie(any(User.class));
    }

    // ------------------------------------------------------------------
    // refreshToken
    // ------------------------------------------------------------------

    @Test
    void testRefreshToken_issuesNewCookiesForAValidToken() {
        // Given
        MockHttpServletRequest request = new MockHttpServletRequest();
        RefreshToken stored = refreshToken();
        when(jwtService.getRefreshTokenFromCookie(request)).thenReturn("refresh-value");
        when(refreshTokenService.findByToken("refresh-value")).thenReturn(Optional.of(stored));
        when(refreshTokenService.verifyExpiration(stored)).thenReturn(stored);
        when(jwtService.generateTokenCookie(user)).thenReturn(tokenCookie);
        when(jwtService.generateRefreshTokenCookie("refresh-value")).thenReturn(refreshCookie);

        // When
        RefreshTokenMessageDTO response = authenticationService.refreshToken(request);

        // Then
        assertEquals(tokenCookie, response.getToken());
        assertEquals(refreshCookie, response.getRefreshToken());
        assertEquals("Token refreshed successfully", response.getMessage());
    }

    @Test
    void testRefreshToken_throwsWhenTheTokenIsNotInTheDatabase() {
        // Given
        MockHttpServletRequest request = new MockHttpServletRequest();
        when(jwtService.getRefreshTokenFromCookie(request)).thenReturn("unknown-token");
        when(refreshTokenService.findByToken("unknown-token")).thenReturn(Optional.empty());

        // When
        RefreshTokenException exception =
                assertThrows(RefreshTokenException.class, () -> authenticationService.refreshToken(request));

        // Then
        assertTrue(exception.getMessage().contains("Refresh token is not in database!"));
    }

    @Test
    void testRefreshToken_returnsAnEmptyMessageWhenTheCookieIsMissing() {
        // Given
        MockHttpServletRequest request = new MockHttpServletRequest();
        when(jwtService.getRefreshTokenFromCookie(request)).thenReturn(null);

        // When
        RefreshTokenMessageDTO response = authenticationService.refreshToken(request);

        // Then
        assertNull(response.getToken());
        assertNull(response.getRefreshToken());
        assertEquals("Refresh token is empty!", response.getMessage());
    }

    @Test
    void testRefreshToken_returnsAnEmptyMessageWhenTheCookieIsBlank() {
        // Given
        MockHttpServletRequest request = new MockHttpServletRequest();
        when(jwtService.getRefreshTokenFromCookie(request)).thenReturn("");

        // When
        RefreshTokenMessageDTO response = authenticationService.refreshToken(request);

        // Then
        assertNull(response.getToken());
        assertEquals("Refresh token is empty!", response.getMessage());
    }

    // ------------------------------------------------------------------
    // logout
    // ------------------------------------------------------------------

    @Test
    void testLogout_deletesTheRefreshTokenOfAnAuthenticatedUser() {
        // Given
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user, null, List.of()));
        when(jwtService.deleteTokenCookie()).thenReturn(tokenCookie);
        when(jwtService.deleteRefreshTokenCookie()).thenReturn(refreshCookie);

        // When
        LogoutDTO response = authenticationService.logout();

        // Then
        verify(refreshTokenService).deleteByUser(user.getId());
        assertEquals(tokenCookie.toString(), response.getTokenCookie());
        assertEquals(refreshCookie.toString(), response.getRefreshTokenCookie());
    }

    @Test
    void testLogout_skipsTheRefreshTokenDeletionForAnAnonymousPrincipal() {
        // Given
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("anonymousUser", null, List.of()));
        when(jwtService.deleteTokenCookie()).thenReturn(tokenCookie);
        when(jwtService.deleteRefreshTokenCookie()).thenReturn(refreshCookie);

        // When
        LogoutDTO response = authenticationService.logout();

        // Then
        verify(refreshTokenService, never()).deleteByUser(any(UUID.class));
        assertNotNull(response.getTokenCookie());
    }

}
