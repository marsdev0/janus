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
            // insert(list)是伪批量插入
            // 批量插入
            usageLogMapper.insertBatch(list);
            ack.acknowledge();
        } catch (Exception e) {
            // 当上述批处理插入时，存在requestId重复，就会导致这批都插入失败，影响很大
            // 所以下面必须有兜底的处理
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
