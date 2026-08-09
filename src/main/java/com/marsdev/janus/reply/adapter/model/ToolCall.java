/*
 * Copyright (c) 2026 marsdev0
 *
 * Licensed under the MIT License.
 * See the LICENSE file in the project root for full license information.
 */

package com.marsdev.janus.reply.adapter.model;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Data;

/**
 * 工具【调用】 —— <b>响应方向</b>。出现在 assistant 消息的 {@link ChatMessage#getToolCalls()} 里
 * 表示「模型实际调用了哪个工具」
 *
 * <p>类比：客人点的那道单。含具体实参值
 * <p>OpenAI 形态：{@code {id, type:"function", function:{name, arguments(JSON 字符串)}}}
 *
 * @author geyan
 * @date 2026/8/9
 */
@Data
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class ToolCall {

    /**
     * 本次调用的唯一 id；后续 role:tool 的结果消息用 {@link ChatMessage#getToolCallId()} 关联回这里
     */
    private String id;

    /**
     * 类型，同 {@link Tool#getType()}，目前 "function"
     */
    private String type = "function";

    /**
     * 本次调用的函数名 + 实参（{@link FunctionCall}）
     */
    private FunctionCall function;
}
