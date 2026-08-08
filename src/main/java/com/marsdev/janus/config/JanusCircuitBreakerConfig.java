package com.marsdev.janus.config;

import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.Duration;

/**
 * @author geyan
 * @date 2026/7/31
 */
@Configuration
public class JanusCircuitBreakerConfig {

    @Bean
    public CircuitBreakerRegistry circuitBreakerRegistry() {
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
                // 错误率 > 50% 熔断
                .failureRateThreshold(50)
                // 慢调用比例
                .slowCallRateThreshold(80)
                .slowCallDurationThreshold(Duration.ofSeconds(10))
                // OPEN持续30s
                .waitDurationInOpenState(Duration.ofSeconds(30))
                // 滑动窗口20次
                .slidingWindowSize(20)
                // 至少10次才统计
                .minimumNumberOfCalls(10)
                // 半开放3个探测
                .permittedNumberOfCallsInHalfOpenState(3)
                // 忽略掉这个错误，因为业务遇到这个错误，直接返回给端上，不会重试
                .ignoreException(e -> e instanceof WebClientResponseException w
                        && w.getStatusCode().is4xxClientError()
                        && w.getStatusCode().value() != 429)
                .build();
        return CircuitBreakerRegistry.of(config);
    }
}
