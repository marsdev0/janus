package com.marsdev.janus.common;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @author geyan
 * @date 2026/7/31
 */
@Getter
@AllArgsConstructor
public enum ErrorCode {
    // Generic
    BAD_REQUEST(400, 40000, "Bad request"),
    UNAUTHORIZED(401, 40100, "Unauthorized: missing or invalid API key"),
    FORBIDDEN(403, 40300, "Forbidden"),
    NOT_FOUND(404, 40400, "Resource not found"),
    INTERNAL_ERROR(500, 50000, "Internal server error"),

    // Channel / Routing / Failover
    NO_CHANNEL(404, 40401, "No channel available for this model"),
    ALL_CIRCUITS_OPEN(503, 50301, "All channels tripped (circuit open), please retry later"),
    ALL_CHANNELS_FAILED(502, 50201, "All channels failed"),
    UNSUPPORTED_PROVIDER(400, 40002, "Unsupported provider"),

    // Quota / API Key
    QUOTA_EXCEEDED(402, 40201, "Quota exceeded"),
    TOKEN_EXPIRED(401, 40102, "API key expired"),
    TOKEN_DISABLED(401, 40103, "API key disabled"),
    MODEL_NOT_ALLOWED(403, 40301, "This API key is not allowed to access this model"),

    // Rate limiting
    RATE_LIMITED(429, 42901, "Too many requests, please retry later"),

    ;

    private final int httpStatus;

    private final int errCode;

    private final String errMsg;
}
