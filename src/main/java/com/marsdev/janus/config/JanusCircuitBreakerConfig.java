/*
 * Copyright (c) 2026 marsdev0
 *
 * Licensed under the MIT License.
 * See the LICENSE file in the project root for full license information.
 */

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
                // Trip the circuit when failure rate > 50%
                .failureRateThreshold(50)
                // Slow call rate threshold
                .slowCallRateThreshold(80)
                .slowCallDurationThreshold(Duration.ofSeconds(10))
                // Stay OPEN for 30s
                .waitDurationInOpenState(Duration.ofSeconds(30))
                // Sliding window of 20 calls
                .slidingWindowSize(20)
                // Require at least 10 calls before evaluating
                .minimumNumberOfCalls(10)
                // 3 probe calls in half-open state
                .permittedNumberOfCallsInHalfOpenState(3)
                // Ignore these errors: the business returns them directly to the client without retry
                .ignoreException(e -> e instanceof WebClientResponseException w
                        && w.getStatusCode().is4xxClientError()
                        && w.getStatusCode().value() != 429)
                .build();
        return CircuitBreakerRegistry.of(config);
    }
}
