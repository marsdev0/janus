package com.marsdev.janus.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marsdev.janus.common.ErrorCode;
import com.marsdev.janus.common.JanusException;
import com.marsdev.janus.filter.AuthFilter;
import com.marsdev.janus.filter.MeteringFilter;
import com.marsdev.janus.filter.QuotaPreCheckFilter;
import com.marsdev.janus.model.RequestContext;
import com.marsdev.janus.model.TokenAuth;
import com.marsdev.janus.reply.UpstreamProxy;
import com.marsdev.janus.reply.UsageParser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * @author geyan
 * @date 2026/7/31
 */
@RestController
@RequestMapping("/v1")
@RequiredArgsConstructor
@Slf4j
public class RelayController {

    private final UpstreamProxy upstreamProxy;
    private final ObjectMapper objectMapper;
    private final AuthFilter authFilter;
    private final QuotaPreCheckFilter quotaPreCheckFilter;
    private final MeteringFilter meteringFilter;
    private final UsageParser usageParser;

    @PostMapping(value = "/chat/completions", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<String> chat(@RequestBody String body, @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String auth) {
        return createContext(body)
                // auth验证
                .flatMap(ctx -> authFilter.authenticate(auth).map(a -> createAuth(ctx, a)))
                // token预扣
                .flatMap(quotaPreCheckFilter::preCheck)
                // 执行
                .flatMap(ctx ->
                        upstreamProxy.relayNonStream(ctx.getRawBody(), ctx.getModel())
                                // 核算
                                .flatMap(resp -> meteringFilter.settle(ctx, usageParser.parseUsageFromJson(resp), null)
                                        .thenReturn(resp)));
    }

    /**
     * 校验 Key 状态 / 过期 / 模型权限，绑定 tokenAuth
     */
    private RequestContext createAuth(RequestContext ctx, TokenAuth auth) {
        if (!auth.isActive()) {
            throw new JanusException(ErrorCode.TOKEN_DISABLED);
        }
        if (!auth.modelAllowed(ctx.getModel())) {
            throw new JanusException(ErrorCode.MODEL_NOT_ALLOWED);
        }
        ctx.setTokenAuth(auth);
        return ctx;
    }

    private String extractModel(String body) {
        try {
            JsonNode node = objectMapper.readTree(body);
            JsonNode modelNode = node.get("model");
            return modelNode != null && modelNode.isTextual() ? modelNode.asText() : null;
        } catch (Exception e) {
            log.error("extractModel fail ", e);
        }
        return null;
    }

    @PostMapping(value = "/chat/completions", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> chatStream(@RequestBody String body) {
        String model = extractModel(body);
        if (model == null) {
            // body 不是合法 JSON，或没有 model 字段 → 客户端的锅，不该进路由
            return Flux.error(new JanusException(ErrorCode.BAD_REQUEST));
        }
        return upstreamProxy.relayStream(body, model);
    }


    /**
     * 解析 body → 构造 RequestContext（含 model / maxTokens / promptText）
     */
    private Mono<RequestContext> createContext(String body) {
        return Mono.fromCallable(() -> {
            JsonNode root = objectMapper.readTree(body);
            String model = root.path("model").asText(null);
            if (model == null || model.isBlank()) {
                throw new JanusException(ErrorCode.BAD_REQUEST);
            }
            Integer maxTokens = null;
            if (root.has("max_completion_tokens")) {
                maxTokens = root.path("max_completion_tokens").asInt();
            } else if (root.has("max_tokens")) {
                maxTokens = root.path("max_tokens").asInt();
            }

            return RequestContext.builder()
                    .requestId(UUID.randomUUID().toString())
                    .rawBody(body)
                    .model(model)
                    .promptContext(extractPrompt(root))
                    .maxTokens(maxTokens)
                    .build();
        });
    }

    /**
     * 从 messages[].content 拼出 prompt 文本（兼容多模态 array content）
     */
    private String extractPrompt(JsonNode root) {
        StringBuilder sb = new StringBuilder();
        JsonNode messages = root.path("messages");
        if (messages.isArray()) {
            for (JsonNode node : messages) {
                JsonNode content = node.path("content");
                if (content.isTextual()) {
                    sb.append(content.asText());
                } else if (content.isArray()) {
                    for (JsonNode part : content) {
                        JsonNode t = part.path("text");
                        if (t.isTextual()) {
                            sb.append(t.asText());
                        }
                    }
                }
            }
        }
        return sb.toString();
    }
}
