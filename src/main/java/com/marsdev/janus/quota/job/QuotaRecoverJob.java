package com.marsdev.janus.quota.job;

import com.marsdev.janus.entity.ReservedRecord;
import com.marsdev.janus.mapper.ReservedRecordMapper;
import com.marsdev.janus.quota.QuotaProperties;
import com.marsdev.janus.quota.QuotaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.List;

/**
 * 进程 kill -9 / doFinally 未执行 → 预扣永久卡住。本 Job 分钟级扫超时未结算预扣退还
 *
 * @author geyan
 * @date 2026/8/8
 */
@Component
@RequiredArgsConstructor

@Slf4j
public class QuotaRecoverJob {

    private final ReservedRecordMapper reservedRecordMapper;
    private final QuotaProperties quotaProperties;
    private final QuotaService quotaService;

    /**
     * 1. 找出所有超时未结算的记录
     * 2. 修改其状态，改成【已超时退还】
     */
    @Scheduled(fixedDelay = 60_000L)
    public void recoverStaleReservations() {
        List<ReservedRecord> list = reservedRecordMapper.findStale(ReservedRecordMapper.PENDING, quotaProperties.getRecoverStaleMinutes());
        if (CollectionUtils.isEmpty(list)) {
            log.debug("quota recover job list is empty");
            return;
        }
        for (ReservedRecord r : list) {
            int updated = reservedRecordMapper.markTimedOut(r.getRequestId());
            if (updated > 0) {
                // 修改成功，同时需要修改redis的余额
                quotaService.adjust(r.getTokenId(), r.getReserved()).subscribe(
                        v -> {
                            log.info("recover refunded requestId={} tokenId={} reserved={}",
                                    r.getRequestId(), r.getTokenId(), r.getReserved());
                        },
                        err -> log.error("recover refund fail requestId={}", r.getRequestId(), err));
            }
        }
    }
}
