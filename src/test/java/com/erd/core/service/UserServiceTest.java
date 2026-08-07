package com.erd.core.service;

import com.erd.core.dto.request.SignupRequestDTO;
import com.erd.core.dto.response.UserResponseDTO;
import com.erd.core.enumeration.RoleEnum;
import com.erd.core.model.User;
import com.erd.core.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private ModelMapper modelMapper;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    private User user;

    @BeforeEach
    void setUp() {
        user = new User("Ada", "Lovelace", "ada@erd.com", "encoded", RoleEnum.USER);
        user.setId(UUID.randomUUID());
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    private SignupRequestDTO signupRequest(String email, String password) {
        SignupRequestDTO dto = new SignupRequestDTO();
        ReflectionTestUtils.setField(dto, "firstName", "Ada");
        ReflectionTestUtils.setField(dto, "lastName", "Lovelace");
        ReflectionTestUtils.setField(dto, "email", email);
        ReflectionTestUtils.setField(dto, "password", password);
        return dto;
    }

    @Test
    void testLoadUserByUsername_returnsTheStoredUser() {
        // Given
        when(userRepository.findByEmail("ada@erd.com")).thenReturn(Optional.of(user));

        // When
        UserDetails details = userService.loadUserByUsername("ada@erd.com");

        // Then
        assertSame(user, details);
    }

    @Test
    void testLoadUserByUsername_throwsWhenTheEmailIsUnknown() {
        // Given
        when(userRepository.findByEmail("ghost@erd.com")).thenReturn(Optional.empty());

        // When & Then
        UsernameNotFoundException exception = assertThrows(UsernameNotFoundException.class,
                () -> userService.loadUserByUsername("ghost@erd.com"));
        assertEquals("User not found", exception.getMessage());
    }

    @Test
    void testCreate_encodesThePasswordAndForcesTheUserRole() {
        // Given
        SignupRequestDTO request = signupRequest("new@erd.com", "plain-text");
        User mapped = new User();
        when(userRepository.existsByEmail("new@erd.com")).thenReturn(false);
        when(modelMapper.map(request, User.class)).thenReturn(mapped);
        when(passwordEncoder.encode("plain-text")).thenReturn("hashed");

        // When
        userService.create(request);

        // Then
        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertEquals("hashed", captor.getValue().getPassword());
        assertEquals(RoleEnum.USER, captor.getValue().getRole(),
                "Signup must never let the caller choose its own role");
    }

    @Test
    void testCreate_rejectsAnAlreadyRegisteredEmail() {
        // Given
        SignupRequestDTO request = signupRequest("taken@erd.com", "plain-text");
        when(userRepository.existsByEmail("taken@erd.com")).thenReturn(true);

        // When
        IllegalArgumentException exception =
                assertThrows(IllegalArgumentException.class, () -> userService.create(request));

        // Then
        assertTrue(exception.getMessage().contains("taken@erd.com"));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void testFindByEmail_returnsTheUser() {
        // Given
        when(userRepository.findByEmail("ada@erd.com")).thenReturn(Optional.of(user));

        // When & Then
        assertSame(user, userService.findByEmail("ada@erd.com"));
    }

    @Test
    void testFindByEmail_throwsWhenMissing() {
        // Given
        when(userRepository.findByEmail("ghost@erd.com")).thenReturn(Optional.empty());

        // When & Then
        assertThrows(UsernameNotFoundException.class, () -> userService.findByEmail("ghost@erd.com"));
    }

    @Test
    void testFindById_returnsTheUser() {
        // Given
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));

        // When & Then
        assertSame(user, userService.findById(user.getId()));
    }

    @Test
    void testFindById_throwsWhenMissing() {
        // Given
        UUID unknownId = UUID.randomUUID();
        when(userRepository.findById(unknownId)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(UsernameNotFoundException.class, () -> userService.findById(unknownId));
    }

    @Test
    void testGetUserIdByLoggedUserEmail_resolvesTheIdFromTheSecurityContext() {
        // Given
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user, null, List.of()));
        UserResponseDTO responseDto = new UserResponseDTO();
        responseDto.setId(user.getId());
        when(userRepository.findByEmail("ada@erd.com")).thenReturn(Optional.of(user));
        when(modelMapper.map(user, UserResponseDTO.class)).thenReturn(responseDto);

        // When & Then
        assertEquals(user.getId(), userService.getUserIdByLoggedUserEmail());
    }

    @Test
    void testGetUserIdByLoggedUserEmail_looksUpNullWhenThePrincipalIsNotAUser() {
        // Given - an anonymous principal is a String, not a User
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("anonymousUser", null, List.of()));
        UserResponseDTO responseDto = new UserResponseDTO();
        when(userRepository.findByEmail(null)).thenReturn(Optional.of(user));
        when(modelMapper.map(eq(user), eq(UserResponseDTO.class))).thenReturn(responseDto);

        // When & Then
        assertNull(userService.getUserIdByLoggedUserEmail());
    }

}
