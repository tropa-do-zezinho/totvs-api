package com.fiap.apitotvs.service;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.fiap.apitotvs.config.AzureClients;
import com.fiap.apitotvs.config.AzureProperties;
import com.fiap.apitotvs.dto.message.WorkerProcessMessage;
import com.fiap.apitotvs.dto.response.MeetRegisterResponse;
import com.fiap.apitotvs.entity.MeetRegister;
import com.fiap.apitotvs.entity.User;
import com.fiap.apitotvs.enums.MeetRequestStatus;
import com.fiap.apitotvs.exception.BusinessException;
import com.fiap.apitotvs.repository.MeetRegisterRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.io.InputStream;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BlobServiceTest {

    @Mock
    private AzureClients azureClients;
    @Mock
    private AzureProperties azureProperties;
    @Mock
    private MeetRegisterRepository meetRegisterRepository;
    @Mock
    private ServiceBusPublisher serviceBusPublisher;
    @Mock
    private BlobContainerClient blobContainerClient;
    @Mock
    private BlobClient blobClient;

    @InjectMocks
    private BlobService blobService;

    private User user;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(42L);
        user.setEmail("user@totvs.com");
        user.setName("User");

        AzureProperties.Storage storage = new AzureProperties.Storage();
        storage.setSasExpiryHours(24);
        org.mockito.Mockito.lenient().when(azureProperties.getStorage()).thenReturn(storage);
    }

    @Test
    void uploadRejeitaArquivoVazio() {
        MockMultipartFile empty = new MockMultipartFile("file", "a.json", "application/json", new byte[0]);
        assertThrows(BusinessException.class, () -> blobService.upload(empty, user));
        verify(meetRegisterRepository, never()).save(any());
    }

    @Test
    void uploadPersisteEMarcaAnalisandoQuandoFilaOk() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "reunioes.json", "application/json", "{\"ok\":true}".getBytes());

        when(azureClients.blobContainerClient()).thenReturn(blobContainerClient);
        when(blobContainerClient.getBlobClient(anyString())).thenReturn(blobClient);
        when(blobClient.getBlobUrl()).thenReturn("http://blob/reunioes.json");
        when(blobClient.generateSas(any())).thenReturn("sas=token");
        when(meetRegisterRepository.save(any(MeetRegister.class))).thenAnswer(invocation -> {
            MeetRegister saved = invocation.getArgument(0);
            if (saved.getId() == null) {
                saved.setId(1L);
            }
            if (saved.getCreatedAt() == null) {
                saved.setCreatedAt(Instant.now());
                saved.setUpdatedAt(Instant.now());
            }
            return saved;
        });

        MeetRegisterResponse response = blobService.upload(file, user);

        assertEquals("reunioes.json", response.getFileName());
        assertEquals(MeetRequestStatus.ANALISANDO, response.getStatus());
        assertTrue(response.getRequestId().startsWith("req-"));
        assertTrue(response.getBlobUrl().contains("sas=token"));

        verify(blobClient).upload(any(InputStream.class), anyLong(), anyBoolean());
        verify(serviceBusPublisher).publishWorkerProcess(any(WorkerProcessMessage.class));
    }

    @Test
    void uploadMarcaFalhaQuandoServiceBusFalha() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "reunioes.json", "application/json", "{\"ok\":true}".getBytes());

        when(azureClients.blobContainerClient()).thenReturn(blobContainerClient);
        when(blobContainerClient.getBlobClient(anyString())).thenReturn(blobClient);
        when(blobClient.getBlobUrl()).thenReturn("http://blob/reunioes.json");
        when(blobClient.generateSas(any())).thenReturn("sas=token");
        when(meetRegisterRepository.save(any(MeetRegister.class))).thenAnswer(invocation -> {
            MeetRegister saved = invocation.getArgument(0);
            saved.setId(1L);
            saved.setCreatedAt(Instant.now());
            saved.setUpdatedAt(Instant.now());
            return saved;
        });
        doThrow(new BusinessException("queue down"))
                .when(serviceBusPublisher).publishWorkerProcess(any(WorkerProcessMessage.class));

        assertThrows(BusinessException.class, () -> blobService.upload(file, user));

        ArgumentCaptor<MeetRegister> captor = ArgumentCaptor.forClass(MeetRegister.class);
        verify(meetRegisterRepository, org.mockito.Mockito.atLeast(2)).save(captor.capture());
        MeetRegister last = captor.getAllValues().getLast();
        assertEquals(MeetRequestStatus.FALHA, last.getStatus());
        assertEquals("queue down", last.getErrorMessage());
    }

    @Test
    void listByUserRetornaRegistros() {
        MeetRegister meet = new MeetRegister();
        meet.setId(1L);
        meet.setRequestId("req-1");
        meet.setFileName("a.json");
        meet.setBlobUrl("http://x");
        meet.setStatus(MeetRequestStatus.CRIADO);
        meet.setCreatedAt(Instant.now());
        meet.setUpdatedAt(Instant.now());

        when(meetRegisterRepository.findAllByUser_Id(42L)).thenReturn(List.of(meet));

        List<MeetRegisterResponse> list = blobService.listByUser(user);

        assertEquals(1, list.size());
        assertEquals("req-1", list.getFirst().getRequestId());
    }
}
