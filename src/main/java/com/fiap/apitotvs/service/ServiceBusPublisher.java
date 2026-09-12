package com.fiap.apitotvs.service;

import com.azure.messaging.servicebus.ServiceBusMessage;
import com.azure.messaging.servicebus.ServiceBusSenderClient;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fiap.apitotvs.config.AzureClients;
import com.fiap.apitotvs.dto.message.WorkerProcessMessage;
import com.fiap.apitotvs.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ServiceBusPublisher {

    private final AzureClients azureClients;
    private final ObjectMapper objectMapper;

    public void publishWorkerProcess(WorkerProcessMessage payload) {
        try {
            ServiceBusSenderClient sender = azureClients.serviceBusSenderClient();
            String body = objectMapper.writeValueAsString(payload);
            ServiceBusMessage message = new ServiceBusMessage(body);
            message.setContentType("application/json");
            message.setMessageId(payload.getRequestId());
            sender.sendMessage(message);
            log.info("Published worker message request_id={}", payload.getRequestId());
        } catch (BusinessException e) {
            throw e;
        } catch (JsonProcessingException e) {
            throw new BusinessException("Failed to serialize Service Bus message");
        } catch (Exception e) {
            log.error("Failed to publish Service Bus message: {}", e.getMessage(), e);
            throw new BusinessException("Failed to publish message to Azure Service Bus");
        }
    }
}
