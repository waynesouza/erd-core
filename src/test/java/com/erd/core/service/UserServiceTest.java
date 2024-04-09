package com.erd.core.service;

import com.erd.core.dto.request.ResetPasswordRequestDTO;
import com.erd.core.dto.request.SignupRequestDTO;
import com.erd.core.model.PasswordReset;
import com.erd.core.model.User;
import com.erd.core.repository.PasswordResetRepository;
import com.erd.core.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordResetRepository passwordResetRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private ModelMapper modelMapper;

    @Mock
    private MailService mailService;

    @InjectMocks
    private UserService userService;

    @Test
    public void testLoadUserByUsername() {
        User user = new User();
        user.setEmail("test@test.com");

        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(user));

        UserDetails userDetails = userService.loadUserByUsername("test@test.com");

        assertEquals(user.getEmail(), userDetails.getUsername());
    }

    @Test
    public void testLoadUserByUsernameNotFound() {
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());

        assertThrows(UsernameNotFoundException.class, () -> userService.loadUserByUsername("test@test.com"));
    }

    @Test
    public void testCreate() {
        SignupRequestDTO signupRequestDto = new SignupRequestDTO();
        signupRequestDto.setEmail("test@test.com");
        signupRequestDto.setPassword("P4$sword");

        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
        when(modelMapper.map(any(SignupRequestDTO.class), eq(User.class))).thenReturn(new User());

        userService.create(signupRequestDto);

        verify(userRepository).save(any(User.class));
        verify(mailService).sendEmail(anyString(), anyMap(), anyString());
    }

    @Test
    public void testCreateWithInvalidPassword() {
        SignupRequestDTO signupRequestDto = new SignupRequestDTO();
        signupRequestDto.setEmail("test@test.com");
        signupRequestDto.setPassword("invalid");

        when(userRepository.existsByEmail(anyString())).thenReturn(false);

        assertThrows(IllegalArgumentException.class, () -> userService.create(signupRequestDto));
    }

    @Test
    public void testCreateWithDuplicatedEmail() {
        SignupRequestDTO signupRequestDto = new SignupRequestDTO();
        signupRequestDto.setEmail("test@test.com");
        signupRequestDto.setPassword("P4$sword");

        when(userRepository.existsByEmail(anyString())).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> userService.create(signupRequestDto));
    }

    @Test
    public void testFindByEmail() {
        User user = new User();
        user.setEmail("test@test.com");

        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(user));

        User result = userService.findByEmail("test@test.com");

        assertEquals(user.getEmail(), result.getEmail());
    }

    @Test
    public void testFindByEmailNotFound() {
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());

        assertThrows(UsernameNotFoundException.class, () -> userService.findByEmail("test@test.com"));
    }

    @Test
    public void testFindById() {
        User user = new User();
        UUID id = UUID.randomUUID();
        user.setId(id);

        when(userRepository.findById(any(UUID.class))).thenReturn(Optional.of(user));

        User result = userService.findById(id);

        assertEquals(user.getId(), result.getId());
    }

    @Test
    public void testFindByIdNotFound() {
        when(userRepository.findById(any(UUID.class))).thenReturn(Optional.empty());

        assertThrows(UsernameNotFoundException.class, () -> userService.findById(UUID.randomUUID()));
    }

    @Test
    public void testForgotPassword() {
        User user = new User();
        user.setEmail("test@test.com");

        Authentication auth = new UsernamePasswordAuthenticationToken(user.getEmail(), null);
        SecurityContextHolder.getContext().setAuthentication(auth);

        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(user));

        userService.forgotPassword();

        verify(userRepository).save(any(User.class));
        verify(mailService).sendEmail(anyString(), anyMap(), anyString());
    }

    @Test
    public void testResetPassword() {
        User user = new User();
        user.setEmail("test@test.com");
        user.setPasswordReset(new PasswordReset("token"));

        when(userRepository.findByPasswordResetToken(anyString(), any(LocalDateTime.class))).thenReturn(Optional.of(user));
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");

        userService.resetPassword(new ResetPasswordRequestDTO("token", "newP4$sword"));

        verify(passwordResetRepository).delete(any(PasswordReset.class));
        verify(userRepository).save(any(User.class));
    }

    @Test
    public void testResetPasswordWithInvalidPassword() {
        User user = new User();
        user.setEmail("test@test.com");
        user.setPasswordReset(new PasswordReset("token"));

        when(userRepository.findByPasswordResetToken(anyString(), any(LocalDateTime.class))).thenReturn(Optional.of(user));

        assertThrows(IllegalArgumentException.class, () -> userService.resetPassword(new ResetPasswordRequestDTO("token", "invalid")));
    }

    @Test
    public void testResetPasswordInvalidToken() {
        when(userRepository.findByPasswordResetToken(anyString(), any(LocalDateTime.class))).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> userService.resetPassword(new ResetPasswordRequestDTO("invalidToken", "newPassword")));
    }

}
