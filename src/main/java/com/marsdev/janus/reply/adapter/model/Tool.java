/*
 * Copyright (c) 2026 marsdev0
 *
 * Licensed under the MIT License.
 * See the LICENSE file in the project root for full license information.
 */

package com.marsdev.janus.reply.adapter.model;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Data;

/**
 * Tool [definition] — <b>request direction</b>. Appears in {@link ChatRequest#getTools()},
 * telling the model "which tools are available".
 *
 * <p>Analogy: the dishes listed on a menu. It only declares a schema (name, parameter structure)
 * and does not include any concrete invocation.
 * <p>OpenAI shape: {@code {type:"function", function:{name, description, parameters}}}
 *
 * <p>Counterpart of {@link ToolCall} (tool invocation, response direction): Tool is the
 * "capability list", ToolCall is "one exercise of it".
 *
 * @author geyan
 * @date 2026/8/9
 */
@Data
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class Tool {

    /**
     * Type; currently only OpenAI's "function"
     */
    private String type = "function";

    /**
     * The concrete definition of the tool (name / description / parameter schema)
     */
    private FunctionDef function;
}
