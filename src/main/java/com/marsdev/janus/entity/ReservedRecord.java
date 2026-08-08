package com.marsdev.janus.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * In-flight reservation record entity
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
     * Request ID, used as the idempotency key
     */
    private String requestId;

    /**
     * Associated token.id
     */
    private Long tokenId;

    /**
     * Pre-deducted token quota (reserved)
     */
    private Long reserved;

    /**
     * Status: 0 = pending, 1 = settled, 2 = refunded on timeout
     */
    private Integer status;

    /**
     * Reservation creation time
     */
    private LocalDateTime createdAt;

    /**
     * Settlement time, written when status = 1 or 2
     */
    private LocalDateTime settledAt;
}
