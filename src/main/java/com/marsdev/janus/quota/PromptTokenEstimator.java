/*
 * Copyright (c) 2026 marsdev0
 *
 * Licensed under the MIT License.
 * See the LICENSE file in the project root for full license information.
 */

package com.marsdev.janus.quota;

import com.knuddels.jtokkit.Encodings;
import com.knuddels.jtokkit.api.Encoding;
import com.knuddels.jtokkit.api.EncodingType;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

/**
 * @author geyan
 * @date 2026/8/4
 */
@Component
public class PromptTokenEstimator {

    /**
     *  From the OpenAI Cookbook:
     *  │ Every message follows this format: <|im_start|>{role/name}\n{content}<|im_end|>\n
     *  │ Each message has a constant overhead of ~4 tokens.
     */
    private static final int DEFAULT_TOKEN_COUNT = 4;

    private Encoding encoding;

    @PostConstruct
    public void init() {
        encoding = Encodings.newDefaultEncodingRegistry()
                .getEncoding(EncodingType.CL100K_BASE);
    }

    /**
     * Count the tokens of a prompt
     */
    public long estimate(String prompt) {
        if (prompt == null || prompt.isEmpty()) {
            return DEFAULT_TOKEN_COUNT;
        }
        return encoding.countTokens(prompt) + DEFAULT_TOKEN_COUNT;
    }
}
