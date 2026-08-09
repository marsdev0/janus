/*
 * Copyright (c) 2026 marsdev0
 *
 * Licensed under the MIT License.
 * See the LICENSE file in the project root for full license information.
 */

package com.marsdev.janus.reply.adapter.model;

import lombok.Getter;

/**
 * 归一化后的上游错误。
 *
 * <p>各 adapter 的 {@code normalizeError} 把厂商错误码映射成它，UpstreamProxy 据此决定是否 failover。
 *
 * <p>继承 {@link RuntimeException}：可以直接作为响应式链路的 error 信号抛出（{@code Mono.error(...)}），
 * 在 {@code onErrorResume} 里用 {@code instanceof GatewayError} 捕获。
 *
 * @author geyan
 * @date 2026/8/9
 */
@Getter
public class GatewayError extends RuntimeException {

    /**
     * 原始 HTTP 状态码（4xx / 5xx）
     */
    private final int status;

    /**
     * 归一化错误码（如 OPENAI_429 / CLAUDE_529 / GEMINI_500），便于日志和监控归类
     */
    private final String code;

    /**
     * 上游返回的原始响应体，便于排查（保留原文不解析）
     */
    private final String rawBody;

    /**
     * 是否可重试 —— 决定是否触发 failover：
     * <ul>
     *   <li><b>true</b>：429 限流 / 5xx 服务端错误 / 超时 —— 换个渠道可能成功，应 failover</li>
     *   <li><b>false</b>：400 请求格式错 / 401 鉴权失败 —— 换渠道也一样失败，直接返回不重试</li>
     * </ul>
     * UpstreamProxy 的 onErrorResume 读这个字段决定走向。
     */
    private final boolean retryable;

    public GatewayError(int status, String code, String rawBody, boolean retryable) {
        super(code + " (" + status + "): " + rawBody);   // 作为异常 message，便于日志直接打印
        this.status = status;
        this.code = code;
        this.rawBody = rawBody;
        this.retryable = retryable;
    }
}
