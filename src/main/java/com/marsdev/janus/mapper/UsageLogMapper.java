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
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
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
            for (UsageLog u : list) {
                sum += u.getPromptTokens() + u.getCompletionTokens();
            }
        }
        return sum;
    }

    @Insert({
            "<script>",
            "INSERT INTO usage_log (request_id, token_id, channel_id, model,",
            "  prompt_tokens, completion_tokens, cost, latency_ms, status) VALUES",
            "<foreach collection='list' item='it' separator=','>",
            "  (#{it.requestId},#{it.tokenId},#{it.channelId},#{it.model},",
            "   #{it.promptTokens},#{it.completionTokens},#{it.cost},#{it.latencyMs},#{it.status})",
            "</foreach>",
            "</script>"
    })
    int insertBatch(@Param("list") List<UsageLog> list);
}
