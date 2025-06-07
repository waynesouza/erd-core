package com.erd.core.service;

import com.erd.core.dto.request.SignupRequestDTO;
import com.erd.core.dto.response.UserResponseDTO;
import com.erd.core.enumeration.RoleEnum;
import com.erd.core.model.User;
import com.erd.core.repository.UserRepository;
import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class UserService implements UserDetailsService {

    private static final Logger logger = LoggerFactory.getLogger(UserService.class);

    private final UserRepository userRepository;
    private final ModelMapper modelMapper;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, ModelMapper modelMapper, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.modelMapper = modelMapper;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return userRepository.findByEmail(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
    }

    public void create(SignupRequestDTO signupRequestDto) {
        if (userRepository.existsByEmail(signupRequestDto.getEmail())) {
            throw new IllegalArgumentException("User with email " + signupRequestDto.getEmail() + " already exists");
        }

        var user = modelMapper.map(signupRequestDto, User.class);
        user.setPassword(passwordEncoder.encode(signupRequestDto.getPassword()));
        user.setRole(RoleEnum.USER); // Set default role for new users
        userRepository.save(user);
    }

    public User findByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
    }

    public User findById(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
    }

    public UUID getUserIdByLoggedUserEmail() {
        String email = getLoggedUserEmail();

        logger.info("Getting user id by email: {}", email);
        return modelMapper.map(findByEmail(email), UserResponseDTO.class).getId();
    }

    private String getLoggedUserEmail() {
        logger.info("Getting logged user email");
        var principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof User user) {
            return user.getEmail();
        }
        return null;
    }

}
