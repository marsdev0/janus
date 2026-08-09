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
 * 工具定义的细节（{@link Tool#getFunction()}）。是「声明」：名字 + 描述 + 参数 JSON Schema。
 *
 * <p>与 {@link FunctionCall}（调用细节：name + arguments）刻意分成两个类，
 * 在类型层面区分「定义」和「调用」，避免混用。
 *
 * @author geyan
 * @date 2026/8/9
 */
@Data
public class FunctionDef {

    /**
     * 函数名，模型根据此选择调用哪个工具
     */
    private String name;

    /**
     * 描述，告诉模型这个工具干什么，模型主要靠它判断何时调用
     */
    private String description;

    /**
     * 参数 JSON Schema，声明参数结构（如 {@code {type:object, properties:{city:{type:string}}}}）
     * 用 {@link com.fasterxml.jackson.databind.JsonNode} 保留任意嵌套的原结构
     * schema 本身是递归 JSON，强类型化不划算
     */
    private JsonNode parameters;
}
