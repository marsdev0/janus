/*
 * Copyright (c) 2026 marsdev0
 *
 * Licensed under the MIT License.
 * See the LICENSE file in the project root for full license information.
 */

package com.marsdev.janus.reply.adapter.model;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;

/**
 * Details of a tool definition ({@link Tool#getFunction()}). It is a "declaration": name +
 * description + parameter JSON Schema.
 *
 * <p>Deliberately split from {@link FunctionCall} (call details: name + arguments) into two
 * separate classes, distinguishing "definition" and "call" at the type level to avoid misuse.
 *
 * @author geyan
 * @date 2026/8/9
 */
@Data
public class FunctionDef {

    /**
     * Function name; the model uses it to choose which tool to invoke
     */
    private String name;

    /**
     * Description telling the model what this tool does; the model relies mainly on it to decide when to call
     */
    private String description;

    /**
     * Parameter JSON Schema declaring the parameter structure (e.g. {@code {type:object, properties:{city:{type:string}}}}).
     * Uses {@link com.fasterxml.jackson.databind.JsonNode} to preserve the original arbitrarily-nested structure;
     * the schema itself is recursive JSON, so strongly typing it is not worth it.
     */
    private JsonNode parameters;
}
