package com.marsdev.janus.audit;

import com.marsdev.janus.common.util.JsonUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * @author geyan
 * @date 2026/8/9
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AuditProducer {

    private final KafkaTemplate<String, String> kafka;

    public void send(AuditEvent event) {
        try {
            kafka.send(AuditConst.TOPIC_AUDIT,
                    String.valueOf(event.getTokenId()),
                    JsonUtils.toJson(event)).whenComplete((v, ex) -> {
                if (ex != null) {
                    log.error("send kafka msg failed, tokenId: {}", event.getTokenId(), ex);
                }
            });
        } catch (Exception e) {
            log.warn("audit send failed", e);
        }
    }
}
