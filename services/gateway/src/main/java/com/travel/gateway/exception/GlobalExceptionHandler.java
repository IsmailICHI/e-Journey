package com.travel.gateway.exception;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.tracing.Tracer;
import org.springframework.boot.web.reactive.error.ErrorWebExceptionHandler;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
@Order(-2)
public class GlobalExceptionHandler implements ErrorWebExceptionHandler {

    private final Tracer tracer;
    private final ObjectMapper objectMapper;

    public GlobalExceptionHandler(Tracer tracer, ObjectMapper objectMapper) {
        this.tracer = tracer;
        this.objectMapper = objectMapper;
    }

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {
        if (exchange.getResponse().isCommitted()) {
            return Mono.error(ex);
        }

        HttpStatusCode statusCode = HttpStatus.INTERNAL_SERVER_ERROR;
        String errorMessage = "An unexpected gateway error occurred";
        String errorTitle = "Internal Server Error";

        if (ex instanceof ResponseStatusException rse) {
            statusCode = rse.getStatusCode();
            HttpStatus resolved = HttpStatus.resolve(statusCode.value());
            errorTitle = resolved != null ? resolved.getReasonPhrase() : "Error";
            if (rse.getReason() != null && !rse.getReason().isBlank()) {
                errorMessage = rse.getReason();
            } else if (rse.getMessage() != null) {
                errorMessage = rse.getMessage();
            }
        }

        exchange.getResponse().setStatusCode(statusCode);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);

        String traceId = (tracer.currentSpan() != null) ? tracer.currentSpan().context().traceId() : "N/A";
        String path = exchange.getRequest().getURI().getPath();

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", Instant.now().toString());
        body.put("status", statusCode.value());
        body.put("error", errorTitle);
        body.put("message", errorMessage);
        body.put("errorMessage", errorMessage);
        body.put("path", path);
        body.put("traceId", traceId);

        byte[] responseBody;
        try {
            responseBody = objectMapper.writeValueAsBytes(body);
        } catch (JsonProcessingException serializationError) {
            return Mono.error(serializationError);
        }
        DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(responseBody);

        return exchange.getResponse().writeWith(Mono.just(buffer));
    }
}