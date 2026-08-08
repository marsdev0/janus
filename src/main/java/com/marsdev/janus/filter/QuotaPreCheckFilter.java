package com.marsdev.janus.filter;

import com.marsdev.janus.common.ErrorCode;
import com.marsdev.janus.common.JanusException;
import com.marsdev.janus.entity.ReservedRecord;
import com.marsdev.janus.mapper.ReservedRecordMapper;
import com.marsdev.janus.model.RequestContext;
import com.marsdev.janus.quota.PromptTokenEstimator;
import com.marsdev.janus.quota.QuotaProperties;
import com.marsdev.janus.quota.QuotaService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * Reservation (pre-deduction) service
 *
 * @author geyan
 * @date 2026/8/7
 */
@Component
@RequiredArgsConstructor
public class QuotaPreCheckFilter {

    private final QuotaProperties properties;
    private final PromptTokenEstimator estimator;
    private final QuotaService quotaService;
    private final ReservedRecordMapper reservedRecordMapper;


    public Mono<RequestContext> preCheck(RequestContext ctx) {
        // Reservation cap for the model's context tokens
        long cap = properties.completionCap(ctx.getModel());
        long completionReserved = ctx.getMaxTokens() != null ? Math.min(ctx.getMaxTokens(), cap) : cap;

        // Reservation = prompt token count + completion token count
        long reserved = estimator.estimate(ctx.getPromptContext()) + completionReserved;

        return quotaService.preConsume(ctx.getTokenId(), reserved)
                .flatMap(result ->
                        result ? Mono.fromCallable(() -> reservedRecordMapper.insert(createRecord(ctx, reserved)))
                                .thenReturn(ctx.withReserved(reserved)) : Mono.error(new JanusException(ErrorCode.QUOTA_EXCEEDED)));
    }

    private ReservedRecord createRecord(RequestContext ctx, long reserved) {
        ReservedRecord r = new ReservedRecord();
        r.setRequestId(ctx.getRequestId());
        r.setTokenId(ctx.getTokenId());
        r.setReserved(reserved);
        r.setStatus(0);
        return r;
    }
}
