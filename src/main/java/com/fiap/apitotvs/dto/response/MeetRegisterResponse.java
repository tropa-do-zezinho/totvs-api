package com.fiap.apitotvs.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MeetRegisterResponse {
    private Long id;
    private String requestId;
    private String fileName;
    private String blobUrl;
}
