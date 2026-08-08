package com.marsdev.janus.ratelimit;

import com.marsdev.janus.common.utils.ScriptUtil;
import com.marsdev.janus.model.RequestContext;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * @author geyan
 * @date 2026/8/8
 */
@Component
@RequiredArgsConstructor
public class RateLimitService {

    private final ReactiveStringRedisTemplate redisTemplate;
    private final RateLimitProperties properties;

    private DefaultRedisScript<Long> ratelimitScript;

    @PostConstruct
    public void init() {
        ratelimitScript = ScriptUtil.load("lua/ratelimit.lua");
    }

    /**
     * 检查 key、model 是否超限
     */
    public Mono<Integer> check(RequestContext ctx) {
        List<String> keys = List.of("rl:token:" + ctx.getTokenId(), "rl:model:" + ctx.getModel());

        String thresholds = properties.getTokenLimit() + "," + properties.getModelLimit();
        return redisTemplate.execute(
                ratelimitScript,
                keys,
                String.valueOf(properties.getWindowSeconds()),
                thresholds,
                String.valueOf(System.currentTimeMillis()),
                ctx.getRequestId()).next().map(Long::intValue);
    }

    /**
     * 渠道级限流
     */
    public Mono<Integer> checkChannel(RequestContext ctx, Long channelId) {
        List<String> keys = List.of("rl:channel:" + channelId);
        return redisTemplate.execute(
                ratelimitScript,
                keys,
                String.valueOf(properties.getWindowSeconds()),
                String.valueOf(properties.getChannelLimit()),
                String.valueOf(System.currentTimeMillis()),
                ctx.getRequestId()).next().map(Long::intValue);
    }
}
