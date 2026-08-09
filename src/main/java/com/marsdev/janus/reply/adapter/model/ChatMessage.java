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
 * Canonical 消息模型，对应 OpenAI messages 数组的一个元素。role 决定语义：
 * <ul>
 *   <li><b>system</b> —— 系统指令；OpenAI 放 messages 里，Claude 要提到顶层 system 字段（ClaudeAdapter 处理）</li>
 *   <li><b>user</b> —— 用户输入</li>
 *   <li><b>assistant</b> —— 模型回复，可能带 {@link #toolCalls}（模型发起的工具调用）</li>
 *   <li><b>tool</b> —— 工具执行结果回填，用 {@link #toolCallId} 关联回那次 ToolCall</li>
 * </ul>
 *
 * @author geyan
 * @date 2026/8/9
 */
@Data
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class ChatMessage {

    /**
     * 角色：system / user / assistant / tool
     */
    private String role;

    /**
     * 内容。OpenAI 里既可能是纯字符串 "hello"，也可能是多模态数组
     * {@code [{type:text,text:...},{type:image_url,image_url:{url:...}}]}
     * 先用 Object 透传保留原结构；多模态等真做再强类型化
     */
    private Object content;

    /**
     * 可选，参与者名（OpenAI 用于区分同名 role，较少见）
     */
    private String name;

    /**
     * 工具【调用】列表，只在 role=assistant 的消息里出现。
     * 模型决策调用工具时在这里返回 ToolCall（含 id + name + arguments 实参）。
     * {@link JsonInclude#NON_NULL} 避免非 assistant 消息序列化出空的 tool_calls 字段。
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private List<ToolCall> toolCalls;

    /**
     * tool 角色消息专用：关联回对应 ToolCall 的 id（模型靠它把工具结果接回对话）
     */
    private String toolCallId;

    /**
     * 拒绝内容（OpenAI 较新字段，模型拒绝执行时填充）
     */
    private String refusal;

    /**
     * 扩展字段透传，语义同 {@link ChatRequest#extra}
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
