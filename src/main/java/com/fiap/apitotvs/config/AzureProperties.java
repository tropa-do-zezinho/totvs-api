package com.fiap.apitotvs.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;

@Data
@ConfigurationProperties(prefix = "azure")
public class AzureProperties {

    private final Storage storage = new Storage();
    private final ServiceBus serviceBus = new ServiceBus();

    @Data
    public static class Storage {
        /** Full connection string: DefaultEndpointsProtocol=https;AccountName=...;AccountKey=...;EndpointSuffix=... */
        private String connectionString;
        private String accountName;
        private String accountKey;
        /** e.g. https://myaccount.blob.core.windows.net */
        private String endpoint;
        private String containerName = "reunioes";
        private int sasExpiryHours = 24;

        public boolean hasConnectionString() {
            return StringUtils.hasText(connectionString);
        }

        public boolean hasSharedKeyAuth() {
            return StringUtils.hasText(accountName) && StringUtils.hasText(accountKey);
        }

        public boolean isConfigured() {
            return hasConnectionString() || hasSharedKeyAuth();
        }

        public String resolveEndpoint() {
            if (StringUtils.hasText(endpoint)) {
                return endpoint;
            }
            return "https://" + accountName + ".blob.core.windows.net";
        }
    }

    @Data
    public static class ServiceBus {
        private String connectionString;
        private String queueName = "meet-process";

        public boolean isConfigured() {
            return StringUtils.hasText(connectionString);
        }
    }
}
