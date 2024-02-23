package com.erd.core.service;

import com.erd.core.dto.request.AuthenticationRequestDTO;
import com.erd.core.dto.response.AuthenticationResponseDTO;
import com.erd.core.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class AuthenticationService {

    private static final Logger logger = LoggerFactory.getLogger(AuthenticationService.class);

    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    public AuthenticationService(JwtService jwtService, AuthenticationManager authenticationManager) {
        this.jwtService = jwtService;
        this.authenticationManager = authenticationManager;
    }

    public AuthenticationResponseDTO authenticate(AuthenticationRequestDTO authRequestDto) {
        logger.info("Authenticating user");
        var authentication = authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(authRequestDto.getEmail(), authRequestDto.getPassword()));
        SecurityContextHolder.getContext().setAuthentication(authentication);

        var userDetails = (User) authentication.getPrincipal();

        var tokenCookie = jwtService.generateTokenCookie(userDetails);

        return new AuthenticationResponseDTO(tokenCookie.toString(), userDetails.getEmail(), userDetails.getFirstName() + " " + userDetails.getLastName());
    }

    public String logout() {
        return jwtService.deleteTokenCookie().toString();
    }

}
