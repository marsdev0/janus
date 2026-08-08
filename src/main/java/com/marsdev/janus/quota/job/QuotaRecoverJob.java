/*
 * Copyright (c) 2026 marsdev0
 *
 * Licensed under the MIT License.
 * See the LICENSE file in the project root for full license information.
 */

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
 * Process kill -9 / doFinally not executed → reservation stuck forever. This job scans at minute-level for timed-out unsettled reservations and refunds them.
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
     * 1. Find all timed-out, unsettled records
     * 2. Update their status to [timed out and refunded]
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
                // Updated successfully; also adjust the Redis balance
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
