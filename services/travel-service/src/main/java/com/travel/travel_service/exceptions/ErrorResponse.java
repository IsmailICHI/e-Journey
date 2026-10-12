package com.travel.travel_service.exceptions;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(
        Instant timestamp,
        int status,
        String error,
        String message,
        String path,
        Map<String, String> validationErrors
) {
    public ErrorResponse(String errorMessage) {
        this(Instant.now(), 400, "Bad Request", errorMessage, null, null);
    }

    public ErrorResponse(int status, String error, String message, String path) {
        this(Instant.now(), status, error, message, path, null);
    }

    public ErrorResponse(int status, String error, String message, String path, Map<String, String> validationErrors) {
        this(Instant.now(), status, error, message, path, validationErrors);
    }

    @JsonProperty("errorMessage")
    public String errorMessage() {
        return message;
    }
}
