package com.fiap.apitotvs.dto.request.worker;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class WorkerInsightsEnvelopeRequest {

    @NotBlank
    @JsonProperty("event_type")
    private String eventType;

    @NotBlank
    @JsonProperty("schema_version")
    private String schemaVersion;

    @NotBlank
    @JsonProperty("request_id")
    private String requestId;

    @NotBlank
    private String status;

    @JsonProperty("sent_at_utc")
    private Instant sentAtUtc;

    @Valid
    private InsightsDocumentRequest insights;
}
