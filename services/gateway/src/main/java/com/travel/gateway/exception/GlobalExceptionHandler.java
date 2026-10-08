package com.travel.gateway.exception;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.tracing.Tracer;
import org.springframework.boot.web.reactive.error.ErrorWebExceptionHandler;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

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

        exchange.getResponse().setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);

        String traceId = (tracer.currentSpan() != null) ? tracer.currentSpan().context().traceId() : "N/A";
        byte[] responseBody;
        try {
            responseBody = objectMapper.writeValueAsBytes(Map.of(
                "error", "Gateway Error",
                "traceId", traceId,
                "message", "An unexpected gateway error occurred"));
        } catch (JsonProcessingException serializationError) {
            return Mono.error(serializationError);
        }
        DataBuffer buffer = exchange.getResponse().bufferFactory()
            .wrap(responseBody);

        return exchange.getResponse().writeWith(Mono.just(buffer));
    }
}