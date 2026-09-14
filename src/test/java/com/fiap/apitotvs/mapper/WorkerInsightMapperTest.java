package com.fiap.apitotvs.mapper;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fiap.apitotvs.dto.request.worker.InsightsDocumentRequest;
import com.fiap.apitotvs.dto.request.worker.MeetingInsightRequest;
import com.fiap.apitotvs.dto.request.worker.WorkerInsightsEnvelopeRequest;
import com.fiap.apitotvs.dto.response.WorkerInsightDetailResponse;
import com.fiap.apitotvs.dto.response.WorkerInsightSummaryResponse;
import com.fiap.apitotvs.entity.MeetingInsight;
import com.fiap.apitotvs.entity.MeetRegister;
import com.fiap.apitotvs.entity.WorkerInsightResult;
import com.fiap.apitotvs.enums.MeetRequestStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class WorkerInsightMapperTest {

    private WorkerInsightMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new WorkerInsightMapper(new ObjectMapper());
    }

    @Test
    void toMeetingEntityCopiaCamposEPayload() {
        MeetingInsightRequest request = new MeetingInsightRequest();
        request.setIdMeeting(123L);
        request.setStatusAnalise("concluida_llm_rag");
        request.setModelo("mock");
        request.setVersaoPrompt("1.0");
        request.setMetadata(Map.of("titulo", "Reunião"));

        MeetingInsight entity = mapper.toMeetingEntity(request, "req-1");

        assertEquals("req-1", entity.getRequestId());
        assertEquals(123L, entity.getIdMeeting());
        assertEquals("concluida_llm_rag", entity.getStatusAnalise());
        assertEquals("mock", entity.getModelo());
        assertNotNull(entity.getPayloadJson());
        assertEquals(123, ((Number) entity.getPayloadJson().get("id_meeting")).intValue());
    }

    @Test
    void applyEnvelopePreencheResultado() {
        WorkerInsightResult result = new WorkerInsightResult();
        InsightsDocumentRequest insights = new InsightsDocumentRequest();
        insights.setSchemaVersion("1.1");
        insights.setGeradoEmUtc(Instant.parse("2026-09-13T14:00:00Z"));
        insights.setResumo(Map.of("status_pipeline", "concluido"));

        WorkerInsightsEnvelopeRequest envelope = new WorkerInsightsEnvelopeRequest();
        envelope.setRequestId("req-1");
        envelope.setEventType("meeting_insights.completed");
        envelope.setSchemaVersion("1.0");
        envelope.setStatus("concluido");
        envelope.setSentAtUtc(Instant.parse("2026-09-13T14:01:00Z"));
        envelope.setInsights(insights);

        mapper.applyEnvelope(result, envelope);

        assertEquals("req-1", result.getRequestId());
        assertEquals("concluido", result.getWorkerStatus());
        assertEquals("1.1", result.getInsightsSchemaVersion());
        assertEquals("concluido", result.getResumoJson().get("status_pipeline"));
    }

    @Test
    void toSummaryEToDetailUsamMeetRegister() {
        MeetRegister meet = new MeetRegister();
        meet.setStatus(MeetRequestStatus.PROCESSADO);
        meet.setFileName("a.json");
        meet.setErrorMessage(null);

        WorkerInsightResult result = new WorkerInsightResult();
        result.setMeetRegister(meet);
        result.setRequestId("req-1");
        result.setEventType("meeting_insights.completed");
        result.setWorkerStatus("concluido");
        result.setReunioesRecebidas(1);
        result.setResumoJson(Map.of("ok", true));
        result.setSchemaVersion("1.0");
        result.setInsightsSchemaVersion("1.1");

        MeetingInsight meeting = new MeetingInsight();
        meeting.setPayloadJson(Map.of("id_meeting", 1));
        result.setMeetings(List.of(meeting));

        WorkerInsightSummaryResponse summary = mapper.toSummary(result);
        WorkerInsightDetailResponse detail = mapper.toDetail(result);

        assertEquals("req-1", summary.getRequestId());
        assertEquals(MeetRequestStatus.PROCESSADO, summary.getStatus());
        assertEquals("a.json", summary.getFileName());
        assertEquals(1, detail.getInsights().getReunioes().size());
    }
}
