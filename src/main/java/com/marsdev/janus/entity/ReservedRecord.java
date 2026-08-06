package com.marsdev.janus.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 在途预扣记录 表对象
 *
 * @author geyan
 * @date 2026/8/7
 */
@Data
@TableName("reserved_record")
public class ReservedRecord {

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
     * token预扣额度
     */
    private Long reserved;

    /**
     * 0未结算 1已结算 2已超时退还
     */
    private Integer status;

    /**
     * 预扣创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 结算时间，当status=1 or 2时写入
     */
    private LocalDateTime settledAt;
}
