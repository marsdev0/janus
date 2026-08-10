/*
 * Copyright (c) 2026 marsdev0
 *
 * Licensed under the MIT License.
 * See the LICENSE file in the project root for full license information.
 */

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
     * Channel name
     */
    private String name;

    /**
     * Commercial identity: openai/azure/zhipu/ollama/vllm/anthropic/google/proxy
     */
    private String provider;

    /**
     * Protocol type: openai/claude/gemini  ← determines which ProviderAdapter to use
     */
    private String protocol;

    /**
     * Upstream base URL
     */
    private String baseUrl;

    /**
     * Channel API key (AES-encrypted at rest)
     */
    private String apiKey;

    /**
     * Supported models, comma-separated
     */
    private String models;

    /**
     * Weight
     */
    private Integer weight;

    /**
     * Priority; higher = preferred (primary/backup)
     */
    private Integer priority;

    /**
     * Status: 1 = enabled / 0 = disabled
     */
    private Integer status;

    private LocalDateTime createdAt;
}
