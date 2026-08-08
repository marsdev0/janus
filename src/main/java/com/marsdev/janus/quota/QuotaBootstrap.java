package com.marsdev.janus.quota;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.marsdev.janus.entity.Token;
import com.marsdev.janus.mapper.ReservedRecordMapper;
import com.marsdev.janus.mapper.TokenMapper;
import com.marsdev.janus.mapper.UsageLogMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * 配额真相源：MySQL（usage_logs 账本 + reserved_records 在途）。Redis 为热路径，可重建
 * <p>
 * 定时刷新redis的余额
 *
 * @author geyan
 * @date 2026/8/7
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class QuotaBootstrap {

    private final TokenMapper tokenMapper;
    private final UsageLogMapper usageLogMapper;
    private final ReservedRecordMapper reservedRecordMapper;
    private final ReactiveStringRedisTemplate redisTemplate;

    /**
     * quota = quota_limit − Σusage − Σ(reserved status=0)
     */
    public Mono<Void> rebuildQuota(Token t) {
        if (t == null) {
            return Mono.empty();
        }
        Long tokenId = t.getId();
        long limit = t.getQuotaLimit() != null ? t.getQuotaLimit() : 0L;
        long used = usageLogMapper.sumTokensByToken(tokenId);
        long inFlight = reservedRecordMapper.sumReservedByToken(tokenId, ReservedRecordMapper.PENDING);
        long balance = limit - used - inFlight;
        log.info("rebuildQuota tokenId={} limit={} used={} inFlight={} → balance={}",
                tokenId, limit, used, inFlight, balance);

        return redisTemplate.opsForValue()
                .set(QuotaService.quotaKey(tokenId), String.valueOf(balance))
                .then();
    }

    /**
     * 启动 5s 后预热所有启用 token，之后每 5min 兜底刷新
     */
    @Scheduled(initialDelay = 5_000L, fixedDelay = 300_000L)
    public void warmup() {
        List<Token> list = tokenMapper.selectList(new QueryWrapper<Token>().eq("status", 1));
        if (!CollectionUtils.isEmpty(list)) {
            for (Token t : list) {
                rebuildQuota(t).subscribe(
                        v -> {},
                        err -> log.error("warmup fail tokenId={}", t.getId(), err));
            }
        }

    }
}
