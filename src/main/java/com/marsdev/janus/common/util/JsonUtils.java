/*
 * Copyright (c) 2026 marsdev0
 *
 * Licensed under the MIT License.
 * See the LICENSE file in the project root for full license information.
 */

package com.marsdev.janus.common.util;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Static JSON utility for unified serialization / parsing (shared by adapter protocol
 * conversion, Redis key caching, usage chunk parsing, etc.).
 *
 * <p>Wraps a Jackson {@link ObjectMapper} singleton. Key config: disable
 * {@link DeserializationFeature#FAIL_ON_UNKNOWN_PROPERTIES} so unknown fields are ignored
 * during deserialization — upstream response fields are frequently extended/changed, and
 * ignoring unknown fields improves compatibility so a single new field won't break parsing.
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
            throw new RuntimeException("JSON serialization failed", e);
        }
    }

    public static <T> T fromJson(String json, Class<T> clazz) {
        try {
            return MAPPER.readValue(json, clazz);
        } catch (Exception e) {
            throw new RuntimeException("JSON parse failed: " + json, e);
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
