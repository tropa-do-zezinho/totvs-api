package com.fiap.apitotvs.dto.response;

import com.fiap.apitotvs.enums.MeetRequestStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MeetRegisterResponse {
    private Long id;
    private String requestId;
    private String fileName;
    private String blobUrl;
    private MeetRequestStatus status;
    private String errorMessage;
    private Instant createdAt;
    private Instant updatedAt;
}
