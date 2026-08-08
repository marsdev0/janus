package com.marsdev.janus.ratelimit;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * @author geyan
 * @date 2026/8/8
 */
@Data
@Component
@ConfigurationProperties(prefix = "janus.ratelimit")
public class RateLimitProperties {

    /**
     * Sliding window size in seconds, default 60
     */
    private int windowSeconds = 60;

    /**
     * Max requests per API key per window
     */
    private int tokenLimit = 100;

    /**
     * Max requests per model per window
     */
    private int modelLimit = 50;

    /**
     * Max requests per channel per window
     */
    private int channelLimit = 200;
}
