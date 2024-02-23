package com.erd.core.controller;

import com.erd.core.dto.request.AuthenticationRequestDTO;
import com.erd.core.service.AuthenticationService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
        return ResponseEntity.ok().header(HttpHeaders.SET_COOKIE, authenticationResponse.getToken()).body(authenticationResponse);
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout() {
        var token = authenticationService.logout();
        return ResponseEntity.ok().header(HttpHeaders.SET_COOKIE, token).body("Logged out successfully");
    }

    @PostMapping("/refresh-token")
    public ResponseEntity<?> refreshToken(HttpServletRequest request) {
        try {
            var refreshTokenMessageDto = authenticationService.refreshToken(request);
            return ResponseEntity.ok().header(HttpHeaders.SET_COOKIE, refreshTokenMessageDto.getToken().toString()).body(refreshTokenMessageDto.getMessage());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

}
