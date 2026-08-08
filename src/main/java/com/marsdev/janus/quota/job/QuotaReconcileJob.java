package com.marsdev.janus.quota.job;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.marsdev.janus.entity.Token;
import com.marsdev.janus.mapper.ReservedRecordMapper;
import com.marsdev.janus.mapper.TokenMapper;
import com.marsdev.janus.mapper.UsageLogMapper;
import com.marsdev.janus.quota.QuotaProperties;
import com.marsdev.janus.quota.QuotaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * 分钟级对账：expected = quota_limit − Σusage − Σ(reserved status=0)，与 Redis 偏差超阈值则修正
 *
 * @author geyan
 * @date 2026/8/8
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class QuotaReconcileJob {

    private final TokenMapper tokenMapper;
    private final UsageLogMapper usageLogMapper;
    private final ReservedRecordMapper reservedRecordMapper;
    private final ReactiveStringRedisTemplate redisTemplate;
    private final QuotaProperties quotaProperties;


    @Scheduled(fixedDelay = 60_000L)
    public void reconcile() {
        // 1. 找出所有token
        List<Token> tokens = tokenMapper.selectList(new QueryWrapper<Token>().eq("status", TokenMapper.ENABLE));
        if (CollectionUtils.isEmpty(tokens)) {
            return;
        }
        for (Token t : tokens) {
            // 2. 计算 expected
            long limit = t.getQuotaLimit() != null ? t.getQuotaLimit() : 0L;
            long used = usageLogMapper.sumTokensByToken(t.getId());
            long inFlight = reservedRecordMapper.sumReservedByToken(t.getId(), ReservedRecordMapper.PENDING);
            long expected = limit - used - inFlight;

            String key = QuotaService.quotaKey(t.getId());
            redisTemplate.opsForValue().get(key)
                    .flatMap(actual -> {
                        long act = Long.parseLong(actual);
                        if (Math.abs(expected - act) <= quotaProperties.getReconcileThreshold()) {
                            return Mono.empty();
                        }
                        return redisTemplate.opsForValue().set(key, String.valueOf(expected))
                                .doOnSuccess(v -> log.warn(
                                        "reconcile tokenId={} redis={} expected={} → corrected",
                                        t.getId(), act, expected));
                    })
                    .subscribe(v -> {
                        },
                            err -> log.warn("reconcile skip tokenId={} (redis missing/err)", t.getId()));
        }

    }
}
