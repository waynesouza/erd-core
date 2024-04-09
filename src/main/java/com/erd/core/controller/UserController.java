package com.erd.core.controller;

import com.erd.core.dto.request.ResetPasswordRequestDTO;
import com.erd.core.dto.request.SignupRequestDTO;
import com.erd.core.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@RestController
@RequestMapping("/api/user")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping(produces = APPLICATION_JSON_VALUE, consumes = APPLICATION_JSON_VALUE)
    public ResponseEntity<?> registerUser(@RequestBody SignupRequestDTO signupRequestDto) {
        try {
            userService.create(signupRequestDto);
            return ResponseEntity.ok("User created successfully");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping(value = "/forgot-password")
    public ResponseEntity<?> forgotPassword() {
        try {
            userService.forgotPassword();
            return ResponseEntity.ok("Password reset link sent to your email");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping(value = "/reset-password", consumes = APPLICATION_JSON_VALUE)
    public ResponseEntity<?> resetPassword(@RequestBody ResetPasswordRequestDTO resetPasswordRequestDto) {
        try {
            userService.resetPassword(resetPasswordRequestDto);
            return ResponseEntity.ok("Password reset successfully");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

}
