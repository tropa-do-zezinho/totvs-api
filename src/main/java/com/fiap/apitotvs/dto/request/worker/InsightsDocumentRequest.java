package com.fiap.apitotvs.dto.request.worker;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class InsightsDocumentRequest {

    @JsonProperty("schema_version")
    private String schemaVersion;

    @JsonProperty("gerado_em_utc")
    private Instant geradoEmUtc;

    private Map<String, Object> resumo;

    @Valid
    private List<MeetingInsightRequest> reunioes = new ArrayList<>();
}
