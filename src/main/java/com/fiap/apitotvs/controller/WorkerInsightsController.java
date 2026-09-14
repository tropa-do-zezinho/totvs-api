package com.fiap.apitotvs.controller;

import com.fiap.apitotvs.dto.request.worker.WorkerInsightsEnvelopeRequest;
import com.fiap.apitotvs.dto.response.WorkerInsightPersistResponse;
import com.fiap.apitotvs.service.WorkerInsightsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/worker")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class WorkerInsightsController {

    private final WorkerInsightsService workerInsightsService;

    @PostMapping("/insights")
    public ResponseEntity<WorkerInsightPersistResponse> receiveInsights(
            @Valid @RequestBody WorkerInsightsEnvelopeRequest request
    ) {
        return ResponseEntity.ok(workerInsightsService.receiveInsights(request));
    }
}
