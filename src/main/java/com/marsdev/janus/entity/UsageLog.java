package com.marsdev.janus.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Usage ledger entity
 *
 * @author geyan
 * @date 2026/8/6
 */
@Data
@TableName("usage_log")
public class UsageLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * Request ID, used as the idempotency key
     */
    private String requestId;

    /**
     * Associated token.id
     */
    private Long tokenId;

    /**
     * Associated channel.id
     */
    private Long channelId;

    /**
     * Requested model name
     */
    private String model;

    /**
     * Prompt token count
     */
    private Integer promptTokens;

    /**
     * Completion token count
     */
    private Integer completionTokens;

    /**
     * Local invocation cost, in CNY
     */
    private BigDecimal cost;

    /**
     * End-to-end latency, in milliseconds
     */
    private Integer latencyMs;

    /**
     * Request status: 0 = success, 1 = failure
     */
    private Integer status;

    private LocalDateTime createdAt;
}
