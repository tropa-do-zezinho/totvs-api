package com.fiap.apitotvs.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fiap.apitotvs.dto.request.worker.InsightsDocumentRequest;
import com.fiap.apitotvs.dto.request.worker.WorkerInsightsEnvelopeRequest;
import com.fiap.apitotvs.dto.response.WorkerInsightPersistResponse;
import com.fiap.apitotvs.exception.GlobalExceptionHandler;
import com.fiap.apitotvs.exception.ResourceNotFoundException;
import com.fiap.apitotvs.service.WorkerInsightsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class WorkerInsightsControllerTest {

    @Mock
    private WorkerInsightsService workerInsightsService;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new WorkerInsightsController(workerInsightsService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void receiveInsightsRetorna200() throws Exception {
        when(workerInsightsService.receiveInsights(any()))
                .thenReturn(new WorkerInsightPersistResponse("req-1", "persistido", 2));

        mockMvc.perform(post("/api/v1/worker/insights")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sampleEnvelope())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.request_id").value("req-1"))
                .andExpect(jsonPath("$.status").value("persistido"))
                .andExpect(jsonPath("$.reunioes_recebidas").value(2));
    }

    @Test
    void receiveInsightsRetorna404QuandoNaoEncontrado() throws Exception {
        when(workerInsightsService.receiveInsights(any()))
                .thenThrow(new ResourceNotFoundException("MeetRegister", "requestId", "req-x"));

        WorkerInsightsEnvelopeRequest envelope = sampleEnvelope();
        envelope.setRequestId("req-x");

        mockMvc.perform(post("/api/v1/worker/insights")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(envelope)))
                .andExpect(status().isNotFound());
    }

    private WorkerInsightsEnvelopeRequest sampleEnvelope() {
        InsightsDocumentRequest insights = new InsightsDocumentRequest();
        insights.setSchemaVersion("1.1");
        insights.setGeradoEmUtc(Instant.parse("2026-09-13T14:00:00Z"));
        insights.setResumo(Map.of("status_pipeline", "concluido"));
        insights.setReunioes(List.of());

        WorkerInsightsEnvelopeRequest envelope = new WorkerInsightsEnvelopeRequest();
        envelope.setEventType("meeting_insights.completed");
        envelope.setSchemaVersion("1.0");
        envelope.setRequestId("req-1");
        envelope.setStatus("concluido");
        envelope.setSentAtUtc(Instant.parse("2026-09-13T14:00:00Z"));
        envelope.setInsights(insights);
        return envelope;
    }
}
