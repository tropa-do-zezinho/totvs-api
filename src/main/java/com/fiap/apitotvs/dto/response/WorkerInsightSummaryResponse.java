package com.fiap.apitotvs.dto.response;

import com.fiap.apitotvs.enums.MeetRequestStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkerInsightSummaryResponse {
    private String requestId;
    private MeetRequestStatus status;
    private String eventType;
    private String workerStatus;
    private Instant sentAtUtc;
    private Instant geradoEmUtc;
    private Instant receivedAt;
    private Integer reunioesRecebidas;
    private Map<String, Object> resumo;
    private String fileName;
    private String errorMessage;
}
