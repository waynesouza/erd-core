package com.erd.core.controller;

import com.erd.core.dto.request.SignupRequestDTO;
import com.erd.core.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @Mock
    private UserService userService;

    @InjectMocks
    private UserController userController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(userController).build();
        objectMapper = new ObjectMapper();
    }

    private String signupPayload() throws Exception {
        return objectMapper.writeValueAsString(Map.of(
                "firstName", "Ada",
                "lastName", "Lovelace",
                "email", "ada@erd.com",
                "password", "plain-text"));
    }

    @Test
    void testRegisterUser_returnsOkOnSuccess() throws Exception {
        // When & Then
        mockMvc.perform(post("/api/user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(signupPayload()))
                .andExpect(status().isOk())
                .andExpect(content().string("User created successfully"));

        verify(userService).create(any(SignupRequestDTO.class));
    }

    @Test
    void testRegisterUser_returnsBadRequestWithTheFailureMessage() throws Exception {
        // Given
        doThrow(new IllegalArgumentException("User with email ada@erd.com already exists"))
                .when(userService).create(any(SignupRequestDTO.class));

        // When & Then
        mockMvc.perform(post("/api/user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(signupPayload()))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("User with email ada@erd.com already exists"));
    }

}
