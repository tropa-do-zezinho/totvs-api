package com.fiap.apitotvs.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WorkerInsightPersistResponse {

    @JsonProperty("request_id")
    private String requestId;

    private String status;

    @JsonProperty("reunioes_recebidas")
    private Integer reunioesRecebidas;
}
