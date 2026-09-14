package com.fiap.apitotvs.dto.response;

import com.fiap.apitotvs.enums.MeetRequestStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkerInsightDetailResponse {
    private String requestId;
    private MeetRequestStatus status;
    private String eventType;
    private String schemaVersion;
    private String workerStatus;
    private Instant sentAtUtc;
    private Instant receivedAt;
    private String fileName;
    private String errorMessage;
    private InsightsDocumentResponse insights;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class InsightsDocumentResponse {
        private String schemaVersion;
        private Instant geradoEmUtc;
        private Map<String, Object> resumo;
        private List<Map<String, Object>> reunioes;
    }
}
