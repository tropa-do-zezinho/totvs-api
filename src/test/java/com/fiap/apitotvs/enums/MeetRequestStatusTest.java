package com.fiap.apitotvs.enums;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MeetRequestStatusTest {

    @Test
    void fromValueAceitaValorJson() {
        assertEquals(MeetRequestStatus.CRIADO, MeetRequestStatus.fromValue("criado"));
        assertEquals(MeetRequestStatus.ANALISANDO, MeetRequestStatus.fromValue("analisando"));
        assertEquals(MeetRequestStatus.PROCESSADO, MeetRequestStatus.fromValue("PROCESSADO"));
        assertEquals(MeetRequestStatus.FALHA, MeetRequestStatus.fromValue("falha"));
    }

    @Test
    void fromValueRejeitaNuloOuDesconhecido() {
        assertThrows(IllegalArgumentException.class, () -> MeetRequestStatus.fromValue(null));
        assertThrows(IllegalArgumentException.class, () -> MeetRequestStatus.fromValue(" "));
        assertThrows(IllegalArgumentException.class, () -> MeetRequestStatus.fromValue("desconhecido"));
    }

    @Test
    void getValueRetornaSlug() {
        assertEquals("processado", MeetRequestStatus.PROCESSADO.getValue());
    }
}
