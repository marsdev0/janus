package com.marsdev.janus.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.marsdev.janus.entity.ReservedRecord;
import org.springframework.util.CollectionUtils;

import java.util.List;

/**
 * @author geyan
 * @date 2026/8/2
 */
public interface ReservedRecordMapper extends BaseMapper<ReservedRecord> {

    /**
     * 未结算
     */
    int PENDING = 0;

    /**
     * 已结算
     */
    int SETTLED = 1;

    /**
     * 已超时退还
     */
    int TIMEOUT_REFUNDED = 2;

    /**
     * 幂等结算：status 0→1，返回受影响行数（1=首次，0=已结算）
     */
    default int markSettle(String requestId) {
        LambdaUpdateWrapper<ReservedRecord> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(ReservedRecord::getRequestId, requestId)
                .eq(ReservedRecord::getStatus, PENDING)
                .set(ReservedRecord::getStatus, SETTLED);
        return update(updateWrapper);
    }

    /**
     * 该 token 在途预扣总和（status=0）
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
     * 找出超时未结算的在途预扣
     */
    default List<ReservedRecord> findStale(int status, long minutes) {
        LambdaQueryWrapper<ReservedRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ReservedRecord::getStatus, status)
                .apply("created_at < DATE_SUB(NOW(), INTERVAL {0} MINUTE)", minutes);
        return selectList(wrapper);
    }

    /**
     * 幂等超时退还：status 0→2
     */
    default int markTimedOut(String requestId) {
        LambdaUpdateWrapper<ReservedRecord> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(ReservedRecord::getStatus, PENDING)
                .eq(ReservedRecord::getRequestId, requestId)
                .set(ReservedRecord::getStatus, TIMEOUT_REFUNDED);
        return update(wrapper);
    }
}
