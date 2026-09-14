package com.fiap.apitotvs.controller;

import com.fiap.apitotvs.dto.response.WorkerInsightDetailResponse;
import com.fiap.apitotvs.dto.response.WorkerInsightSummaryResponse;
import com.fiap.apitotvs.entity.User;
import com.fiap.apitotvs.service.WorkerInsightsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/insights")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class InsightsController {

    private final WorkerInsightsService workerInsightsService;

    @GetMapping
    public ResponseEntity<List<WorkerInsightSummaryResponse>> list(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(workerInsightsService.listByUser(user));
    }

    @GetMapping("/{requestId}")
    public ResponseEntity<WorkerInsightDetailResponse> getByRequestId(
            @PathVariable String requestId,
            @AuthenticationPrincipal User user
    ) {
        return ResponseEntity.ok(workerInsightsService.getByRequestId(requestId, user));
    }
}
