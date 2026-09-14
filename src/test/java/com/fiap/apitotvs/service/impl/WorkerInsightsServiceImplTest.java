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
import com.fiap.apitotvs.enums.MeetRequestStatus;
import com.fiap.apitotvs.exception.BusinessException;
import com.fiap.apitotvs.exception.ResourceNotFoundException;
import com.fiap.apitotvs.mapper.WorkerInsightMapper;
import com.fiap.apitotvs.repository.MeetRegisterRepository;
import com.fiap.apitotvs.repository.WorkerInsightResultRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WorkerInsightsServiceImplTest {

    @Mock
    private MeetRegisterRepository meetRegisterRepository;
    @Mock
    private WorkerInsightResultRepository workerInsightResultRepository;
    @Mock
    private WorkerInsightMapper workerInsightMapper;

    @InjectMocks
    private WorkerInsightsServiceImpl workerInsightsService;

    @Test
    void receiveInsightsPersisteEMarcaProcessado() {
        MeetRegister meetRegister = baseMeetRegister();
        when(meetRegisterRepository.findByRequestId("req-1")).thenReturn(Optional.of(meetRegister));
        when(workerInsightResultRepository.findByRequestId("req-1")).thenReturn(Optional.empty());
        when(workerInsightResultRepository.saveAndFlush(any(WorkerInsightResult.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(workerInsightResultRepository.save(any(WorkerInsightResult.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        MeetingInsightRequest meetingRequest = new MeetingInsightRequest();
        meetingRequest.setIdMeeting(900001L);
        meetingRequest.setStatusAnalise("concluida_llm_rag");

        MeetingInsight meetingEntity = new MeetingInsight();
        meetingEntity.setIdMeeting(900001L);
        when(workerInsightMapper.toMeetingEntity(meetingRequest, "req-1")).thenReturn(meetingEntity);

        WorkerInsightsEnvelopeRequest envelope = successEnvelope(List.of(meetingRequest));

        WorkerInsightPersistResponse response = workerInsightsService.receiveInsights(envelope);

        assertEquals("req-1", response.getRequestId());
        assertEquals("persistido", response.getStatus());
        assertEquals(1, response.getReunioesRecebidas());
        assertEquals(MeetRequestStatus.PROCESSADO, meetRegister.getStatus());
        verify(workerInsightMapper).applyEnvelope(any(WorkerInsightResult.class), any(WorkerInsightsEnvelopeRequest.class));
        verify(meetRegisterRepository).save(meetRegister);
    }

    @Test
    void receiveInsightsMarcaFalhaQuandoWorkerReportaErro() {
        MeetRegister meetRegister = baseMeetRegister();
        when(meetRegisterRepository.findByRequestId("req-1")).thenReturn(Optional.of(meetRegister));
        when(workerInsightResultRepository.findByRequestId("req-1")).thenReturn(Optional.empty());
        when(workerInsightResultRepository.saveAndFlush(any())).thenAnswer(i -> i.getArgument(0));
        when(workerInsightResultRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        WorkerInsightsEnvelopeRequest envelope = successEnvelope(List.of());
        envelope.setStatus("falha");

        workerInsightsService.receiveInsights(envelope);

        assertEquals(MeetRequestStatus.FALHA, meetRegister.getStatus());
        assertEquals("Worker reported status: falha", meetRegister.getErrorMessage());
    }

    @Test
    void receiveInsightsLanca404SeMeetRegisterNaoExiste() {
        when(meetRegisterRepository.findByRequestId("req-x")).thenReturn(Optional.empty());

        WorkerInsightsEnvelopeRequest envelope = successEnvelope(List.of());
        envelope.setRequestId("req-x");

        assertThrows(ResourceNotFoundException.class, () -> workerInsightsService.receiveInsights(envelope));
        verify(workerInsightResultRepository, never()).save(any());
    }

    @Test
    void receiveInsightsExigeIdMeeting() {
        MeetRegister meetRegister = baseMeetRegister();
        when(meetRegisterRepository.findByRequestId("req-1")).thenReturn(Optional.of(meetRegister));
        when(workerInsightResultRepository.findByRequestId("req-1")).thenReturn(Optional.empty());
        when(workerInsightResultRepository.saveAndFlush(any())).thenAnswer(i -> i.getArgument(0));

        MeetingInsightRequest invalid = new MeetingInsightRequest();
        invalid.setIdMeeting(null);

        WorkerInsightsEnvelopeRequest envelope = successEnvelope(List.of(invalid));

        assertThrows(BusinessException.class, () -> workerInsightsService.receiveInsights(envelope));
    }

    @Test
    void listByUserMapeiaResumos() {
        User user = new User();
        user.setId(1L);

        WorkerInsightResult result = new WorkerInsightResult();
        WorkerInsightSummaryResponse summary = WorkerInsightSummaryResponse.builder()
                .requestId("req-1")
                .build();

        when(workerInsightResultRepository.findAllByUserId(1L)).thenReturn(List.of(result));
        when(workerInsightMapper.toSummary(result)).thenReturn(summary);

        List<WorkerInsightSummaryResponse> list = workerInsightsService.listByUser(user);

        assertEquals(1, list.size());
        assertEquals("req-1", list.getFirst().getRequestId());
    }

    @Test
    void getByRequestIdLanca404QuandoNaoEncontrado() {
        User user = new User();
        user.setId(1L);
        when(workerInsightResultRepository.findDetailedByRequestIdAndUserId("req-x", 1L))
                .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> workerInsightsService.getByRequestId("req-x", user));
    }

    @Test
    void getByRequestIdRetornaDetalhe() {
        User user = new User();
        user.setId(1L);
        WorkerInsightResult result = new WorkerInsightResult();
        WorkerInsightDetailResponse detail = WorkerInsightDetailResponse.builder()
                .requestId("req-1")
                .build();

        when(workerInsightResultRepository.findDetailedByRequestIdAndUserId("req-1", 1L))
                .thenReturn(Optional.of(result));
        when(workerInsightMapper.toDetail(result)).thenReturn(detail);

        WorkerInsightDetailResponse response = workerInsightsService.getByRequestId("req-1", user);

        assertEquals("req-1", response.getRequestId());
    }

    private MeetRegister baseMeetRegister() {
        MeetRegister meetRegister = new MeetRegister();
        meetRegister.setId(1L);
        meetRegister.setRequestId("req-1");
        meetRegister.setFileName("reunioes.json");
        meetRegister.setBlobUrl("https://blob/file");
        meetRegister.setStatus(MeetRequestStatus.ANALISANDO);
        return meetRegister;
    }

    private WorkerInsightsEnvelopeRequest successEnvelope(List<MeetingInsightRequest> reunioes) {
        InsightsDocumentRequest insights = new InsightsDocumentRequest();
        insights.setSchemaVersion("1.1");
        insights.setGeradoEmUtc(Instant.parse("2026-09-13T14:00:00Z"));
        insights.setResumo(Map.of("status_pipeline", "concluido"));
        insights.setReunioes(reunioes);

        WorkerInsightsEnvelopeRequest envelope = new WorkerInsightsEnvelopeRequest();
        envelope.setEventType("meeting_insights.completed");
        envelope.setSchemaVersion("1.0");
        envelope.setRequestId("req-1");
        envelope.setStatus("concluido");
        envelope.setSentAtUtc(Instant.parse("2026-09-13T14:00:00Z"));
        envelope.setInsights(insights);
        return envelope;
    }
}
