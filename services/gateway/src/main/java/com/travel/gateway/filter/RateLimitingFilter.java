package com.travel.gateway.filter;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.BucketConfiguration;
// import io.github.bucket4j.ConsumptionProbe;
import io.github.bucket4j.Refill;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.util.List;

@Component
public class RateLimitingFilter implements GlobalFilter, Ordered {

    private static final List<String> PUBLIC_ENDPOINTS = List.of(
            "/api/v1/auth/login",
            "/api/v1/auth/register",
            "/api/v1/movies/public",
            "/api/v1/auth/mfa/verify");
    private static final String PUBLIC_RATING_SUMMARY = "/api/v1/ratings/summaries";

    private final ProxyManager<String> proxyManager;

    public RateLimitingFilter(ProxyManager<String> proxyManager) {
        this.proxyManager = proxyManager;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest req = exchange.getRequest();
        String path = req.getURI().getPath();
        boolean publicEndpoint = PUBLIC_ENDPOINTS.stream().anyMatch(path::startsWith)
            || (HttpMethod.GET.equals(req.getMethod())
                && (path.matches("/api/v1/ratings/movie/\\d+/summary")
                    || PUBLIC_RATING_SUMMARY.equals(path))
                && req.getHeaders().getFirst(HttpHeaders.AUTHORIZATION) == null);

        String key = publicEndpoint ? null : req.getHeaders().getFirst("X-User-Id");
        if (key == null || key.isBlank()) {
            key = req.getRemoteAddress() != null && req.getRemoteAddress().getAddress() != null
                ? req.getRemoteAddress().getAddress().getHostAddress()
                : "anon";
        }

        Bucket bucket = proxyManager.getProxy("rl:v2:" + key, () -> getConfig());

        return Mono.fromCallable(() -> bucket.tryConsumeAndReturnRemaining(1))
                .subscribeOn(Schedulers.boundedElastic())
            .flatMap(probe -> {
                if (probe.isConsumed()) {
                        return chain.filter(exchange);
                    }
                    exchange.getResponse().setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
                long retryAfterSeconds = Math.max(1,
                    Duration.ofNanos(probe.getNanosToWaitForRefill()).toSeconds());
                exchange.getResponse().getHeaders().set(HttpHeaders.RETRY_AFTER,
                    Long.toString(retryAfterSeconds));
                    return exchange.getResponse().setComplete();
                });
    }

    private BucketConfiguration getConfig() {
        return BucketConfiguration.builder()
            .addLimit(Bandwidth.classic(300, Refill.greedy(300, Duration.ofMinutes(1))))
                .build();
    }

    @Override
    public int getOrder() {
        return 0;
    }
}