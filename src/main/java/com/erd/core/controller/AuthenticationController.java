package com.erd.core.controller;

import com.erd.core.dto.request.AuthenticationRequestDTO;
import com.erd.core.service.AuthenticationService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Objects;

import static org.springframework.http.HttpHeaders.SET_COOKIE;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@CrossOrigin(origins = "http://localhost:8081", maxAge = 3600, allowCredentials = "true")
@RestController
@RequestMapping("/api/auth")
public class AuthenticationController {

    private final AuthenticationService authenticationService;

    public AuthenticationController(AuthenticationService authenticationService) {
        this.authenticationService = authenticationService;
    }

    @PostMapping(value = "/login", consumes = APPLICATION_JSON_VALUE, produces = APPLICATION_JSON_VALUE)
    public ResponseEntity<?> login(@RequestBody AuthenticationRequestDTO authRequestDto) {
        var authenticationResponse = authenticationService.authenticate(authRequestDto);
        return ResponseEntity.ok()
                .header(SET_COOKIE, authenticationResponse.getToken())
                .header(SET_COOKIE, authenticationResponse.getRefreshToken())
                .body(authenticationResponse);
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout() {
        var logoutDto = authenticationService.logout();
        return ResponseEntity.ok().header(SET_COOKIE, logoutDto.getTokenCookie()).header(SET_COOKIE, logoutDto.getRefreshTokenCookie()).body("Logged out successfully");
    }

    @PostMapping("/refresh-token")
    public ResponseEntity<?> refreshToken(HttpServletRequest request) {
        var refreshTokenMessageDto = authenticationService.refreshToken(request);
        if (Objects.isNull(refreshTokenMessageDto.getToken())) {
            return ResponseEntity.badRequest().body(refreshTokenMessageDto.getMessage());
        }
        return ResponseEntity.ok().header(SET_COOKIE, refreshTokenMessageDto.getToken().toString()).body(refreshTokenMessageDto.getMessage());
    }

}
