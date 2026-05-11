package com.deloitte.user_service.security;

import com.deloitte.user_service.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Standalone UserDetailsService bean.
 *
 * WHY separate class and not inside SecurityConfig:
 * SecurityConfig needs JwtAuthFilter.
 * JwtAuthFilter needs UserDetailsService.
 * If UserDetailsService lives inside SecurityConfig, Spring sees a circular dependency
 * and crashes at startup.
 *
 * By moving it here as its own @Service, Spring can create it independently,
 * inject it into JwtAuthFilter, then inject JwtAuthFilter into SecurityConfig.
 * Linear dependency chain — no circle.
 */
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        var user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

        return org.springframework.security.core.userdetails.User
                .withUsername(user.getUsername())
                .password(user.getPassword())
                .authorities(List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name())))
                .build();
    }
}