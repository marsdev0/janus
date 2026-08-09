/*
 * Copyright (c) 2026 marsdev0
 *
 * Licensed under the MIT License.
 * See the LICENSE file in the project root for full license information.
 */

package com.marsdev.janus.reply.adapter.model;

import lombok.Data;

/**
 * 工具调用的细节（{@link ToolCall#getFunction()}）。是「调用」：函数名 + 实参值
 * 与 {@link FunctionDef}（定义）相对
 *
 * @author geyan
 * @date 2026/8/9
 */
@Data
public class FunctionCall {

    /**
     * 被调用的函数名（对应某个 {@link FunctionDef#getName()}）
     */
    private String name;

    /**
     * 实参，<b>是 JSON 字符串</b>（不是对象）。OpenAI 规范：arguments 序列化成字符串
     * 如 {@code "{\"city\":\"Beijing\"}"}。客户端/adapter 拿到后需要再 parse 一次才能用
     */
    private String arguments;
}
