package com.fiap.apitotvs.service;

import com.fiap.apitotvs.dto.request.LoginRequest;
import com.fiap.apitotvs.dto.request.SignUpRequest;
import com.fiap.apitotvs.dto.response.AuthResponse;
import com.fiap.apitotvs.entity.User;
import com.fiap.apitotvs.exception.BusinessException;
import com.fiap.apitotvs.repository.UserRepository;
import com.fiap.apitotvs.security.JwtUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtUtil jwtUtil;
    @Mock
    private AuthenticationManager authenticationManager;

    @InjectMocks
    private UserService userService;

    @Test
    void registerCriaUsuarioERetornaToken() {
        SignUpRequest request = new SignUpRequest("Maria", "maria@totvs.com", "senha123");
        when(userRepository.findByNameContainingIgnoreCase("Maria")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("maria@totvs.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("senha123")).thenReturn("hash");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User saved = invocation.getArgument(0);
            saved.setId(1L);
            return saved;
        });
        when(jwtUtil.generateToken(any(User.class), anyMap())).thenReturn("jwt-token");

        AuthResponse response = userService.register(request);

        assertEquals("jwt-token", response.getToken());
        assertEquals("1", response.getUserId());
        assertEquals("Maria", response.getUsername());
        assertEquals("maria@totvs.com", response.getEmail());

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertEquals("hash", captor.getValue().getPassword());
    }

    @Test
    void registerFalhaSeEmailJaExiste() {
        SignUpRequest request = new SignUpRequest("Maria", "maria@totvs.com", "senha123");
        when(userRepository.findByNameContainingIgnoreCase("Maria")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("maria@totvs.com")).thenReturn(Optional.of(new User()));

        assertThrows(BusinessException.class, () -> userService.register(request));
    }

    @Test
    void loginRetornaTokenQuandoCredenciaisValidas() {
        User user = new User();
        user.setId(7L);
        user.setName("Maria");
        user.setEmail("maria@totvs.com");

        Authentication authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(user);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(userRepository.findByEmail("maria@totvs.com")).thenReturn(Optional.of(user));
        when(jwtUtil.generateToken(eq(user), anyMap())).thenReturn("jwt-login");

        AuthResponse response = userService.login(new LoginRequest("maria@totvs.com", "senha123"));

        assertEquals("jwt-login", response.getToken());
        assertEquals("7", response.getUserId());
    }
}
