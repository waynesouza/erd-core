package com.erd.core.service;

import com.erd.core.dto.request.SignupRequestDTO;
import com.erd.core.model.PasswordReset;
import com.erd.core.model.User;
import com.erd.core.repository.PasswordResetRepository;
import com.erd.core.repository.UserRepository;
import org.modelmapper.ModelMapper;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static com.erd.core.util.ConstantUtil.PASSWORD_PATTERN;
import static com.erd.core.util.ConstantUtil.TEMPLATE_RESET_PASSWORD;
import static com.erd.core.util.ConstantUtil.TEMPLATE_WELCOME;

@Service
public class UserService implements UserDetailsService {

    private final UserRepository userRepository;
    private final PasswordResetRepository passwordResetRepository;
    private final ModelMapper modelMapper;
    private final PasswordEncoder passwordEncoder;
    private final MailService mailService;

    public UserService(UserRepository userRepository, PasswordResetRepository passwordResetRepository, ModelMapper modelMapper, PasswordEncoder passwordEncoder, MailService mailService) {
        this.userRepository = userRepository;
        this.passwordResetRepository = passwordResetRepository;
        this.modelMapper = modelMapper;
        this.passwordEncoder = passwordEncoder;
        this.mailService = mailService;
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

        checkPassword(signupRequestDto.getPassword());

        var user = modelMapper.map(signupRequestDto, User.class);
        user.setPassword(passwordEncoder.encode(signupRequestDto.getPassword()));
        userRepository.save(user);
        mailService.sendEmail(signupRequestDto.getEmail(), buildVariables(signupRequestDto), TEMPLATE_WELCOME);
    }

    public User findByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
    }

    public User findById(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
    }

    public void forgotPassword(String email) {
        var user = findByEmail(email);
        var token = UUID.randomUUID().toString();
        user.setPasswordReset(new PasswordReset(token));

        Map<String, String> variables = new HashMap<>();
        variables.put("firstName", user.getFirstName());
        variables.put("lastName", user.getLastName());
        variables.put("token", token);

        userRepository.save(user);
        mailService.sendEmail(email, variables, TEMPLATE_RESET_PASSWORD);
    }

    public void resetPassword(String token, String newPassword) {
        var user = userRepository.findByPasswordResetToken(token, LocalDateTime.now())
                .orElseThrow(() -> new IllegalArgumentException("Invalid or expired token"));

        checkPassword(newPassword);

        user.setPassword(passwordEncoder.encode(newPassword));
        passwordResetRepository.delete(user.getPasswordReset());
        userRepository.save(user);
    }

    private void checkPassword(String password) {
        if (!PASSWORD_PATTERN.matcher(password).matches()) {
            throw new IllegalArgumentException("Password must contain at least 8 characters, one uppercase letter, one lowercase letter, one number and one special character");
        }
    }

    private Map<String, String> buildVariables(SignupRequestDTO requestDto) {
        var variables = new HashMap<String, String>();
        variables.put("firstName", requestDto.getFirstName());
        variables.put("lastName", requestDto.getLastName());

        return variables;
    }

}
