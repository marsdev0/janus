/*
 * Copyright (c) 2026 marsdev0
 *
 * Licensed under the MIT License.
 * See the LICENSE file in the project root for full license information.
 */

package com.marsdev.janus.filter;

import com.marsdev.janus.common.ErrorCode;
import com.marsdev.janus.common.JanusException;
import com.marsdev.janus.entity.UsageLog;
import com.marsdev.janus.mapper.ReservedRecordMapper;
import com.marsdev.janus.mapper.UsageLogMapper;
import com.marsdev.janus.model.RequestContext;
import com.marsdev.janus.model.Usage;
import com.marsdev.janus.model.UsageResult;
import com.marsdev.janus.quota.QuotaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * Symmetric settlement: delta = reserved − actual (positive or negative); idempotent (reserved_records.status 0→1).
 * No bare subscribe: runs as the main-flow tail, so errors propagate.
 *
 * @author geyan
 * @date 2026/8/4
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class MeteringFilter {

    private final QuotaService quotaService;
    private final ReservedRecordMapper reservedRecordMapper;
    private final UsageLogMapper usageLogMapper;

    /**
     * Settlement:
     * 1. Mark the status of reserved_records.
     * 2. If already settled, skip — idempotency is enforced via MySQL.
     * 3. If not settled: (a) update reserved_records status, (b) adjust the Redis balance, (c) update usage_logs.
     */
    public Mono<UsageResult> settle(RequestContext ctx, Usage usage, Long channelId) {
        if (usage == null) {
            return Mono.error(new JanusException(ErrorCode.INTERNAL_ERROR));
        }
        long actual = usage.actual();
        long delta = ctx.getReserved() - actual;

        return Mono.fromCallable(() -> reservedRecordMapper.markSettle(ctx.getRequestId()))
                .flatMap(update -> {
                    if (update == 0) {
                        // No row updated; already settled
                        return Mono.just(new UsageResult(0, 0, true));
                    }
                    // Adjust balance; update usage_logs
                    return quotaService.adjust(ctx.getTokenId(), delta)
                            .then(Mono.fromRunnable(() -> recordUsage(ctx, usage, channelId)))
                            .thenReturn(new UsageResult(actual, delta, false));
                });
    }

    private void recordUsage(RequestContext ctx, Usage usage, Long channelId) {
        UsageLog u = new UsageLog();
        u.setRequestId(ctx.getRequestId());
        u.setTokenId(ctx.getTokenId());
        u.setChannelId(channelId);
        u.setModel(ctx.getModel());
        u.setPromptTokens((int) usage.getPrompt());
        u.setCompletionTokens((int) usage.getCompletion());
        u.setStatus(0);
        try {
            usageLogMapper.insert(u);
        } catch (Exception e) {
            log.error("recordUsage fail requestId={}", ctx.getRequestId(), e);
        }
    }
}
