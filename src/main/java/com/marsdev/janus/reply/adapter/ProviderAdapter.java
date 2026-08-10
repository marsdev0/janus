/*
 * Copyright (c) 2026 marsdev0
 *
 * Licensed under the MIT License.
 * See the LICENSE file in the project root for full license information.
 */

package com.marsdev.janus.reply.adapter;

import com.marsdev.janus.model.Usage;
import com.marsdev.janus.reply.adapter.model.ChatRequest;
import com.marsdev.janus.reply.adapter.model.GatewayError;

import java.util.Map;

/**
 * Provider adapter interface (Adapter pattern, §2.1). One implementation per protocol
 * (openai/claude/gemini), responsible for the bidirectional conversion between
 * the Canonical model and each provider's proprietary format.
 *
 * <p>The core forwarding/routing/metering logic only knows about {@link ChatRequest}
 * (Canonical) and does not couple to any specific provider — adding a new protocol just
 * means adding a new implementation class, following the open-closed principle (§4.4).
 *
 * <p>Implementations register themselves via {@code @Component("xxxAdapter")} and are looked
 * up by {@link ProviderAdapterFactory} using the convention {@code protocol + "Adapter"}
 * (openaiAdapter / claudeAdapter / geminiAdapter).
 *
 * @author geyan
 * @date 2026/8/9
 */
public interface ProviderAdapter {

    /**
     * Outbound: Canonical → provider request body JSON.
     * <p>Handles per-vendor differences here: e.g. Claude hoists system to the top level,
     * Gemini uses generationConfig.maxOutputTokens.
     * <p>For {@code extra}: same-protocol (OpenAIAdapter) merges as-is; cross-protocol
     * (Claude/Gemini) must filter to only pass through fields the target protocol recognizes (§4.2).
     */
    String toUpstreamReq(ChatRequest req);

    /**
     * Upstream request path:
     * OpenAI {@code /chat/completions}
     * Claude {@code /v1/messages}
     * Gemini {@code .../generateContent}
     */
    String upstreamPath();

    /**
     * Auth header name:
     * OpenAI {@code Authorization}
     * Claude {@code x-api-key}
     * Gemini {@code x-goog-api-key}
     */
    String authHeaderName();


    /**
     * Auth header value:
     * OpenAI {@code "Bearer " + apiKey}
     * Claude/Gemini use {@code apiKey} directly
     */
    String authHeaderValue(String apiKey);

    /**
     * Inbound: parse usage (non-stream response body / final streaming chunk) → normalized {@link Usage}.
     * <p>The usage fields differ per vendor: OpenAI {@code prompt_tokens/completion_tokens},
     * Claude {@code input_tokens/output_tokens}, Gemini {@code promptTokenCount/candidatesTokenCount}.
     * <p>Returns null on parse failure; MeteringFilter then falls back to a local tokenizer estimate.
     */
    Usage parseUsage(String raw);

    /**
     * Error normalization: vendor error → unified {@link GatewayError}.
     * <p>{@link GatewayError#isRetryable()} decides whether to failover — this is the sole basis
     * for the failover decision.
     */
    GatewayError normalizeError(int status, String body);

    /**
     * Extra headers (e.g. the {@code anthropic-version} required by Claude); empty by default,
     * override as needed.
     */
    default Map<String, String> extraHeaders() {
        return Map.of();
    }

    /**
     * Parse the completion delta text from a streaming SSE chunk (used for local tokenizer
     * fallback estimation when the upstream does not provide usage).
     * The delta location differs per protocol: OpenAI choices[0].delta.content,
     * Claude content_block_delta.delta.text, Gemini candidates[0].content.parts.
     * Chunks without a delta (role/usage chunks) return null.
     */
    default String parseDeltaContent(String raw) {
        return null;
    }

    /**
     * Non-stream: vendor response → the OpenAI-format JSON expected by the client.
     * <p>Since the request was normalized ({@link #toUpstreamReq}), the response must also be
     * normalized — otherwise the proprietary formats returned by Claude/Gemini upstreams
     * (e.g. Claude's {@code content:[{text}]}, Gemini's {@code candidates}) would be passed
     * straight through to OpenAI-compatible clients and fail to parse. OpenAIAdapter passes through (Identity).
     */
    String fromUpstreamResp(String raw);

    /**
     * Streaming: vendor SSE chunk → OpenAI-format SSE chunk. Defaults to pass-through
     * (OpenAI protocol needs no conversion); Claude/Gemini override this to convert their own
     * SSE events into OpenAI's {@code {choices:[{delta:{content}}]}} structure.
     */
    default String normalizeStreamChunk(String raw) {
        return raw;
    }
}
