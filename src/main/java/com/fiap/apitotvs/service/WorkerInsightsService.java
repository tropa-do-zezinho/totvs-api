package com.fiap.apitotvs.service;

import com.fiap.apitotvs.dto.request.worker.WorkerInsightsEnvelopeRequest;
import com.fiap.apitotvs.dto.response.WorkerInsightDetailResponse;
import com.fiap.apitotvs.dto.response.WorkerInsightPersistResponse;
import com.fiap.apitotvs.dto.response.WorkerInsightSummaryResponse;
import com.fiap.apitotvs.entity.User;

import java.util.List;

public interface WorkerInsightsService {

    WorkerInsightPersistResponse receiveInsights(WorkerInsightsEnvelopeRequest request);

    List<WorkerInsightSummaryResponse> listByUser(User user);

    WorkerInsightDetailResponse getByRequestId(String requestId, User user);
}
