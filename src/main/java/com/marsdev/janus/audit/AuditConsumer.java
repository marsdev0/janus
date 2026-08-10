/*
 * Copyright (c) 2026 marsdev0
 *
 * Licensed under the MIT License.
 * See the LICENSE file in the project root for full license information.
 */

package com.marsdev.janus.audit;

import com.marsdev.janus.common.util.JsonUtils;
import com.marsdev.janus.converter.AuditConverter;
import com.marsdev.janus.entity.UsageLog;
import com.marsdev.janus.mapper.UsageLogMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * @author geyan
 * @date 2026/8/9
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AuditConsumer {

    private final UsageLogMapper usageLogMapper;
    private final AuditConverter auditConverter;

    @KafkaListener(topics = AuditConst.TOPIC_AUDIT, groupId = "janus-audit-writer", batch = "true")
    public void consume(List<ConsumerRecord<String, String>> records, Acknowledgment ack) {
        List<UsageLog> list = new ArrayList<>();
        for (ConsumerRecord<String, String> r : records) {
            AuditEvent auditEvent = JsonUtils.fromJson(r.value(), AuditEvent.class);
            list.add(auditConverter.toUsageLog(auditEvent));
        }
        try {
            // insert(list) is a fake batch insert
            // batch insert
            usageLogMapper.insertBatch(list);
            ack.acknowledge();
        } catch (Exception e) {
            // If the batch insert above hits a duplicate requestId, the whole batch fails,
            // which has a big impact.
            // So a fallback is mandatory below.
            log.warn("batch insert failed, fallback to one-by-one", e);
            for (UsageLog u : list) {
                try {
                    usageLogMapper.insert(u);
                } catch (Exception ex) {
                    log.error("drop audit row req={}", u.getRequestId(), ex);
                }
                ack.acknowledge();
            }
        }
    }
}
