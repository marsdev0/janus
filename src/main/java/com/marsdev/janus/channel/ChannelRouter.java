package com.marsdev.janus.channel;

import com.marsdev.janus.common.ErrorCode;
import com.marsdev.janus.common.JanusException;
import com.marsdev.janus.entity.Channel;
import com.marsdev.janus.mapper.ChannelMapper;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * @author geyan
 * @date 2026/7/31
 */
@Component
@RequiredArgsConstructor
public class ChannelRouter {

    private final ChannelMapper channelMapper;
    private final CircuitBreakerRegistry cbRegistry;

    public List<Channel> route(String model) {
        // 1. Query all enabled channels that support this model
        List<Channel> all = channelMapper.findEnableByModel(model);
        if (all.isEmpty()) {
            throw new JanusException(ErrorCode.NO_CHANNEL);
        }
        // 2. Filter out channels whose circuit breaker is open
        List<Channel> healthy = all.stream().filter(c -> !isCircuitOpen(c.getId())).toList();
        if (healthy.isEmpty()) {
            throw new JanusException(ErrorCode.ALL_CIRCUITS_OPEN);
        }
        // TODO@geyan Group by priority, then weighted-random sort within the highest-priority group
        return healthy;
    }

    private boolean isCircuitOpen(Long channelId) {
        String name = circuitName(channelId);
        // Reuse the registered circuit breaker; create one with default config the first time the channel is seen
        CircuitBreaker cb = cbRegistry.find(name).orElseGet(() -> cbRegistry.circuitBreaker(name));
        return cb.getState() == CircuitBreaker.State.OPEN;
    }

    private static String circuitName(Long channelId) {
        return "channel-" + channelId;
    }
}
