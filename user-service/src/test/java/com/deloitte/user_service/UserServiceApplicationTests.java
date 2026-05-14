package com.deloitte.user_service;

import com.deloitte.user_service.dto.request.RegisterRequestDTO;
import com.deloitte.user_service.entity.Role;
import com.deloitte.user_service.entity.User;
import com.deloitte.user_service.exception.AuthException;
import com.deloitte.user_service.repository.UserRepository;
import com.deloitte.user_service.security.JwtService;
import com.deloitte.user_service.service.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for UserService.
 *
 * @ExtendWith(MockitoExtension.class) — tells JUnit to use Mockito
 * @Mock — creates a fake version of the dependency
 * @InjectMocks — creates the real UserService and injects the mocks into it
 *
 * WHY mocks: we're testing UserService logic only.
 * We don't want to hit a real database in unit tests.
 * Mocks simulate the database behavior we define.
 */
@ExtendWith(MockitoExtension.class)
public class UserServiceApplicationTests {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private UserService userService;

    @Test
    void register_success() {
        // ARRANGE — set up the scenario
        RegisterRequestDTO request = new RegisterRequestDTO();
        request.setUsername("madhes");
        request.setEmail("madhes@test.com");
        request.setPassword("password123");
        request.setRole(Role.CUSTOMER);

        // fake: no existing user found
        when(userRepository.findByUsername("madhes")).thenReturn(Optional.empty());
        // fake: BCrypt returns this hash
        when(passwordEncoder.encode("password123")).thenReturn("hashedpassword");
        // fake: save returns the user object
        when(userRepository.save(any(User.class))).thenReturn(new User());

        // ACT — call the method
        String result = userService.register(request);

        // ASSERT — verify the outcome
        assertEquals("User registered successfully", result);
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    void register_throws_when_username_exists() {
        // ARRANGE
        RegisterRequestDTO request = new RegisterRequestDTO();
        request.setUsername("madhes");
        request.setEmail("madhes@test.com");
        request.setPassword("password123");
        request.setRole(Role.CUSTOMER);

        // fake: user already exists
        when(userRepository.findByUsername("madhes"))
                .thenReturn(Optional.of(new User()));

        // ACT + ASSERT — expect AuthException to be thrown
        assertThrows(AuthException.class, () -> userService.register(request));

        // verify save was never called
        verify(userRepository, never()).save(any(User.class));
    }
}