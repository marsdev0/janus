package com.marsdev.janus.filter;

import com.marsdev.janus.common.ErrorCode;
import com.marsdev.janus.common.JanusException;
import com.marsdev.janus.model.RequestContext;
import com.marsdev.janus.ratelimit.RateLimitService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * @author geyan
 * @date 2026/8/8
 */
@Component
@RequiredArgsConstructor
public class RateLimitFilter {

    private final RateLimitService rateLimitService;

    public Mono<RequestContext> check(RequestContext ctx) {
        return rateLimitService.check(ctx)
                .flatMap(v -> {
                    if (v == 0) {
                        return Mono.just(ctx);
                    } else {
                        // Over limit: return 429 directly, skip reservation and do not write reserved_record
                        return Mono.error(new JanusException(ErrorCode.RATE_LIMITED));
                    }
                });
    }
}
