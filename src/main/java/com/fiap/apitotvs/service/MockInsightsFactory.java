package com.fiap.apitotvs.service;

import com.fiap.apitotvs.dto.request.worker.InsightsDocumentRequest;
import com.fiap.apitotvs.dto.request.worker.MeetingInsightRequest;
import com.fiap.apitotvs.dto.request.worker.WorkerInsightsEnvelopeRequest;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Envelope de insights fictício quando Blob Storage / Service Bus não estão disponíveis.
 */
@Component
public class MockInsightsFactory {

    public WorkerInsightsEnvelopeRequest buildSuccess(String requestId, String fileName) {
        Instant now = Instant.now();

        Map<String, Object> qualidade = new LinkedHashMap<>();
        qualidade.put("linhas_entrada", 42);
        qualidade.put("reunioes_unicas", 2);
        qualidade.put("duplicatas_identicas_removidas", 0);

        Map<String, Object> totais = new LinkedHashMap<>();
        totais.put("reunioes", 2);
        totais.put("candidatas_llm", 2);
        totais.put("selecionadas_llm", 2);
        totais.put("concluidas_llm_rag", 2);
        totais.put("pendentes_llm", 0);

        Map<String, Object> resumo = new LinkedHashMap<>();
        resumo.put("status_pipeline", "concluido");
        resumo.put("arquivo_origem", fileName);
        resumo.put("modo", "simulacao-api-mock");
        resumo.put("qualidade_dados", qualidade);
        resumo.put("totais", totais);
        resumo.put("mensagem",
                "Análise mockada: Azure Blob Storage e/ou Service Bus indisponíveis; "
                        + "worker e fila não foram acionados.");

        InsightsDocumentRequest insights = new InsightsDocumentRequest();
        insights.setSchemaVersion("1.1");
        insights.setGeradoEmUtc(now);
        insights.setResumo(resumo);
        insights.setReunioes(List.of(
                meeting(1001L, "Reunião comercial — renovação de contrato", "ALTA"),
                meeting(1002L, "Sync interno — backlog do trimestre", "MEDIA")
        ));

        WorkerInsightsEnvelopeRequest envelope = new WorkerInsightsEnvelopeRequest();
        envelope.setEventType("meeting_insights.completed");
        envelope.setSchemaVersion("1.0");
        envelope.setRequestId(requestId);
        envelope.setStatus("concluido");
        envelope.setSentAtUtc(now);
        envelope.setInsights(insights);
        return envelope;
    }

    private MeetingInsightRequest meeting(long idMeeting, String titulo, String prioridadeNivel) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("titulo", titulo);
        metadata.put("data", "2026-09-13 10:00:00");
        metadata.put("formato", "VIDEO");
        metadata.put("status", "COMPLETED");

        Map<String, Object> prioridade = new LinkedHashMap<>();
        prioridade.put("nivel", prioridadeNivel);
        prioridade.put("score", "ALTA".equals(prioridadeNivel) ? 0.91 : 0.62);

        Map<String, Object> triagem = new LinkedHashMap<>();
        triagem.put("categoria", "negocios");
        triagem.put("motivo", "Conteúdo mock gerado pela API (fallback Azure)");

        Map<String, Object> analiseIa = new LinkedHashMap<>();
        analiseIa.put("resumo", "Resumo fictício da reunião " + idMeeting
                + ": alinhamento de próximos passos e riscos.");
        analiseIa.put("acoes", List.of(
                "Validar pendências com o time responsável",
                "Atualizar status no CRM"
        ));
        analiseIa.put("sentimento", "neutro-positivo");

        Map<String, Object> rag = new LinkedHashMap<>();
        rag.put("fontes_consultadas", 0);
        rag.put("observacao", "RAG desabilitado na simulação");

        MeetingInsightRequest meeting = new MeetingInsightRequest();
        meeting.setSchemaVersion("1.1");
        meeting.setIdMeeting(idMeeting);
        meeting.setMetadata(metadata);
        meeting.setPrioridade(prioridade);
        meeting.setTriagem(triagem);
        meeting.setStatusAnalise("concluido");
        meeting.setAnaliseIa(analiseIa);
        meeting.setRag(rag);
        meeting.setModelo("mock-api-v1");
        meeting.setVersaoPrompt("sim-1.0");
        return meeting;
    }
}
