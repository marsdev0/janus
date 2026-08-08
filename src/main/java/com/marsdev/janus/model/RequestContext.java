/*
 * Copyright (c) 2026 marsdev0
 *
 * Licensed under the MIT License.
 * See the LICENSE file in the project root for full license information.
 */

package com.marsdev.janus.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request context shared across the filter chain
 *
 * @author geyan
 * @date 2026/8/7
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RequestContext {

    private String requestId;

    private String rawBody;

    private String model;

    private String promptContext;

    private Integer maxTokens;

    private TokenAuth tokenAuth;

    private long reserved;

    public Long getTokenId() {
        return this.tokenAuth != null ? this.tokenAuth.getId() : null;
    }

    public RequestContext withReserved(long reserved) {
        this.reserved = reserved;
        return this;
    }
}
