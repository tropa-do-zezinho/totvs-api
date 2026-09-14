package com.fiap.apitotvs.controller;

import com.fiap.apitotvs.dto.request.worker.WorkerInsightsEnvelopeRequest;
import com.fiap.apitotvs.dto.response.WorkerInsightPersistResponse;
import com.fiap.apitotvs.service.WorkerInsightsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/worker")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class WorkerInsightsController {

    private final WorkerInsightsService workerInsightsService;

    @Value("${INSIGHTS_API_TOKEN:}")
    private String expectedToken;

    @PostMapping("/insights")
    public ResponseEntity<WorkerInsightPersistResponse> receiveInsights(
            @Valid @RequestBody WorkerInsightsEnvelopeRequest request,
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        if (expectedToken == null || expectedToken.isBlank()) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
        }
        if (authorization == null || !authorization.startsWith("Bearer ") ||
                !MessageDigest.isEqual(
                        expectedToken.getBytes(StandardCharsets.UTF_8),
                        authorization.substring(7).getBytes(StandardCharsets.UTF_8))) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok(workerInsightsService.receiveInsights(request));
    }
}
