package com.fiap.apitotvs.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Arrays;

public enum MeetRequestStatus {
    CRIADO("criado"),
    ANALISANDO("analisando"),
    PROCESSADO("processado"),
    FALHA("falha");

    private final String value;

    MeetRequestStatus(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static MeetRequestStatus fromValue(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("MeetRequestStatus is required");
        }
        return Arrays.stream(values())
                .filter(status -> status.value.equalsIgnoreCase(value) || status.name().equalsIgnoreCase(value))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown MeetRequestStatus: " + value));
    }
}
