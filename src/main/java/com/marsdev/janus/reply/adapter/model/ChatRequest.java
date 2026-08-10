/*
 * Copyright (c) 2026 marsdev0
 *
 * Licensed under the MIT License.
 * See the LICENSE file in the project root for full license information.
 */

package com.marsdev.janus.reply.adapter.model;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Data;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Canonical request model — janus's unified internal representation of a chat request.
 *
 * @author geyan
 * @date 2026/8/9
 */
@Data
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class ChatRequest {

    /**
     * Model name; needed by both routing (ChannelRouter.route) and adapter conversion
     */
    private String model;

    /**
     * Conversation message list, role: system/user/assistant/tool;
     * metering reads content to estimate tokens
     */
    private List<ChatMessage> messages;


    /**
     * Tool [definition] list (request direction, tells the model which tools are available; see {@link Tool})
     */
    private List<Tool> tools;

    /**
     * Whether streaming; affects whether the adapter injects stream_options and whether the SSE path is taken
     */
    private boolean stream;

    /**
     * Maximum output tokens
     */
    private Integer maxTokens;

    /**
     * Sampling temperature, 0~2; the higher the more random
     */
    private Double temperature;

    /**
     * Fallback pass-through for non-core / unknown fields.
     * <p>On deserialization Jackson puts any fields the Canonical doesn't recognize in here
     * ({@link JsonAnySetter}), and merges them back to the top level on serialization
     * ({@link JsonAnyGetter}). This way new OpenAI fields can pass through without changing
     * the Canonical (§2.5).
     * <p>LinkedHashMap preserves insertion order so the serialized field order stays stable
     * (easier to inspect/test).
     */
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private Map<String, Object> extra = new LinkedHashMap<>();

    /**
     * Jackson deserialization hook: an undeclared field → stored in extra, instead of erroring or being dropped
     */
    @JsonAnySetter
    public void addExtra(String key, Object value) {
        extra.put(key, value);
    }

    /**
     * Jackson serialization hook: emits extra entries as top-level fields (merged with the core fields into the full request body)
     */
    @JsonAnyGetter
    public Map<String, Object> getExtra() {
        return extra;
    }
}
