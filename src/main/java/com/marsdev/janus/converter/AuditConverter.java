package com.marsdev.janus.converter;

import com.marsdev.janus.audit.AuditEvent;
import com.marsdev.janus.entity.UsageLog;
import org.mapstruct.Mapper;

/**
 * @author geyan
 * @date 2026/8/9
 */
@Mapper(componentModel = "spring")
public interface AuditConverter {

    UsageLog toUsageLog(AuditEvent event);
}
