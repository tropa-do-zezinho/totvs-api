package com.fiap.apitotvs.entity;

import com.fiap.apitotvs.enums.MeetRequestStatus;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class MeetRegisterTest {

    @Test
    void markAnalisandoLimpaErro() {
        MeetRegister meet = new MeetRegister();
        meet.setStatus(MeetRequestStatus.CRIADO);
        meet.setErrorMessage("erro anterior");

        meet.markAnalisando();

        assertEquals(MeetRequestStatus.ANALISANDO, meet.getStatus());
        assertNull(meet.getErrorMessage());
    }

    @Test
    void markProcessadoLimpaErro() {
        MeetRegister meet = new MeetRegister();
        meet.setStatus(MeetRequestStatus.ANALISANDO);
        meet.setErrorMessage("temp");

        meet.markProcessado();

        assertEquals(MeetRequestStatus.PROCESSADO, meet.getStatus());
        assertNull(meet.getErrorMessage());
    }

    @Test
    void markFalhaDefineMensagem() {
        MeetRegister meet = new MeetRegister();
        meet.setStatus(MeetRequestStatus.ANALISANDO);

        meet.markFalha("Worker reported status: falha");

        assertEquals(MeetRequestStatus.FALHA, meet.getStatus());
        assertEquals("Worker reported status: falha", meet.getErrorMessage());
    }
}
