package com.marsdev.janus.reply;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marsdev.janus.model.Usage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 解析 SSE / JSON 响应里的 usage 与 delta；注入 stream_options
 *
 * @author geyan
 * @date 2026/8/5
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class UsageParser {

    private final ObjectMapper objectMapper;

    /**
     * 非流式 JSON 响应 → usage；无则 zero
     */
    public Usage parseUsageFromJson(String resp) {
        JsonNode root = readTree(resp);
        if (root == null) {
            return new Usage();
        }
        JsonNode u = root.path("usage");
        if (u.isMissingNode() || u.isNull()) {
            return null;
        }
        Usage usage = new Usage();
        usage.setPrompt(u.path("prompt_tokens").asLong(0));
        usage.setCompletion(u.path("completion_tokens").asLong(0));
        return usage;
    }

    private JsonNode readTree(String s) {
        if (s == null || "[DONE]".equals(s.trim())) {
            return null;
        }
        try {
            return objectMapper.readTree(s);
        } catch (Exception e) {
            return null;
        }
    }
}
