/*
 * Copyright (c) 2026 marsdev0
 *
 * Licensed under the MIT License.
 * See the LICENSE file in the project root for full license information.
 */

package com.marsdev.janus.reply.adapter.model;

import lombok.Data;

/**
 * Details of a tool call ({@link ToolCall#getFunction()}). It is a "call": function name + argument values.
 * Counterpart of {@link FunctionDef} (definition).
 *
 * @author geyan
 * @date 2026/8/9
 */
@Data
public class FunctionCall {

    /**
     * The invoked function name (corresponding to some {@link FunctionDef#getName()})
     */
    private String name;

    /**
     * Arguments, <b>as a JSON string</b> (not an object). Per the OpenAI spec, arguments are
     * serialized into a string such as {@code "{\"city\":\"Beijing\"}"}. The client/adapter must
     * parse it once more before using it.
     */
    private String arguments;
}
