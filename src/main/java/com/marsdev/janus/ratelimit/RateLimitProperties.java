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
     * 滑动窗口大小，60s
     */
    private int windowSeconds = 60;

    /**
     * 单 key 每窗口最大请求数
     */
    private int tokenLimit = 100;

    /**
     * 单模型每窗口最大请求数
     */
    private int modelLimit = 50;

    /**
     * 单渠道每窗口最大请求数
     */
    private int channelLimit = 200;
}
