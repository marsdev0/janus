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
 * Tool [call] — <b>response direction</b>. Appears in {@link ChatMessage#getToolCalls()} of an
 * assistant message, representing "which tool the model actually invoked".
 *
 * <p>Analogy: the dish the guest ordered. Includes the concrete argument values.
 * <p>OpenAI shape: {@code {id, type:"function", function:{name, arguments(JSON string)}}}
 *
 * @author geyan
 * @date 2026/8/9
 */
@Data
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class ToolCall {

    /**
     * The unique id of this call; the subsequent role:tool result message links back here
     * via {@link ChatMessage#getToolCallId()}
     */
    private String id;

    /**
     * Type, same as {@link Tool#getType()}; currently "function"
     */
    private String type = "function";

    /**
     * The function name + arguments of this call ({@link FunctionCall})
     */
    private FunctionCall function;
}
