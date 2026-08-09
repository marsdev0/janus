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
 * 厂商适配器接口（Adapter 模式，§2.1）。每个协议（openai/claude/gemini）一个实现，
 * 负责「Canonical ⇄ 厂商私有格式」的双向转换。
 *
 * <p>核心转发/路由/计费逻辑只认 {@link ChatRequest}（Canonical），不耦合任何厂商 ——
 * 新增协议只要加一个实现类，符合开闭原则（§4.4）。
 *
 * <p>实现类用 {@code @Component("xxxAdapter")} 注册，由 {@link ProviderAdapterFactory} 按
 * {@code protocol + "Adapter"} 取（openaiAdapter / claudeAdapter / geminiAdapter）。
 *
 * @author geyan
 * @date 2026/8/9
 */
public interface ProviderAdapter {

    /**
     * 出站：Canonical → 厂商请求体 JSON。
     * <p>这里处理各家差异：如 Claude 把 system 提到顶层、Gemini 用 generationConfig.maxOutputTokens。
     * <p>对 {@code extra}：同协议（OpenAIAdapter）原样合并；跨协议（Claude/Gemini）需过滤只透传目标协议认的字段（§4.2）。
     */
    String toUpstreamReq(ChatRequest req);

    /**
     * 上游请求路径：
     * OpenAI {@code /chat/completions}
     * Claude {@code /v1/messages}
     * Gemini {@code .../generateContent}
     */
    String upstreamPath();

    /**
     * 鉴权 header 名：
     * OpenAI {@code Authorization}
     * Claude {@code x-api-key}
     * Gemini {@code x-goog-api-key}
     */
    String authHeaderName();


    /**
     * 鉴权 header 值：
     * OpenAI {@code "Bearer " + apiKey}
     * Claude/Gemini 直接 {@code apiKey}
     */
    String authHeaderValue(String apiKey);

    /**
     * 入站：解析 usage（非流式响应体 / 流式末尾 chunk）→ 归一化 {@link Usage}。
     * <p>各家 usage 字段不同：OpenAI {@code prompt_tokens/completion_tokens}、
     * Claude {@code input_tokens/output_tokens}、Gemini {@code promptTokenCount/candidatesTokenCount}。
     * <p>解析失败返回 null，由 MeteringFilter 走本地 tokenizer 估算兜底。
     */
    Usage parseUsage(String raw);

    /**
     * 错误归一化：厂商错误 → 统一 {@link GatewayError}。
     * <p>{@link GatewayError#isRetryable()} 决定是否 failover —— 这是 Failover 判断的唯一依据。
     */
    GatewayError normalizeError(int status, String body);

    /**
     * 额外 header（如 Claude 必须的 {@code anthropic-version}），默认空，按需覆盖
     */
    default Map<String, String> extraHeaders() {
        return Map.of();
    }

    /**
     * 解析流式 SSE chunk 的 completion 增量文本（上游不给 usage 时，本地 tokenizer 兜底估算用）。
     * 各协议 delta 位置不同：OpenAI choices[0].delta.content、Claude content_block_delta.delta.text、
     * Gemini candidates[0].content.parts。无 delta 的 chunk（role/usage chunk）返回 null。
     */
    default String parseDeltaContent(String raw) {
        return null;
    }

    /**
     * 非流式：厂商响应 → 客户端期望的 OpenAI 格式 JSON。
     * <p>请求归一化了（{@link #toUpstreamReq}），响应也必须归一化 —— 否则 Claude/Gemini 上游返回的
     * 私有格式（如 Claude 的 {@code content:[{text}]}、Gemini 的 {@code candidates}）直接透传给
     * OpenAI 兼容客户端会解析失败。OpenAIAdapter 直通（Identity）。
     */
    String fromUpstreamResp(String raw);

    /**
     * 流式：厂商 SSE chunk → OpenAI 格式 SSE chunk。默认直通（OpenAI 协议无需转换）；
     * Claude/Gemini 覆盖此方法把自家 SSE 事件转成 OpenAI 的 {@code {choices:[{delta:{content}}]}} 结构。
     */
    default String normalizeStreamChunk(String raw) {
        return raw;
    }
}
