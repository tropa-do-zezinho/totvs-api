package com.fiap.apitotvs.controller;

import com.fiap.apitotvs.dto.response.MeetRegisterResponse;
import com.fiap.apitotvs.dto.response.WorkerInsightDetailResponse;
import com.fiap.apitotvs.dto.response.WorkerInsightSummaryResponse;
import com.fiap.apitotvs.entity.User;
import com.fiap.apitotvs.enums.MeetRequestStatus;
import com.fiap.apitotvs.exception.GlobalExceptionHandler;
import com.fiap.apitotvs.service.BlobService;
import com.fiap.apitotvs.service.WorkerInsightsService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AuthenticatedControllersTest {

    @Mock
    private BlobService blobService;
    @Mock
    private WorkerInsightsService workerInsightsService;

    private MockMvc blobMockMvc;
    private MockMvc insightsMockMvc;
    private User user;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);
        user.setEmail("user@totvs.com");
        user.setName("User");
        user.setPassword("x");
        user.setIsActive(true);

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities())
        );

        blobMockMvc = MockMvcBuilders.standaloneSetup(new BlobController(blobService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .build();
        insightsMockMvc = MockMvcBuilders.standaloneSetup(new InsightsController(workerInsightsService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .build();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void uploadBlobRetorna201() throws Exception {
        MeetRegisterResponse response = new MeetRegisterResponse(
                1L, "req-1", "a.json", "http://blob", MeetRequestStatus.ANALISANDO,
                null, Instant.now(), Instant.now());
        when(blobService.upload(any(), eq(user))).thenReturn(response);

        MockMultipartFile file = new MockMultipartFile(
                "file", "a.json", MediaType.APPLICATION_JSON_VALUE, "{}".getBytes());

        blobMockMvc.perform(multipart("/api/blobs").file(file))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.requestId").value("req-1"))
                .andExpect(jsonPath("$.status").value("analisando"));
    }

    @Test
    void listBlobsRetorna200() throws Exception {
        when(blobService.listByUser(user)).thenReturn(List.of());

        blobMockMvc.perform(get("/api/blobs"))
                .andExpect(status().isOk());
    }

    @Test
    void listInsightsRetorna200() throws Exception {
        when(workerInsightsService.listByUser(user)).thenReturn(List.of(
                WorkerInsightSummaryResponse.builder().requestId("req-1").build()
        ));

        insightsMockMvc.perform(get("/api/insights"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].requestId").value("req-1"));
    }

    @Test
    void getInsightByRequestIdRetorna200() throws Exception {
        when(workerInsightsService.getByRequestId("req-1", user))
                .thenReturn(WorkerInsightDetailResponse.builder().requestId("req-1").build());

        insightsMockMvc.perform(get("/api/insights/req-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requestId").value("req-1"));
    }
}
