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
        // 1. 查询所有支持该model且已经启用的channel
        List<Channel> all = channelMapper.findEnableByModel(model);
        if (all.isEmpty()) {
            throw new JanusException(ErrorCode.NO_CHANNEL);
        }
        // 2. 过滤掉断路器已经Open的
        List<Channel> healthy = all.stream().filter(c -> !isCircuitOpen(c.getId())).toList();
        if (healthy.isEmpty()) {
            throw new JanusException(ErrorCode.ALL_CIRCUITS_OPEN);
        }
        // TODO@geyan 按优先级分组，最高优先级组内加权随机排序
        return healthy;
    }

    private boolean isCircuitOpen(Long channelId) {
        String name = circuitName(channelId);
        // 先复用已注册的断路器；渠道首次出现时按默认配置创建
        CircuitBreaker cb = cbRegistry.find(name).orElseGet(() -> cbRegistry.circuitBreaker(name));
        return cb.getState() == CircuitBreaker.State.OPEN;
    }

    private static String circuitName(Long channelId) {
        return "channel-" + channelId;
    }
}
