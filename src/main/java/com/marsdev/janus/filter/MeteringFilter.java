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
 * 对称结算：delta = reserved − actual（可正可负），幂等（reserved_records.status 0→1）
 * 不裸 subscribe：作为主流尾端，错误可传播
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
     * 结算：
     * 1. 标记 reserved_records 的 status
     * 2. 如果状态是已结算，则跳过，这里用 mysql 实现幂等的
     * 3. 如果未结算，a.先更新 reserved_records 的 status；b.调整redis的额度；c.更新 usage_logs
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
                        // 没有修改，已经是结算状态
                        return Mono.just(new UsageResult(0, 0, true));
                    }
                    // 调整额度; 更新 usage_logs
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
