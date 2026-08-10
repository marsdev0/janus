/*
 * Copyright (c) 2026 marsdev0
 *
 * Licensed under the MIT License.
 * See the LICENSE file in the project root for full license information.
 */

package com.marsdev.janus.reply.adapter;

import com.fasterxml.jackson.databind.JsonNode;
import com.marsdev.janus.common.util.JsonUtils;
import com.marsdev.janus.model.Usage;
import com.marsdev.janus.reply.adapter.model.ChatRequest;
import com.marsdev.janus.reply.adapter.model.GatewayError;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * @author geyan
 * @date 2026/8/9
 */
@Component("openaiAdapter")
public class OpenAIAdapter implements ProviderAdapter {

    @Override
    public String toUpstreamReq(ChatRequest req) {
        if (req.isStream()) {
            // Streaming: inject stream_options.include_usage so the upstream returns accurate usage in the final chunk
            // (otherwise upstreamUsage is always null and every request falls back to local completionBuf estimation)
            req.getExtra().put("stream_options", Map.of("include_usage", true));
        }
        return JsonUtils.toJson(req);
    }

    @Override
    public String upstreamPath() {
        return "/chat/completions";
    }

    @Override
    public String authHeaderName() {
        return "Authorization";
    }

    @Override
    public String authHeaderValue(String apiKey) {
        return "Bearer " + apiKey;
    }

    @Override
    public Usage parseUsage(String raw) {
        JsonNode node = JsonUtils.parse(raw);
        if (node == null) {
            return null;
        }
        JsonNode u = node.path("usage");
        if (u.isMissingNode()) {
            return null;
        }
        Usage usage = new Usage();
        usage.setPrompt(u.path("prompt_tokens").asLong(0));
        usage.setCompletion(u.path("completion_tokens").asLong(0));
        return usage;
    }

    @Override
    public GatewayError normalizeError(int status, String body) {
        boolean retryable = status == 429 || status >= 500;
        return new GatewayError(status, "OPENAI_" + status, body, retryable);
    }

    @Override
    public String parseDeltaContent(String raw) {
        JsonNode node = JsonUtils.parse(raw);
        if (node == null) {
            return null;
        }
        return node.path("choices").path(0).path("delta").path("content").asText(null);
    }

    @Override
    public String fromUpstreamResp(String raw) {
        // Canonical=OpenAI, the upstream response is already in OpenAI format → pass-through (Identity)
        return raw;
    }
}
