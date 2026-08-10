/*
 * Copyright (c) 2026 marsdev0
 *
 * Licensed under the MIT License.
 * See the LICENSE file in the project root for full license information.
 */

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
