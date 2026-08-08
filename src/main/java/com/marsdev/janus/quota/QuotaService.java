package com.marsdev.janus.quota;

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
        preConsumeScript = load("lua/quota_pre_consume.lua");
        adjustScript = load("lua/quota_adjust.lua");

    }

    private DefaultRedisScript<Long> load(String path) {
        DefaultRedisScript<Long> s = new DefaultRedisScript<>();
        s.setScriptSource(new ResourceScriptSource(new ClassPathResource(path)));
        s.setResultType(Long.class);
        return s;
    }

    /**
     * 预扣；true=放行。NOT_INIT 直接 fail-closed（靠 warmup 预热，见 QuotaBootstrap）
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
                    return r >= 0;
                });
    }

    /**
     * 对称调整：delta>0 退还，delta<0 补扣
     */
    public Mono<Long> adjust(Long tokenId, long delta) {
        return redisTemplate.execute(adjustScript,
                        List.of(quotaKey(tokenId)),
                        String.valueOf(delta))
                .next();  // 只取第一个元素
    }


    public static String quotaKey(Long tokenId) {
        return "quota:" + tokenId;
    }
}
