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
 * 工具【定义】 —— <b>请求方向</b>。出现在 {@link ChatRequest#getTools()} 里，告诉模型「有哪些工具可用」。
 *
 * <p>类比：菜单上列出的菜。只声明 schema（叫什么、参数结构），不包含具体调用。
 * <p>OpenAI 形态：{@code {type:"function", function:{name, description, parameters}}}
 *
 * <p>与 {@link ToolCall}（工具调用，响应方向）相对：Tool 是「能力清单」，ToolCall 是「一次行使」。
 *
 * @author geyan
 * @date 2026/8/9
 */
@Data
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class Tool {

    /**
     * 类型，目前 OpenAI 仅有 "function"
     */
    private String type = "function";

    /**
     * 工具的具体定义（名字 / 描述 / 参数 schema）
     */
    private FunctionDef function;
}
