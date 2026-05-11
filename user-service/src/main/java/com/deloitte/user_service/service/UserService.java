package com.deloitte.user_service.service;

import java.util.List;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.deloitte.user_service.dto.request.LoginRequestDTO;
import com.deloitte.user_service.dto.request.RegisterRequestDTO;
import com.deloitte.user_service.dto.response.UserResponseDTO;
import com.deloitte.user_service.entity.User;
import com.deloitte.user_service.exception.AuthException;
import com.deloitte.user_service.repository.UserRepository;
import com.deloitte.user_service.security.JwtService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserService {

    private static final Logger logger = LoggerFactory.getLogger(UserService.class);

    private final UserRepository userRepository;
    private final JwtService jwtService;

    public String register(RegisterRequestDTO request) {

        if (userRepository.findByUsername(request.getUsername()).isPresent()) {
            throw new AuthException("Username already exists");
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPassword(request.getPassword());
        user.setRole(request.getRole());
        userRepository.save(user);

        logger.info("User registered: {}", request.getUsername());

        return "User registered successfully";
    }

    public String login(LoginRequestDTO request) {
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new AuthException("User not found"));

        if (!user.getPassword().equals(request.getPassword())) {
            throw new AuthException("Invalid credentials");
        }

        logger.info("User logged in: {}", request.getUsername());
        return jwtService.generateToken(user.getUsername());
    }

    public List<UserResponseDTO> getAllUsers() {
        List<User> users = userRepository.findAll();
        return users.stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    private UserResponseDTO mapToDTO(User user) {
        return new UserResponseDTO(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getRole().name(),
                user.getCreatedAt());
    }

}
