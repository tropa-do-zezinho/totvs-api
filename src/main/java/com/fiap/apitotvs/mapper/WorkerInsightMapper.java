package com.fiap.apitotvs.mapper;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fiap.apitotvs.dto.request.worker.MeetingInsightRequest;
import com.fiap.apitotvs.dto.request.worker.WorkerInsightsEnvelopeRequest;
import com.fiap.apitotvs.dto.response.WorkerInsightDetailResponse;
import com.fiap.apitotvs.dto.response.WorkerInsightSummaryResponse;
import com.fiap.apitotvs.entity.MeetingInsight;
import com.fiap.apitotvs.entity.MeetRegister;
import com.fiap.apitotvs.entity.WorkerInsightResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class WorkerInsightMapper {

    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {};

    private final ObjectMapper objectMapper;

    public MeetingInsight toMeetingEntity(MeetingInsightRequest request, String requestId) {
        MeetingInsight entity = new MeetingInsight();
        entity.setRequestId(requestId);
        entity.setIdMeeting(request.getIdMeeting());
        entity.setStatusAnalise(request.getStatusAnalise());
        entity.setModelo(request.getModelo());
        entity.setVersaoPrompt(request.getVersaoPrompt());
        entity.setPayloadJson(toMap(request));
        return entity;
    }

    public WorkerInsightSummaryResponse toSummary(WorkerInsightResult result) {
        MeetRegister meetRegister = result.getMeetRegister();
        return WorkerInsightSummaryResponse.builder()
                .requestId(result.getRequestId())
                .status(meetRegister.getStatus())
                .eventType(result.getEventType())
                .workerStatus(result.getWorkerStatus())
                .sentAtUtc(result.getSentAtUtc())
                .geradoEmUtc(result.getGeradoEmUtc())
                .receivedAt(result.getReceivedAt())
                .reunioesRecebidas(result.getReunioesRecebidas())
                .resumo(result.getResumoJson())
                .fileName(meetRegister.getFileName())
                .errorMessage(meetRegister.getErrorMessage())
                .build();
    }

    public WorkerInsightDetailResponse toDetail(WorkerInsightResult result) {
        MeetRegister meetRegister = result.getMeetRegister();
        List<Map<String, Object>> reunioes = result.getMeetings().stream()
                .map(MeetingInsight::getPayloadJson)
                .toList();

        return WorkerInsightDetailResponse.builder()
                .requestId(result.getRequestId())
                .status(meetRegister.getStatus())
                .eventType(result.getEventType())
                .schemaVersion(result.getSchemaVersion())
                .workerStatus(result.getWorkerStatus())
                .sentAtUtc(result.getSentAtUtc())
                .receivedAt(result.getReceivedAt())
                .fileName(meetRegister.getFileName())
                .errorMessage(meetRegister.getErrorMessage())
                .insights(WorkerInsightDetailResponse.InsightsDocumentResponse.builder()
                        .schemaVersion(result.getInsightsSchemaVersion())
                        .geradoEmUtc(result.getGeradoEmUtc())
                        .resumo(result.getResumoJson())
                        .reunioes(reunioes)
                        .build())
                .build();
    }

    public void applyEnvelope(WorkerInsightResult result, WorkerInsightsEnvelopeRequest request) {
        result.setRequestId(request.getRequestId());
        result.setEventType(request.getEventType());
        result.setSchemaVersion(request.getSchemaVersion());
        result.setWorkerStatus(request.getStatus());
        result.setSentAtUtc(request.getSentAtUtc());

        if (request.getInsights() != null) {
            result.setInsightsSchemaVersion(request.getInsights().getSchemaVersion());
            result.setGeradoEmUtc(request.getInsights().getGeradoEmUtc());
            result.setResumoJson(request.getInsights().getResumo());
        }
    }

    private Map<String, Object> toMap(MeetingInsightRequest request) {
        Map<String, Object> payload = objectMapper.convertValue(request, MAP_TYPE);
        return payload == null ? new LinkedHashMap<>() : payload;
    }
}
