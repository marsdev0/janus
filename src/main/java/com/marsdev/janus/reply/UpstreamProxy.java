package com.marsdev.janus.reply;

import com.marsdev.janus.channel.ChannelRouter;
import com.marsdev.janus.common.ErrorCode;
import com.marsdev.janus.common.JanusException;
import com.marsdev.janus.entity.Channel;
import com.marsdev.janus.filter.MeteringFilter;
import com.marsdev.janus.model.RequestContext;
import com.marsdev.janus.model.Usage;
import com.marsdev.janus.quota.PromptTokenEstimator;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.reactor.circuitbreaker.operator.CircuitBreakerOperator;
import lombok.RequiredArgsConstructor;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
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

    private static final int MAX_ATTEMPTS = 10;

    // Forwards upstream SSE events as-is; ends naturally when data is [DONE]
    private static final ParameterizedTypeReference<ServerSentEvent<String>> SSE_TYPE =
            new ParameterizedTypeReference<>() {
            };

    /**
     * 新增 failover（故障转移）
     */
    public Mono<String> relayNonStream(String body, String model) {
        List<Channel> candidates = router.route(model);
        return relayWithFailover(body, candidates, 0);

    }

    private Mono<String> relayWithFailover(String body, List<Channel> candidates, int idx) {
        if (idx >= candidates.size() || idx >= MAX_ATTEMPTS) {
            return Mono.error(new JanusException(ErrorCode.ALL_CHANNELS_FAILED));
        }
        Channel ch = candidates.get(idx);
        // 当前channel的专属断路器
        CircuitBreaker cb = cbRegistry.circuitBreaker("channel-" + ch.getId());
        return doRequestNonStream(ch, body)
                .transformDeferred(CircuitBreakerOperator.of(cb))
                .onErrorResume(e -> {
                    if (e instanceof WebClientResponseException w) {
                        int code = w.getStatusCode().value();
                        // 4xx（除 429 限流）是客户端的错，换渠道也一样 → 直接透传给客户端
                        if (w.getStatusCode().is4xxClientError() && code != 429) {
                            return Mono.error(e);
                        }
                    }
                    // 5xx / 超时 / 连接失败 / 429 / 断路器 OPEN → 换渠道
                    return relayWithFailover(body, candidates, idx + 1);
                });
    }

    private Mono<String> doRequestNonStream(Channel ch, String body) {
        return webClient.post()
                .uri(ch.getBaseUrl() + "/chat/completions")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + ch.getApiKey())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(String.class);
    }


    /**
     * failover 只能在首字符之前，一旦流式开始推送，就不能重试
     * 流式转发 + failover + metering。metering 串进主流尾端（concatWith），不裸 subscribe
     */
    public Flux<ServerSentEvent<String>> relayStreamWithMetering(RequestContext ctx) {
        List<Channel> candidates = router.route(ctx.getModel());
        String body = usageParser.withStreamOptions(ctx.getRawBody());

        AtomicBoolean firstByte = new AtomicBoolean(false);
        AtomicLong channelIdRef = new AtomicLong(-1);
        AtomicReference<Usage> upstreamUsage = new AtomicReference<>();
        StringBuilder completionBuf = new StringBuilder();

        return failoverStream(body, candidates, 0, firstByte, channelIdRef)
                .doOnNext(sse -> {
                    // 累加 usage / completion
                    Usage u = usageParser.parseUsage(sse.data());
                    if (u != null) {
                        upstreamUsage.set(u);
                    }
                    String delta = usageParser.parseDeltaContent(sse.data());
                    if (delta != null) {
                        completionBuf.append(delta);
                    }
                })
                .concatWith(Mono.defer(() -> {
                    // 流结束 → 结算（仅一次）
                    Usage u = upstreamUsage.get() != null
                            ? upstreamUsage.get()
                            : new Usage(estimator.estimate(ctx.getPromptContext()), estimator.estimate(completionBuf.toString()));  // 本地兜底=主路径
                    Long channelId = channelIdRef.get() >= 0 ? channelIdRef.get() : null;
                    return meteringFilter.settle(ctx, u, channelId).then(Mono.empty());
                }));
    }

    private Flux<ServerSentEvent<String>> failoverStream(String body, List<Channel> candidates, int idx,
                                                         AtomicBoolean firstByte, AtomicLong channelIdRef) {
        if (idx >= candidates.size() || idx >= MAX_ATTEMPTS) {
            return Flux.error(new JanusException(ErrorCode.ALL_CHANNELS_FAILED));
        }
        Channel ch = candidates.get(idx);
        CircuitBreaker cb = cbRegistry.circuitBreaker("channel-" + ch.getId());
        return doRequestStream(ch, body)
                .transformDeferred(CircuitBreakerOperator.of(cb))
                // 从上游收到第一个事件后标记，之后不能再 Failover（宁可失败也不重复输出）
                .doOnNext(e -> {
                    firstByte.set(true);
                    channelIdRef.set(ch.getId());
                })
                .onErrorResume(e -> {
                    if (e instanceof WebClientResponseException w) {
                        int code = w.getStatusCode().value();
                        // 4xx（除 429 限流）是客户端的错，换渠道也一样 → 直接透传给客户端
                        if (w.getStatusCode().is4xxClientError() && code != 429) {
                            return Flux.error(e);
                        }
                    }
                    if (firstByte.get()) {
                        // 首字节后 → 不重试
                        return Flux.empty();
                    }
                    // 首字节前失败 → Failover
                    return failoverStream(body, candidates, idx + 1, firstByte, channelIdRef);
                });

    }

    private Flux<ServerSentEvent<String>> doRequestStream(Channel ch, String body) {
        return webClient.post()
                .uri(ch.getBaseUrl() + "/chat/completions")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + ch.getApiKey())
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.TEXT_EVENT_STREAM)
                .bodyValue(body)
                .retrieve()
                .bodyToFlux(SSE_TYPE);
    }
}
