package com.marsdev.janus.reply;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
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
            return new Usage();
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

    /**
     * 给请求体注入 stream_options.include_usage=true（流式主动要 usage）
     */
    public String withStreamOptions(String rawBody) {
        try {
            ObjectNode root = (ObjectNode) objectMapper.readTree(rawBody);
            ObjectNode streamOptionsNode = root.has("stream_options") ? (ObjectNode) root.get("stream_options")
                    : root.putObject("stream_options");
            streamOptionsNode.put("include_usage", true);
            return objectMapper.writeValueAsString(root);
        } catch (Exception e) {
            log.warn("withStreamOptions parse fail, passthrough raw body", e);
            return rawBody;
        }
    }

    /**
     * SSE data（JSON 或 [DONE]）→ usage; 无则 null
     */
    public Usage parseUsage(String data) {
        JsonNode root = readTree(data);
        if (root == null) {
            return null;
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

    /**
     * SSE data → delta.content 文本; 无则 null
     */
    public String parseDeltaContent(String data) {
        JsonNode root = readTree(data);
        if (root == null) {
            return null;
        }
        JsonNode delta = root.path("choices").path(0).path("delta").path("content");
        return delta.isTextual() ? delta.asText() : null;
    }
}
