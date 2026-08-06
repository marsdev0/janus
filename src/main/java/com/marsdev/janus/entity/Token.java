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
     * SHA256，用于校验比对
     */
    private String keyHash;

    /**
     * key的前8位，用于列表展示
     */
    private String keyPrefix;

    private Long userId;

    /**
     * Key名称
     */
    private String name;

    /**
     * 允许的模型,逗号分隔,空=全部
     */
    private String models;

    /**
     * 总额度(token)
     */
    private Long quotaLimit;

    /**
     * 状态:1启用 0禁用
     */
    private Integer status;

    private LocalDateTime createdAt;

    /**
     * 过期时间,NULL=永久有效
     */
    private LocalDateTime expiresAt;
}
