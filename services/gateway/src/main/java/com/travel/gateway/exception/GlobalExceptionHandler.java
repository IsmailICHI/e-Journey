package com.travel.gateway.exception;

import io.micrometer.tracing.Tracer;
import org.springframework.boot.web.reactive.error.ErrorWebExceptionHandler;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

@Component
@Order(-2)
public class GlobalExceptionHandler implements ErrorWebExceptionHandler {

    private final Tracer tracer;

    public GlobalExceptionHandler(Tracer tracer) {
        this.tracer = tracer;
    }

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {
        if (exchange.getResponse().isCommitted()) {
            return Mono.error(ex);
        }

        exchange.getResponse().setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);

        String traceId = (tracer.currentSpan() != null) ? tracer.currentSpan().context().traceId() : "N/A";
        String msg = ex.getMessage() != null ? ex.getMessage().replace("\"", "\\\"") : "Unexpected error";

        String jsonError = String.format("{\"error\": \"Gateway Error\", \"traceId\": \"%s\", \"message\": \"%s\"}", traceId, msg);
        
        DataBuffer buffer = exchange.getResponse().bufferFactory()
                .wrap(jsonError.getBytes(StandardCharsets.UTF_8));

        return exchange.getResponse().writeWith(Mono.just(buffer));
    }
}