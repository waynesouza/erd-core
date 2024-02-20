package com.erd.core.service;

import com.erd.core.dto.request.AuthenticationRequestDTO;
import com.erd.core.dto.response.AuthenticationResponseDTO;
import com.erd.core.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class AuthenticationService {

    private static final Logger logger = LoggerFactory.getLogger(AuthenticationService.class);

    private final JwtService jwtService;
    private final UserService userService;
    private final AuthenticationManager authenticationManager;

    public AuthenticationService(JwtService jwtService, UserService userService, AuthenticationManager authenticationManager) {
        this.jwtService = jwtService;
        this.userService = userService;
        this.authenticationManager = authenticationManager;
    }

    public AuthenticationResponseDTO authenticate(AuthenticationRequestDTO authRequestDto) {
        logger.info("Authenticating user");
        Authentication authentication = authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(authRequestDto.getEmail(), authRequestDto.getPassword()));
        if (authentication.isAuthenticated()) {
            String token = jwtService.generateToken(authRequestDto.getEmail());
            User user = userService.findByEmail(authRequestDto.getEmail());
            String fullName = user.getFirstName() + " " + user.getLastName();
            return new AuthenticationResponseDTO(token, authRequestDto.getEmail(), fullName);
        } else {
            throw new UsernameNotFoundException("User not found");
        }
    }

}
