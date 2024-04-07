package com.erd.core.controller;

import com.erd.core.dto.request.SignupRequestDTO;
import com.erd.core.model.PasswordReset;
import com.erd.core.model.User;
import com.erd.core.repository.UserRepository;
import com.erd.core.service.MailService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @MockBean
    private MailService mailService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    public void testRegisterUser() throws Exception {
        SignupRequestDTO signupRequestDto = new SignupRequestDTO();
        signupRequestDto.setEmail("test@test.com");
        signupRequestDto.setPassword("P4$sword");

        doNothing().when(mailService).sendEmail(anyString(), anyMap(), anyString());

        mockMvc.perform(post("/api/user")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(signupRequestDto)))
                .andExpect(status().isOk());

        verify(mailService).sendEmail(anyString(), anyMap(), anyString());
    }

    @Test
    public void testRegisterUserWithInvalidPassword() throws Exception {
        SignupRequestDTO signupRequestDto = new SignupRequestDTO();
        signupRequestDto.setEmail("test@test.com");
        signupRequestDto.setPassword("password");

        mockMvc.perform(post("/api/user")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(signupRequestDto)))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void testResetPassword() throws Exception {
        User user = new User();
        user.setEmail("test@test.com");
        user.setPassword("P4$sword");
        var savedUser = userRepository.save(user);

        PasswordReset passwordReset = new PasswordReset();
        passwordReset.setToken("validToken");
        passwordReset.setExpiration(LocalDateTime.now().plusMinutes(5));
        passwordReset.setUser(savedUser);
        user.setPasswordReset(passwordReset);

        userRepository.save(user);

        mockMvc.perform(post("/api/user/reset-password")
                        .param("token", "validToken")
                        .param("newPassword", "newP4$sword"))
                .andExpect(status().isOk());
    }

    @Test
    public void testResetPasswordWithInvalidPassword() throws Exception {
        User user = new User();
        user.setEmail("test@test.com");
        user.setPassword("P4$sword");
        var savedUser = userRepository.save(user);

        PasswordReset passwordReset = new PasswordReset();
        passwordReset.setToken("validToken");
        passwordReset.setExpiration(LocalDateTime.now().plusMinutes(5));
        passwordReset.setUser(savedUser);
        user.setPasswordReset(passwordReset);

        userRepository.save(user);

        mockMvc.perform(post("/api/user/reset-password")
                        .param("token", "validToken")
                        .param("newPassword", "password"))
                .andExpect(status().isBadRequest());
    }

}
