package com.marsdev.janus.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * @author geyan
 * @date 2026/7/31
 */
@Data
@Component
@ConfigurationProperties(prefix = "janus")
public class JanusProperties {

    private Upstream upstream = new Upstream();

    @Data
    public static class Upstream {

        private String baseUrl;

        private String apiKey;

        private Duration timeout = Duration.ofSeconds(60);
    }
}
