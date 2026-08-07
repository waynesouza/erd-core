package com.erd.core.controller;

import com.erd.core.dto.LogoutDTO;
import com.erd.core.dto.RefreshTokenMessageDTO;
import com.erd.core.dto.request.AuthenticationRequestDTO;
import com.erd.core.dto.response.AuthenticationResponseDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseCookie;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AuthenticationControllerTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @Mock
    private com.erd.core.service.AuthenticationService authenticationService;

    @InjectMocks
    private AuthenticationController authenticationController;

    private ResponseCookie tokenCookie;
    private ResponseCookie refreshCookie;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(authenticationController).build();
        objectMapper = new ObjectMapper();
        tokenCookie = ResponseCookie.from("erd-token", "token-value").path("/api").build();
        refreshCookie = ResponseCookie.from("erd-refresh-token", "refresh-value")
                .path("/api/auth/refresh-token").build();
    }

    @Test
    void testLogin_returnsTheIdentityAndBothCookies() throws Exception {
        // Given
        AuthenticationResponseDTO response = new AuthenticationResponseDTO(
                tokenCookie.toString(), refreshCookie.toString(), "ada@erd.com", "Ada Lovelace");
        when(authenticationService.authenticate(any(AuthenticationRequestDTO.class))).thenReturn(response);

        // When & Then
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("email", "ada@erd.com", "password", "pwd"))))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.SET_COOKIE, tokenCookie.toString()))
                .andExpect(jsonPath("$.email").value("ada@erd.com"))
                .andExpect(jsonPath("$.fullName").value("Ada Lovelace"));
    }

    @Test
    void testLogout_returnsTheExpiringCookies() throws Exception {
        // Given
        when(authenticationService.logout())
                .thenReturn(new LogoutDTO(tokenCookie.toString(), refreshCookie.toString()));

        // When & Then
        mockMvc.perform(post("/api/auth/logout"))
                .andExpect(status().isOk())
                .andExpect(content().string("Logged out successfully"))
                .andExpect(header().string(HttpHeaders.SET_COOKIE, tokenCookie.toString()));

        verify(authenticationService).logout();
    }

    @Test
    void testRefreshToken_returnsTheNewCookieWhenTheTokenIsValid() throws Exception {
        // Given
        when(authenticationService.refreshToken(any()))
                .thenReturn(new RefreshTokenMessageDTO(tokenCookie, refreshCookie, "Token refreshed successfully"));

        // When & Then
        mockMvc.perform(post("/api/auth/refresh-token"))
                .andExpect(status().isOk())
                .andExpect(content().string("Token refreshed successfully"))
                .andExpect(header().string(HttpHeaders.SET_COOKIE, tokenCookie.toString()));
    }

    @Test
    void testRefreshToken_returnsBadRequestWhenNoTokenWasIssued() throws Exception {
        // Given
        when(authenticationService.refreshToken(any()))
                .thenReturn(new RefreshTokenMessageDTO(null, null, "Refresh token is empty!"));

        // When & Then
        mockMvc.perform(post("/api/auth/refresh-token"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Refresh token is empty!"));
    }

}
