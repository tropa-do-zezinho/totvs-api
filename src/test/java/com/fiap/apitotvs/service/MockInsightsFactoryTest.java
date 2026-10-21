package com.fiap.apitotvs.service;

import com.fiap.apitotvs.dto.request.worker.WorkerInsightsEnvelopeRequest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MockInsightsFactoryTest {

    private final MockInsightsFactory factory = new MockInsightsFactory();

    @Test
    void buildSuccessGeraEnvelopeConcluidoComDuasReunioes() {
        WorkerInsightsEnvelopeRequest envelope = factory.buildSuccess("req-1", "arquivo.jsonl");

        assertEquals("meeting_insights.completed", envelope.getEventType());
        assertEquals("req-1", envelope.getRequestId());
        assertEquals("concluido", envelope.getStatus());
        assertNotNull(envelope.getInsights());
        assertEquals(2, envelope.getInsights().getReunioes().size());
        assertTrue(envelope.getInsights().getResumo().get("modo").toString().contains("mock"));
        assertEquals("arquivo.jsonl", envelope.getInsights().getResumo().get("arquivo_origem"));
    }
}
