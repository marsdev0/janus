package com.marsdev.janus.quota;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * @author geyan
 * @date 2026/8/4
 */
@Data
@Component
@ConfigurationProperties(prefix = "janus.quota")
public class QuotaProperties {

    /**
     * Overdraft credit limit: the number of tokens that may be overdrawn
     */
    private long creditLimit = 2000;

    /**
     * Reservation timeout threshold: how long an unsettled reservation is considered stale, in minutes
     */
    private long recoverStaleMinutes = 10;

    /**
     * Reconciliation correction threshold (tokens)
     */
    private long reconcileThreshold = 100;


    private Map<String, Long> modelCompletionCap = new HashMap<>();

    public long completionCap(String model) {
        return modelCompletionCap.getOrDefault(model,
                modelCompletionCap.getOrDefault("default", 2048L));
    }
}
