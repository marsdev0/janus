package com.marsdev.janus.quota;

import com.marsdev.janus.common.utils.ScriptUtil;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scripting.support.ResourceScriptSource;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * @author geyan
 * @date 2026/8/5
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class QuotaService {

    public static final long NOT_INIT = -2L;
    public static final long INSUFFICIENT = -1L;

    private final ReactiveStringRedisTemplate redisTemplate;
    private final QuotaProperties quotaProperties;

    private DefaultRedisScript<Long> preConsumeScript;
    private DefaultRedisScript<Long> adjustScript;

    @PostConstruct
    public void init() {
        preConsumeScript = ScriptUtil.load("lua/quota_pre_consume.lua");
        adjustScript = ScriptUtil.load("lua/quota_adjust.lua");

    }

    /**
     * Reservation (pre-deduction); true = allowed. NOT_INIT fails closed (warmed up by QuotaBootstrap)
     */
    public Mono<Boolean> preConsume(Long tokenId, long reserved) {
        if (tokenId == null) {
            return Mono.just(false);
        }
        return redisTemplate.execute(preConsumeScript,
                        List.of(quotaKey(tokenId)),
                        String.valueOf(reserved),
                        String.valueOf(quotaProperties.getCreditLimit()))
                .next()
                .map(r -> {
                    if (r == NOT_INIT) {
                        log.warn("quota not initialized, tokenId={} (expect warmup)", tokenId);
                        return false;
                    }
                    return r != INSUFFICIENT;
                });
    }

    /**
     * Symmetric adjustment: delta > 0 refunds, delta < 0 charges the difference
     */
    public Mono<Long> adjust(Long tokenId, long delta) {
        return redisTemplate.execute(adjustScript,
                        List.of(quotaKey(tokenId)),
                        String.valueOf(delta))
                .next();  // take the first element only
    }


    public static String quotaKey(Long tokenId) {
        return "quota:" + tokenId;
    }
}
