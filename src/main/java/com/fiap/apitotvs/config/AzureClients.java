package com.fiap.apitotvs.config;

import com.azure.messaging.servicebus.ServiceBusClientBuilder;
import com.azure.messaging.servicebus.ServiceBusSenderClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import com.azure.storage.common.StorageSharedKeyCredential;
import com.fiap.apitotvs.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@EnableConfigurationProperties(AzureProperties.class)
public class AzureClients {

    private final AzureProperties properties;

    private volatile BlobContainerClient blobContainerClient;
    private volatile ServiceBusSenderClient serviceBusSenderClient;

    public BlobContainerClient blobContainerClient() {
        BlobContainerClient existing = blobContainerClient;
        if (existing != null) {
            return existing;
        }
        synchronized (this) {
            if (blobContainerClient == null) {
                blobContainerClient = createBlobContainerClient();
            }
            return blobContainerClient;
        }
    }

    public ServiceBusSenderClient serviceBusSenderClient() {
        ServiceBusSenderClient existing = serviceBusSenderClient;
        if (existing != null) {
            return existing;
        }
        synchronized (this) {
            if (serviceBusSenderClient == null) {
                serviceBusSenderClient = createServiceBusSenderClient();
            }
            return serviceBusSenderClient;
        }
    }

    private BlobContainerClient createBlobContainerClient() {
        AzureProperties.Storage storage = properties.getStorage();
        if (!storage.isConfigured()) {
            throw new BusinessException(
                    "Azure Storage is not configured. Set AZURE_STORAGE_CONNECTION_STRING "
                            + "or AZURE_STORAGE_ACCOUNT_NAME + AZURE_STORAGE_ACCOUNT_KEY "
                            + "(and optionally AZURE_STORAGE_ACCOUNT_ENDPOINT)");
        }

        try {
            BlobServiceClientBuilder builder = new BlobServiceClientBuilder();

            if (storage.hasConnectionString()) {
                validateStorageConnectionString(storage.getConnectionString());
                builder.connectionString(storage.getConnectionString().trim());
            } else {
                builder.endpoint(storage.resolveEndpoint())
                        .credential(new StorageSharedKeyCredential(
                                storage.getAccountName().trim(),
                                storage.getAccountKey().trim()));
            }

            BlobServiceClient serviceClient = builder.buildClient();
            BlobContainerClient containerClient = serviceClient.getBlobContainerClient(
                    storage.getContainerName());

            if (!containerClient.exists()) {
                containerClient.create();
            }

            return containerClient;
        } catch (BusinessException e) {
            throw e;
        } catch (IllegalArgumentException e) {
            throw new BusinessException(
                    "Invalid Azure Storage credentials. "
                            + "Connection string must look like: "
                            + "DefaultEndpointsProtocol=https;AccountName=...;AccountKey=...;EndpointSuffix=core.windows.net. "
                            + "Details: " + e.getMessage());
        } catch (Exception e) {
            throw new BusinessException("Failed to connect to Azure Blob Storage: " + e.getMessage());
        }
    }

    private ServiceBusSenderClient createServiceBusSenderClient() {
        AzureProperties.ServiceBus serviceBus = properties.getServiceBus();
        if (!serviceBus.isConfigured()) {
            throw new BusinessException(
                    "Azure Service Bus is not configured. Set AZURE_SERVICE_BUS_CONNECTION_STRING");
        }

        try {
            String connectionString = serviceBus.getConnectionString().trim();
            boolean emulator = connectionString.contains("UseDevelopmentEmulator=true");
            if (!connectionString.contains("Endpoint=")
                    || (!emulator && !connectionString.contains("SharedAccessKey="))) {
                throw new BusinessException(
                        "Invalid AZURE_SERVICE_BUS_CONNECTION_STRING. Expected format: "
                                + "Endpoint=sb://.../;SharedAccessKeyName=...;SharedAccessKey=... "
                                + "(local emulator also needs UseDevelopmentEmulator=true)");
            }

            return new ServiceBusClientBuilder()
                    .connectionString(connectionString)
                    .sender()
                    .queueName(serviceBus.getQueueName())
                    .buildClient();
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException("Failed to connect to Azure Service Bus: " + e.getMessage());
        }
    }

    private void validateStorageConnectionString(String connectionString) {
        String value = connectionString.trim();
        if (value.startsWith("http://") || value.startsWith("https://")) {
            throw new BusinessException(
                    "AZURE_STORAGE_CONNECTION_STRING looks like a URL. "
                            + "Use a full connection string (or the Azurite default in application.properties).");
        }
        boolean azurite = value.contains("devstoreaccount1") || value.contains("UseDevelopmentStorage=true");
        boolean realAccount = value.contains("AccountName=") && value.contains("AccountKey=");
        if (!azurite && !realAccount) {
            throw new BusinessException(
                    "Invalid AZURE_STORAGE_CONNECTION_STRING. Expected Azurite or: "
                            + "DefaultEndpointsProtocol=https;AccountName=...;AccountKey=...;EndpointSuffix=core.windows.net");
        }
    }
}
