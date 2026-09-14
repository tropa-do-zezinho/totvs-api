package com.fiap.apitotvs.dto.request.worker;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class MeetingInsightRequest {

    @JsonProperty("schema_version")
    private String schemaVersion;

    @NotNull
    @JsonProperty("id_meeting")
    private Long idMeeting;

    private Map<String, Object> metadata;

    private Map<String, Object> prioridade;

    private Map<String, Object> triagem;

    @JsonProperty("status_analise")
    private String statusAnalise;

    @JsonProperty("analise_ia")
    private Map<String, Object> analiseIa;

    private Map<String, Object> rag;

    private String modelo;

    @JsonProperty("versao_prompt")
    private String versaoPrompt;
}
