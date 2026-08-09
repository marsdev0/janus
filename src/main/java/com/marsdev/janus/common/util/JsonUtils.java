package com.marsdev.janus.common.util;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * 静态 JSON 工具，统一序列化 / 解析（适配器协议转换、Redis Key 缓存、usage chunk 解析等共用）。
 *
 * <p>封装 Jackson {@link ObjectMapper} 为单例。关键配置：关闭
 * {@link DeserializationFeature#FAIL_ON_UNKNOWN_PROPERTIES}，反序列化时忽略未知字段——
 * 上游响应字段经常扩展/变动，忽略未知字段可提升兼容性，避免一个新字段打爆解析。
 *
 * @author geyan
 * @date 2026/8/9
 */
public final class JsonUtils {

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    private JsonUtils() {
    }

    public static String toJson(Object o) {
        try {
            return MAPPER.writeValueAsString(o);
        } catch (Exception e) {
            throw new RuntimeException("JSON 序列化失败", e);
        }
    }

    public static <T> T fromJson(String json, Class<T> clazz) {
        try {
            return MAPPER.readValue(json, clazz);
        } catch (Exception e) {
            throw new RuntimeException("JSON 解析失败: " + json, e);
        }
    }

    public static JsonNode parse(String json) {
        try {
            return MAPPER.readTree(json);
        } catch (Exception e) {
            return null;
        }
    }


}
