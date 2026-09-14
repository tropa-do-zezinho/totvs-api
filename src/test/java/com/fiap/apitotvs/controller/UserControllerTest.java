package com.fiap.apitotvs.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fiap.apitotvs.dto.request.LoginRequest;
import com.fiap.apitotvs.dto.request.SignUpRequest;
import com.fiap.apitotvs.dto.response.AuthResponse;
import com.fiap.apitotvs.exception.BusinessException;
import com.fiap.apitotvs.exception.GlobalExceptionHandler;
import com.fiap.apitotvs.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    @Mock
    private UserService userService;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new UserController(userService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void registerRetorna201() throws Exception {
        when(userService.register(any(SignUpRequest.class)))
                .thenReturn(new AuthResponse("token", "1", "Maria", "maria@totvs.com"));

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new SignUpRequest("Maria", "maria@totvs.com", "senha123"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").value("token"))
                .andExpect(jsonPath("$.email").value("maria@totvs.com"));
    }

    @Test
    void loginRetorna200() throws Exception {
        when(userService.login(any(LoginRequest.class)))
                .thenReturn(new AuthResponse("token", "1", "Maria", "maria@totvs.com"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new LoginRequest("maria@totvs.com", "senha123"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("token"));
    }

    @Test
    void registerPropagaBusinessExceptionComo400() throws Exception {
        when(userService.register(any(SignUpRequest.class)))
                .thenThrow(new BusinessException("Email already exists"));

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new SignUpRequest("Maria", "maria@totvs.com", "senha123"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Email already exists"));
    }
}
