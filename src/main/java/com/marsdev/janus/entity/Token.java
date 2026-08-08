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
@TableName("token")
public class Token {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * SHA-256 hash, used for verification
     */
    private String keyHash;

    /**
     * First 8 characters of the key, for display in lists
     */
    private String keyPrefix;

    private Long userId;

    /**
     * Key name
     */
    private String name;

    /**
     * Allowed models, comma-separated; empty = all
     */
    private String models;

    /**
     * Total quota (tokens)
     */
    private Long quotaLimit;

    /**
     * Status: 1 = enabled, 0 = disabled
     */
    private Integer status;

    private LocalDateTime createdAt;

    /**
     * Expiry time; NULL = never expires
     */
    private LocalDateTime expiresAt;
}
