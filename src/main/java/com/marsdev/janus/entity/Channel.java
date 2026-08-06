package com.marsdev.janus.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * @author geyan
 * @date 2026/8/6
 */
@Data
@TableName("channel")
public class Channel {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 渠道名
     */
    private String name;

    /**
     * 上游厂商/平台: openai/azure/zhipu/ollama/vllm/anthropic/google/proxy
     */
    private String provider;

    /**
     * 上游基础地址
     */
    private String baseUrl;

    /**
     * 渠道密钥（AES 加密存储）
     */
    private String apiKey;

    /**
     * 支持的模型，逗号分隔
     */
    private String models;

    /**
     * 权重
     */
    private Integer weight;

    /**
     * 优先级，越大越优先（主备）
     */
    private Integer priority;

    /**
     * 状态：1 启用 / 0 禁用
     */
    private Integer status;

    private LocalDateTime createdAt;
}
