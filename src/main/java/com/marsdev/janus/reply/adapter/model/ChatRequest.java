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
 * Canonical 请求模型 —— janus 内部统一的 chat 请求表示
 *
 * @author geyan
 * @date 2026/8/9
 */
@Data
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class ChatRequest {

    /**
     * 模型名；路由（ChannelRouter.route）和 adapter 转换都要用
     */
    private String model;

    /**
     * 对话消息列表，role: system/user/assistant/tool；计量时要读 content 估算 token
     */
    private List<ChatMessage> messages;


    /**
     * 工具【定义】列表（请求方向，告诉模型有哪些工具可用，见 {@link Tool}
     */
    private List<Tool> tools;

    /**
     * 是否流式；影响 adapter 是否注入 stream_options、以及走 SSE 路径
     */
    private boolean stream;

    /**
     * 最大输出 token
     */
    private Integer maxTokens;

    /**
     * 采样温度，0~2，越高越随机
     */
    private Double temperature;

    /**
     * 非核心/未知字段兜底透传。
     * <p>反序列化时 Jackson 把 Canonical 不认识的字段塞进来（{@link JsonAnySetter}），
     * 序列化时合并回顶层（{@link JsonAnyGetter}）。这样 OpenAI 新增字段无需改 Canonical 即可透传（§2.5）。
     * <p>LinkedHashMap 保持插入顺序，保证序列化后字段顺序稳定（便于排查/测试）。
     */
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private Map<String, Object> extra = new LinkedHashMap<>();

    /**
     * Jackson 反序列化钩子：遇到未声明的字段 → 存进 extra，而不是报错或丢弃
     */
    @JsonAnySetter
    public void addExtra(String key, Object value) {
        extra.put(key, value);
    }

    /**
     * Jackson 序列化钩子：把 extra 的 entry 作为顶层字段输出（与核心字段合并成完整请求体）
     */
    @JsonAnyGetter
    public Map<String, Object> getExtra() {
        return extra;
    }
}
