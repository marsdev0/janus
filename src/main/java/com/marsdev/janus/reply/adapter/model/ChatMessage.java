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
 * Canonical message model, corresponding to one element of the OpenAI messages array. role
 * determines the semantics:
 * <ul>
 *   <li><b>system</b> — system instruction; OpenAI puts it in messages, Claude hoists it to a
 *       top-level system field (handled by ClaudeAdapter)</li>
 *   <li><b>user</b> — user input</li>
 *   <li><b>assistant</b> — model reply, possibly carrying {@link #toolCalls} (tool invocations initiated by the model)</li>
 *   <li><b>tool</b> — tool execution result backfill, linked back to that ToolCall via {@link #toolCallId}</li>
 * </ul>
 *
 * @author geyan
 * @date 2026/8/9
 */
@Data
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class ChatMessage {

    /**
     * Role: system / user / assistant / tool
     */
    private String role;

    /**
     * Content. In OpenAI it can be either a plain string "hello" or a multimodal array
     * {@code [{type:text,text:...},{type:image_url,image_url:{url:...}}]}.
     * Passed through as Object for now to preserve the original structure; strongly type it
     * once multimodal is actually implemented.
     */
    private Object content;

    /**
     * Optional, participant name (OpenAI uses it to distinguish same-name roles; rare)
     */
    private String name;

    /**
     * Tool [call] list, only present in role=assistant messages.
     * When the model decides to invoke a tool it returns a ToolCall here (with id + name + argument values).
     * {@link JsonInclude#NON_NULL} prevents serializing an empty tool_calls field on non-assistant messages.
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private List<ToolCall> toolCalls;

    /**
     * For tool-role messages only: links back to the id of the corresponding ToolCall (the model uses it to attach the tool result back to the conversation)
     */
    private String toolCallId;

    /**
     * Refusal content (a newer OpenAI field, filled when the model refuses to execute)
     */
    private String refusal;

    /**
     * Extension field pass-through, same semantics as {@link ChatRequest#extra}
     */
    private Map<String, Object> extra = new LinkedHashMap<>();

    @JsonAnySetter
    public void addExtra(String key, Object value) {
        extra.put(key, value);
    }

    @JsonAnyGetter
    public Map<String, Object> getExtra() {
        return extra;
    }
}
