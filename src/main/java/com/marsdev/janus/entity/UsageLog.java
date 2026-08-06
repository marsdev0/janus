package com.marsdev.janus.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 用量账本 表对象
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
     * 请求id，幂等键
     */
    private String requestId;

    /**
     * 关联 token.id
     */
    private Long tokenId;

    /**
     * 关联 channel.id
     */
    private Long channelId;

    /**
     * 请求的模型名
     */
    private String model;

    /**
     * prompt token 数
     */
    private Integer promptTokens;

    /**
     * completion token 数
     */
    private Integer completionTokens;

    /**
     * 本地调用成本，单位元
     */
    private BigDecimal cost;

    /**
     * 端到端延迟，单位毫秒
     */
    private Integer latencyMs;

    /**
     * 请求状态 0成功 1失败
     */
    private Integer status;

    private LocalDateTime createdAt;
}
