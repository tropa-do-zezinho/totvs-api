package com.fiap.apitotvs.service;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.models.BlobHttpHeaders;
import com.azure.storage.blob.sas.BlobSasPermission;
import com.azure.storage.blob.sas.BlobServiceSasSignatureValues;
import com.fiap.apitotvs.config.AzureClients;
import com.fiap.apitotvs.config.AzureProperties;
import com.fiap.apitotvs.dto.message.WorkerProcessMessage;
import com.fiap.apitotvs.dto.response.MeetRegisterResponse;
import com.fiap.apitotvs.entity.MeetRegister;
import com.fiap.apitotvs.entity.User;
import com.fiap.apitotvs.enums.MeetRequestStatus;
import com.fiap.apitotvs.exception.BusinessException;
import com.fiap.apitotvs.repository.MeetRegisterRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class BlobService {

    private final AzureClients azureClients;
    private final AzureProperties azureProperties;
    private final MeetRegisterRepository meetRegisterRepository;
    private final ServiceBusPublisher serviceBusPublisher;

    public MeetRegisterResponse upload(MultipartFile file, User user) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("File is required");
        }

        String originalFilename = StringUtils.hasText(file.getOriginalFilename())
                ? file.getOriginalFilename()
                : "upload.bin";
        String requestId = "req-" + UUID.randomUUID();
        String blobName = user.getId() + "/" + requestId + "-" + sanitizeFilename(originalFilename);

        try {
            BlobContainerClient containerClient = azureClients.blobContainerClient();
            BlobClient blobClient = containerClient.getBlobClient(blobName);
            BlobHttpHeaders headers = new BlobHttpHeaders();
            if (StringUtils.hasText(file.getContentType())) {
                headers.setContentType(file.getContentType());
            }

            blobClient.upload(file.getInputStream(), file.getSize(), true);
            if (StringUtils.hasText(file.getContentType())) {
                blobClient.setHttpHeaders(headers);
            }

            String sasUrl = generateSasUrl(blobClient);

            MeetRegister meetRegister = new MeetRegister();
            meetRegister.setBlobUrl(sasUrl);
            meetRegister.setFileName(originalFilename);
            meetRegister.setRequestId(requestId);
            meetRegister.setUser(user);
            meetRegister.setStatus(MeetRequestStatus.CRIADO);
            meetRegister = meetRegisterRepository.save(meetRegister);

            try {
                serviceBusPublisher.publishWorkerProcess(
                        new WorkerProcessMessage(requestId, sasUrl, originalFilename)
                );
                meetRegister.markAnalisando();
                meetRegister = meetRegisterRepository.save(meetRegister);
            } catch (BusinessException e) {
                meetRegister.markFalha(e.getMessage());
                meetRegisterRepository.save(meetRegister);
                throw e;
            }

            log.info("Uploaded blob request_id={} userId={} file={} status={}",
                    requestId, user.getId(), originalFilename, meetRegister.getStatus());
            return toResponse(meetRegister);
        } catch (BusinessException e) {
            throw e;
        } catch (IOException e) {
            throw new BusinessException("Failed to read uploaded file");
        } catch (Exception e) {
            log.error("Blob upload failed: {}", e.getMessage(), e);
            throw new BusinessException("Failed to upload file to Azure Blob Storage");
        }
    }

    public List<MeetRegisterResponse> listByUser(User user) {
        return meetRegisterRepository.findAllByUser_Id(user.getId()).stream()
                .map(this::toResponse)
                .toList();
    }

    private String generateSasUrl(BlobClient blobClient) {
        OffsetDateTime expiry = OffsetDateTime.now()
                .plusHours(azureProperties.getStorage().getSasExpiryHours());
        BlobSasPermission permission = new BlobSasPermission().setReadPermission(true);
        BlobServiceSasSignatureValues values = new BlobServiceSasSignatureValues(expiry, permission);
        String sasToken = blobClient.generateSas(values);
        return blobClient.getBlobUrl() + "?" + sasToken;
    }

    private String sanitizeFilename(String filename) {
        return filename.replaceAll("[\\\\/]+", "_");
    }

    private MeetRegisterResponse toResponse(MeetRegister meetRegister) {
        return new MeetRegisterResponse(
                meetRegister.getId(),
                meetRegister.getRequestId(),
                meetRegister.getFileName(),
                meetRegister.getBlobUrl(),
                meetRegister.getStatus(),
                meetRegister.getErrorMessage(),
                meetRegister.getCreatedAt(),
                meetRegister.getUpdatedAt()
        );
    }
}
