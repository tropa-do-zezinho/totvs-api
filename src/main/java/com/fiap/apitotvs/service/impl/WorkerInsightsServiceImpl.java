package com.fiap.apitotvs.service.impl;

import com.fiap.apitotvs.dto.request.worker.InsightsDocumentRequest;
import com.fiap.apitotvs.dto.request.worker.MeetingInsightRequest;
import com.fiap.apitotvs.dto.request.worker.WorkerInsightsEnvelopeRequest;
import com.fiap.apitotvs.dto.response.WorkerInsightDetailResponse;
import com.fiap.apitotvs.dto.response.WorkerInsightPersistResponse;
import com.fiap.apitotvs.dto.response.WorkerInsightSummaryResponse;
import com.fiap.apitotvs.entity.MeetingInsight;
import com.fiap.apitotvs.entity.MeetRegister;
import com.fiap.apitotvs.entity.User;
import com.fiap.apitotvs.entity.WorkerInsightResult;
import com.fiap.apitotvs.exception.BusinessException;
import com.fiap.apitotvs.exception.ResourceNotFoundException;
import com.fiap.apitotvs.mapper.WorkerInsightMapper;
import com.fiap.apitotvs.repository.MeetRegisterRepository;
import com.fiap.apitotvs.repository.WorkerInsightResultRepository;
import com.fiap.apitotvs.service.WorkerInsightsService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class WorkerInsightsServiceImpl implements WorkerInsightsService {

    private static final Set<String> SUCCESS_STATUSES = Set.of(
            "concluido", "concluído", "sucesso", "completed", "success", "ok"
    );
    private static final Set<String> FAILURE_STATUSES = Set.of(
            "falha", "erro", "failed", "error", "failure"
    );

    private final MeetRegisterRepository meetRegisterRepository;
    private final WorkerInsightResultRepository workerInsightResultRepository;
    private final WorkerInsightMapper workerInsightMapper;

    @Override
    public WorkerInsightPersistResponse receiveInsights(WorkerInsightsEnvelopeRequest request) {
        MeetRegister meetRegister = meetRegisterRepository.findByRequestId(request.getRequestId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "MeetRegister", "requestId", request.getRequestId()));

        WorkerInsightResult result = workerInsightResultRepository.findByRequestId(request.getRequestId())
                .orElseGet(WorkerInsightResult::new);

        result.setMeetRegister(meetRegister);
        workerInsightMapper.applyEnvelope(result, request);
        if (result.getReceivedAt() == null) {
            result.setReceivedAt(Instant.now());
        }

        // Clear + flush before insert to avoid unique constraint conflicts on upsert.
        result.getMeetings().clear();
        result.setReunioesRecebidas(0);
        workerInsightResultRepository.saveAndFlush(result);

        List<MeetingInsight> meetings = buildMeetings(request);
        result.replaceMeetings(meetings);
        workerInsightResultRepository.save(result);

        meetRegister.setInsightResult(result);
        applyMeetRegisterStatus(meetRegister, request.getStatus());
        meetRegisterRepository.save(meetRegister);

        log.info(
                "Persisted worker insights request_id={} workerStatus={} reunioes={}",
                request.getRequestId(),
                request.getStatus(),
                meetings.size()
        );

        return new WorkerInsightPersistResponse(
                request.getRequestId(),
                "persistido",
                meetings.size()
        );
    }

    @Override
    @Transactional(Transactional.TxType.SUPPORTS)
    public List<WorkerInsightSummaryResponse> listByUser(User user) {
        return workerInsightResultRepository.findAllByUserId(user.getId()).stream()
                .map(workerInsightMapper::toSummary)
                .toList();
    }

    @Override
    @Transactional(Transactional.TxType.SUPPORTS)
    public WorkerInsightDetailResponse getByRequestId(String requestId, User user) {
        WorkerInsightResult result = workerInsightResultRepository
                .findDetailedByRequestIdAndUserId(requestId, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "WorkerInsightResult", "requestId", requestId));
        return workerInsightMapper.toDetail(result);
    }

    private List<MeetingInsight> buildMeetings(WorkerInsightsEnvelopeRequest request) {
        InsightsDocumentRequest insights = request.getInsights();
        if (insights == null || insights.getReunioes() == null) {
            return List.of();
        }

        List<MeetingInsight> meetings = new ArrayList<>();
        for (MeetingInsightRequest meetingRequest : insights.getReunioes()) {
            if (meetingRequest == null || meetingRequest.getIdMeeting() == null) {
                throw new BusinessException("Each meeting must include id_meeting");
            }
            meetings.add(workerInsightMapper.toMeetingEntity(meetingRequest, request.getRequestId()));
        }
        return meetings;
    }

    private void applyMeetRegisterStatus(MeetRegister meetRegister, String workerStatus) {
        String normalized = normalizeStatus(workerStatus);
        if (FAILURE_STATUSES.contains(normalized)) {
            meetRegister.markFalha("Worker reported status: " + workerStatus);
            return;
        }
        // Worker contract currently sends "concluido"; unknown non-failure statuses are treated as success.
        if (!SUCCESS_STATUSES.contains(normalized) && StringUtils.hasText(normalized)) {
            log.warn("Unexpected worker status '{}' for request_id={}; marking as processado",
                    workerStatus, meetRegister.getRequestId());
        }
        meetRegister.markProcessado();
    }

    private String normalizeStatus(String status) {
        if (!StringUtils.hasText(status)) {
            return "";
        }
        return status.trim().toLowerCase(Locale.ROOT);
    }
}
