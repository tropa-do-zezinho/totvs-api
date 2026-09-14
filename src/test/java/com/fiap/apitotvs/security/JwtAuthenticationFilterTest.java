package com.fiap.apitotvs.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock
    private JwtUtil jwtUtil;
    @Mock
    private UserDetailsService userDetailsService;
    @Mock
    private HttpServletRequest request;
    @Mock
    private HttpServletResponse response;
    @Mock
    private FilterChain filterChain;

    @Test
    void shouldNotFilterParaRotasDoWorker() {
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtUtil, userDetailsService);
        when(request.getRequestURI()).thenReturn("/api/v1/worker/insights");

        boolean skip = ReflectionTestUtils.invokeMethod(filter, "shouldNotFilter", request);

        assertTrue(Boolean.TRUE.equals(skip));
    }

    @Test
    void shouldFilterParaRotasAutenticadas() {
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtUtil, userDetailsService);
        when(request.getRequestURI()).thenReturn("/api/blobs");

        boolean skip = ReflectionTestUtils.invokeMethod(filter, "shouldNotFilter", request);

        assertFalse(Boolean.TRUE.equals(skip));
    }

    @Test
    void doFilterContinuaSemHeaderAuthorization() throws Exception {
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtUtil, userDetailsService);
        when(request.getHeader("Authorization")).thenReturn(null);

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
    }
}
