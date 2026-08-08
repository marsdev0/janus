/*
 * Copyright (c) 2026 marsdev0
 *
 * Licensed under the MIT License.
 * See the LICENSE file in the project root for full license information.
 */

package com.marsdev.janus.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.marsdev.janus.entity.UsageLog;
import org.springframework.util.CollectionUtils;

import java.util.List;

/**
 * @author geyan
 * @date 2026/8/2
 */
public interface UsageLogMapper extends BaseMapper<UsageLog> {

    default long sumTokensByToken(Long tokenId) {
        LambdaQueryWrapper<UsageLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UsageLog::getTokenId, tokenId);
        List<UsageLog> list = selectList(wrapper);
        long sum = 0;
        if (!CollectionUtils.isEmpty(list)) {
            for (UsageLog u: list) {
                sum += u.getPromptTokens() + u.getCompletionTokens();
            }
        }
        return sum;
    }
}
