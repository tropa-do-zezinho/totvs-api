package com.fiap.apitotvs.service;

import com.fiap.apitotvs.dto.request.LoginRequest;
import com.fiap.apitotvs.dto.request.SignUpRequest;
import com.fiap.apitotvs.dto.response.AuthResponse;
import com.fiap.apitotvs.entity.User;
import com.fiap.apitotvs.exception.BusinessException;
import com.fiap.apitotvs.repository.UserRepository;
import com.fiap.apitotvs.security.JwtUtil;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.security.authentication.AuthenticationManager;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;

    public AuthResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        UserDetails userDetails = (UserDetails) authentication.getPrincipal();

        String email = userDetails.getUsername();

        var userOpt = userRepository.findByEmail(email);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            Map<String, Object> extraClaims = new HashMap<>();
            extraClaims.put("userId", user.getId());
            extraClaims.put("email", user.getEmail());
            extraClaims.put("name", user.getName());

            String token = jwtUtil.generateToken(userDetails, extraClaims);
            return new AuthResponse(token, String.valueOf(user.getId()), user.getName(), user.getEmail());
        }

        throw new BusinessException("User not found");
    }

    public AuthResponse register(SignUpRequest request) {
        validateUniqueCredentials(request.getName(), request.getEmail());

        User user = new User();
        user.setEmail(request.getEmail());
        user.setName(request.getName());
        user.setPassword(passwordEncoder.encode(request.getPassword()));

        user = userRepository.save(user);

        Map<String, Object> extraClaims = new HashMap<>();
        extraClaims.put("userId", user.getId());
        extraClaims.put("email", user.getEmail());
        extraClaims.put("name", user.getName());

        String token = jwtUtil.generateToken(user, extraClaims);
        return new AuthResponse(token, String.valueOf(user.getId()), user.getName(), user.getEmail());
    }

    private void validateUniqueCredentials(String username, String email) {
        // Check username uniqueness across all entity types
        if (userRepository.findByNameContainingIgnoreCase(username).isPresent()) {
            throw new BusinessException("Username '" + username + "' already exists");
        }

        // Check email uniqueness across all entity types
        if (userRepository.findByEmail(email).isPresent()) {
            throw new BusinessException("Email '" + email + "' already exists");
        }
    }
}
