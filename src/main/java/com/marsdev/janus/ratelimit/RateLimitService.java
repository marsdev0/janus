/*
 * Copyright (c) 2026 marsdev0
 *
 * Licensed under the MIT License.
 * See the LICENSE file in the project root for full license information.
 */

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
     * Check whether the API key / model exceeds the rate limit
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
     * Channel-level rate limiting
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
