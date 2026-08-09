package com.marsdev.janus.audit;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * @author geyan
 * @date 2026/8/9
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditEvent {

    private String requestId;

    private Long tokenId;

    private Long channelId;

    private String model;

    private long promptTokens;

    private long completionTokens;

    private BigDecimal cost;

    private int latencyMs;

    private int status;
}
