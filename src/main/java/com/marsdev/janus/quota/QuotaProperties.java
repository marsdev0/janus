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
     * 透支额度，允许透支多少token数
     */
    private long creditLimit = 2000;

    /**
     * 预扣超时阈值，即预扣之后，多少还没结算，单位：分钟
     */
    private long recoverStaleMinutes = 10;

    /**
     * 对账修正阈值(token)
     */
    private long reconcileThreshold = 100;


    private Map<String, Long> modelCompletionCap = new HashMap<>();

    public long completionCap(String model) {
        return modelCompletionCap.getOrDefault(model,
                modelCompletionCap.getOrDefault("default", 2048L));
    }
}
