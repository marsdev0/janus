/*
 * Copyright (c) 2026 marsdev0
 *
 * Licensed under the MIT License.
 * See the LICENSE file in the project root for full license information.
 */

package com.marsdev.janus.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.marsdev.janus.entity.ReservedRecord;
import org.springframework.util.CollectionUtils;

import java.time.LocalDateTime;
import java.util.List;

/**
 * @author geyan
 * @date 2026/8/2
 */
public interface ReservedRecordMapper extends BaseMapper<ReservedRecord> {

    /**
     * Pending settlement
     */
    int PENDING = 0;

    /**
     * Settled
     */
    int SETTLED = 1;

    /**
     * Refunded on timeout
     */
    int TIMEOUT_REFUNDED = 2;

    /**
     * Idempotent settlement: status 0→1; returns affected rows (1 = first time, 0 = already settled)
     */
    default int markSettle(String requestId) {
        LambdaUpdateWrapper<ReservedRecord> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(ReservedRecord::getRequestId, requestId)
                .eq(ReservedRecord::getStatus, PENDING)
                .set(ReservedRecord::getStatus, SETTLED)
                .set(ReservedRecord::getSettledAt, LocalDateTime.now());
        return update(updateWrapper);
    }

    /**
     * Sum of in-flight reservations for this token (status = 0)
     */
    default long sumReservedByToken(Long tokenId, int status) {
        LambdaUpdateWrapper<ReservedRecord> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(ReservedRecord::getTokenId, tokenId);
        wrapper.eq(ReservedRecord::getStatus, status);
        List<ReservedRecord> list = selectList(wrapper);
        long sum = 0;
        if (!CollectionUtils.isEmpty(list)) {
            for (ReservedRecord r : list) {
                sum += r.getReserved();
            }
        }
        return sum;
    }

    /**
     * Find stale in-flight reservations that have not been settled
     */
    default List<ReservedRecord> findStale(int status, long minutes) {
        LambdaQueryWrapper<ReservedRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ReservedRecord::getStatus, status)
                .apply("created_at < DATE_SUB(NOW(), INTERVAL {0} MINUTE)", minutes);
        return selectList(wrapper);
    }

    /**
     * Idempotent timeout refund: status 0→2
     */
    default int markTimedOut(String requestId) {
        LambdaUpdateWrapper<ReservedRecord> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(ReservedRecord::getStatus, PENDING)
                .eq(ReservedRecord::getRequestId, requestId)
                .set(ReservedRecord::getStatus, TIMEOUT_REFUNDED)
                .set(ReservedRecord::getSettledAt, LocalDateTime.now());
        return update(wrapper);
    }
}
