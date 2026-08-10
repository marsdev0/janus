/*
 * Copyright (c) 2026 marsdev0
 *
 * Licensed under the MIT License.
 * See the LICENSE file in the project root for full license information.
 */

package com.marsdev.janus.reply;

import com.marsdev.janus.channel.ChannelRouter;
import com.marsdev.janus.common.ErrorCode;
import com.marsdev.janus.common.JanusException;
import com.marsdev.janus.common.util.JsonUtils;
import com.marsdev.janus.entity.Channel;
import com.marsdev.janus.filter.MeteringFilter;
import com.marsdev.janus.model.RequestContext;
import com.marsdev.janus.model.Usage;
import com.marsdev.janus.quota.PromptTokenEstimator;
import com.marsdev.janus.reply.adapter.ProviderAdapter;
import com.marsdev.janus.reply.adapter.ProviderAdapterFactory;
import com.marsdev.janus.reply.adapter.model.ChatRequest;
import com.marsdev.janus.reply.adapter.model.GatewayError;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.reactor.circuitbreaker.operator.CircuitBreakerOperator;
import lombok.RequiredArgsConstructor;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author geyan
 * @date 2026/7/31
 */
@Component
@RequiredArgsConstructor
public class UpstreamProxy {

    private final WebClient webClient;
    private final ChannelRouter router;
    private final CircuitBreakerRegistry cbRegistry;
    private final UsageParser usageParser;
    private final PromptTokenEstimator estimator;
    private final MeteringFilter meteringFilter;
    private final ProviderAdapterFactory adapterFactory;

    private static final int MAX_ATTEMPTS = 10;

    // Forwards upstream SSE events as-is; ends naturally when data is [DONE]
    private static final ParameterizedTypeReference<ServerSentEvent<String>> SSE_TYPE =
            new ParameterizedTypeReference<>() {
            };

    /**
     * Non-stream forwarding + metering (mirror of the streaming relayStreamWithMetering)
     */
    public Mono<String> relayNonStreamWithMetering(RequestContext ctx) {
        ChatRequest canonical = JsonUtils.fromJson(ctx.getRawBody(), ChatRequest.class);
        AtomicLong channelIdRef = new AtomicLong(-1);
        AtomicReference<ProviderAdapter> hitAdapter = new AtomicReference<>();

        return relayWithFailover(canonical, router.route(ctx.getModel()), 0, channelIdRef, hitAdapter)
                .flatMap(resp -> {
                    Long channelId = channelIdRef.get() >= 0 ? channelIdRef.get() : null;
                    ProviderAdapter a = hitAdapter.get();
                    Usage u = a.parseUsage(resp);                    // parse usage from the raw response (upstream format)
                    return meteringFilter.settle(ctx, u, channelId)
                            .thenReturn(a.fromUpstreamResp(resp));   // return the normalized response (OpenAI format) to the client
                });
    }

    private Mono<String> relayWithFailover(ChatRequest canonical, List<Channel> candidates, int idx,
                                           AtomicLong channelIdRef, AtomicReference<ProviderAdapter> hitAdapter) {
        if (idx >= candidates.size() || idx >= MAX_ATTEMPTS) {
            return Mono.error(new JanusException(ErrorCode.ALL_CHANNELS_FAILED));
        }
        Channel ch = candidates.get(idx);
        ProviderAdapter adapter = adapterFactory.get(ch.getProtocol());   // ③ each channel picks its adapter by its own protocol
        // Circuit breaker dedicated to the current channel
        CircuitBreaker cb = cbRegistry.circuitBreaker("channel-" + ch.getId());
        return doRequestNonStream(ch, adapter.toUpstreamReq(canonical), adapter)
                .transformDeferred(CircuitBreakerOperator.of(cb))
                .doOnNext(resp -> {
                    channelIdRef.set(ch.getId());
                    hitAdapter.set(adapter);
                })
                .onErrorResume(e -> {
                    if (e instanceof GatewayError ge && !ge.isRetryable()) {
                        return Mono.error(ge);
                    }
                    // 5xx / timeout / connection failure / 429 / circuit breaker OPEN → failover to another channel
                    return relayWithFailover(canonical, candidates, idx + 1, channelIdRef, hitAdapter);
                });
    }

    private Mono<String> doRequestNonStream(Channel ch, String upstreamBody, ProviderAdapter adapter) {
        return webClient.post()
                .uri(ch.getBaseUrl() + adapter.upstreamPath())
                .header(adapter.authHeaderName(), adapter.authHeaderValue(ch.getApiKey()))
                .headers(h -> adapter.extraHeaders().forEach(h::add))
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(upstreamBody)
                .retrieve()
                .onStatus(
                        s -> s.isError(),
                        resp -> resp.bodyToMono(String.class)
                                .flatMap(b -> Mono.error(adapter.normalizeError(resp.statusCode().value(), b))))
                .bodyToMono(String.class);
    }

    /**
     * Failover is only allowed before the first byte; once streaming has started, retry is not possible
     * Stream forwarding + failover + metering. Metering is chained at the tail of the main stream (concatWith); no bare subscribe
     */
    public Flux<ServerSentEvent<String>> relayStreamWithMetering(RequestContext ctx) {
        ChatRequest canonical = JsonUtils.fromJson(ctx.getRawBody(), ChatRequest.class);
        canonical.setStream(true);

        AtomicBoolean firstByte = new AtomicBoolean(false);
        AtomicLong channelIdRef = new AtomicLong(-1);
        AtomicReference<ProviderAdapter> hitAdapter = new AtomicReference<>();
        AtomicReference<Usage> upstreamUsage = new AtomicReference<>();

        StringBuilder completionBuf = new StringBuilder();

        return failoverStream(canonical, router.route(ctx.getModel()), 0, firstByte, channelIdRef, hitAdapter)
                .doOnNext(sse -> {
                    // Accumulate usage / completion (parsed from the raw chunk, upstream format)
                    ProviderAdapter adapter = hitAdapter.get();
                    Usage u = adapter.parseUsage(sse.data());
                    if (u != null) {
                        upstreamUsage.set(u);
                    }
                    String delta = adapter.parseDeltaContent(sse.data());
                    if (delta != null) {
                        completionBuf.append(delta);
                    }
                })
                .map(sse -> ServerSentEvent.<String>builder()
                        .id(sse.id()).event(sse.event()).comment(sse.comment())
                        .data(hitAdapter.get().normalizeStreamChunk(sse.data()))   // forward the normalized chunk (OpenAI format)
                        .build())
                .concatWith(Mono.defer(() -> {
                    // Stream ended → settle (exactly once)
                    Usage u = upstreamUsage.get() != null
                            ? upstreamUsage.get()
                            : new Usage(estimator.estimate(ctx.getPromptContext()), estimator.estimate(completionBuf.toString()));  // Local fallback = main path
                    Long channelId = channelIdRef.get() >= 0 ? channelIdRef.get() : null;
                    return meteringFilter.settle(ctx, u, channelId).then(Mono.empty());
                }));
    }

    private Flux<ServerSentEvent<String>> failoverStream(ChatRequest canonical, List<Channel> candidates, int idx,
                                                         AtomicBoolean firstByte, AtomicLong channelIdRef, AtomicReference<ProviderAdapter> hitAdapter) {
        if (idx >= candidates.size() || idx >= MAX_ATTEMPTS) {
            return Flux.error(new JanusException(ErrorCode.ALL_CHANNELS_FAILED));
        }
        Channel ch = candidates.get(idx);
        ProviderAdapter adapter = adapterFactory.get(ch.getProtocol());
        CircuitBreaker cb = cbRegistry.circuitBreaker("channel-" + ch.getId());
        return doRequestStream(ch, adapter.toUpstreamReq(canonical), adapter)
                .transformDeferred(CircuitBreakerOperator.of(cb))
                // Mark after the first event received from upstream; no failover afterwards (fail rather than duplicate output)
                .doOnNext(e -> {
                    firstByte.set(true);
                    channelIdRef.set(ch.getId());
                    hitAdapter.set(adapter);
                })
                .onErrorResume(e -> {
                    if (firstByte.get()) {
                        // Error after first byte: cannot failover (would duplicate output); emit an error event to the client then end
                        ServerSentEvent<String> err = ServerSentEvent.<String>builder()
                                .data("{\"error\":{\"message\":\"upstream stream interrupted\"}}").build();
                        return Flux.just(err);
                    }
                    if (e instanceof GatewayError ge && !ge.isRetryable()) {
                        return Flux.error(ge);
                    }
                    // Failed before first byte → failover
                    return failoverStream(canonical, candidates, idx + 1, firstByte, channelIdRef, hitAdapter);
                });

    }

    private Flux<ServerSentEvent<String>> doRequestStream(Channel ch, String upstreamBody, ProviderAdapter adapter) {
        return webClient.post()
                .uri(ch.getBaseUrl() + adapter.upstreamPath())
                .header(adapter.authHeaderName(), adapter.authHeaderValue(ch.getApiKey()))
                .headers(h -> adapter.extraHeaders().forEach(h::add))
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.TEXT_EVENT_STREAM)
                .bodyValue(upstreamBody)
                .retrieve()
                .onStatus(
                        s -> s.isError(),
                        resp -> resp.bodyToMono(String.class)
                                .flatMap(b -> Mono.error(adapter.normalizeError(resp.statusCode().value(), b))))
                .bodyToFlux(SSE_TYPE);
    }
}
