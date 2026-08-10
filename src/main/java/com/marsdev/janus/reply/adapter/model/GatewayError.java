/*
 * Copyright (c) 2026 marsdev0
 *
 * Licensed under the MIT License.
 * See the LICENSE file in the project root for full license information.
 */

package com.marsdev.janus.reply.adapter.model;

import lombok.Getter;

/**
 * Normalized upstream error.
 *
 * <p>Each adapter's {@code normalizeError} maps the vendor error code onto this, and
 * UpstreamProxy decides whether to failover based on it.
 *
 * <p>Extends {@link RuntimeException}: can be thrown directly as an error signal in a reactive
 * chain ({@code Mono.error(...)}), and caught with {@code instanceof GatewayError} inside
 * {@code onErrorResume}.
 *
 * @author geyan
 * @date 2026/8/9
 */
@Getter
public class GatewayError extends RuntimeException {

    /**
     * Original HTTP status code (4xx / 5xx)
     */
    private final int status;

    /**
     * Normalized error code (e.g. OPENAI_429 / CLAUDE_529 / GEMINI_500), convenient for log/metrics grouping
     */
    private final String code;

    /**
     * The raw response body returned by the upstream, kept for troubleshooting (kept as-is, not parsed)
     */
    private final String rawBody;

    /**
     * Whether retryable — decides whether to trigger failover:
     * <ul>
     *   <li><b>true</b>: 429 rate limit / 5xx server error / timeout — another channel might succeed, so failover</li>
     *   <li><b>false</b>: 400 bad request / 401 auth failure — another channel would fail the same way, so return directly without retry</li>
     * </ul>
     * UpstreamProxy's onErrorResume reads this field to decide what to do.
     */
    private final boolean retryable;

    public GatewayError(int status, String code, String rawBody, boolean retryable) {
        super(code + " (" + status + "): " + rawBody);   // used as the exception message so logs print it directly
        this.status = status;
        this.code = code;
        this.rawBody = rawBody;
        this.retryable = retryable;
    }
}
