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
    // 通用
    BAD_REQUEST(400, 40000, "请求参数错误"),
    UNAUTHORIZED(401, 40100, "未授权：缺少或无效的 API Key"),
    FORBIDDEN(403, 40300, "禁止访问"),
    NOT_FOUND(404, 40400, "资源不存在"),
    INTERNAL_ERROR(500, 50000, "系统内部错误"),

    // 渠道 / 路由 / Failover
    NO_CHANNEL(404, 40401, "无可用渠道支持该模型"),
    ALL_CIRCUITS_OPEN(503, 50301, "所有渠道均已熔断，请稍后重试"),
    ALL_CHANNELS_FAILED(502, 50201, "所有渠道均请求失败"),
    UNSUPPORTED_PROVIDER(400, 40002, "不支持的渠道类型"),

    // 配额 / Key
    QUOTA_EXCEEDED(402, 40201, "额度不足"),
    TOKEN_EXPIRED(401, 40102, "API Key 已过期"),
    TOKEN_DISABLED(401, 40103, "API Key 已禁用"),
    MODEL_NOT_ALLOWED(403, 40301, "该 Key 无权访问此模型"),

    // 限流
    RATE_LIMITED(429, 42901, "请求过于频繁，请稍后再试"),

    ;

    private final int httpStatus;

    private final int errCode;

    private final String errMsg;
}
