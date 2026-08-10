/*
 * Copyright (c) 2026 marsdev0
 *
 * Licensed under the MIT License.
 * See the LICENSE file in the project root for full license information.
 */

package com.marsdev.janus.reply.adapter;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Adapter factory (§4.4). Selects an adapter by the channel's <b>protocol</b>
 * (openai/claude/gemini).
 *
 * <p>Note: protocol ≠ provider. provider is the commercial identity (Zhipu/Tongyi/ollama…)
 * with many possible values; protocol is the protocol type with only 3 values. Multiple
 * providers share one protocol (Zhipu/ollama both use the openai protocol), so adapters are
 * looked up by protocol rather than by provider.
 *
 * <p>Implementation trick: Spring injects all {@link ProviderAdapter} beans by <b>bean name</b>
 * into a {@code Map<String, ProviderAdapter>}, where the key is the bean name (e.g. "openaiAdapter").
 * So adding a new protocol only requires a new {@code @Component("xxxAdapter")} — this class
 * collects them automatically with zero changes, which is how the open-closed principle is realized.
 *
 * @author geyan
 * @date 2026/8/9
 */
@Component
@RequiredArgsConstructor
public class ProviderAdapterFactory {

    /**
     * Auto-injected by Spring: key = bean name (openaiAdapter/claudeAdapter/geminiAdapter),
     * value = the corresponding instance
     */
    private final Map<String, ProviderAdapter> adapters;


    /**
     * Get the adapter by protocol.
     *
     * @param protocol channel protocol: openai / claude / gemini (from {@code Channel.protocol})
     * @return the corresponding ProviderAdapter
     * @throws IllegalArgumentException when the protocol is unsupported (means the channel table
     *                                  was configured with an unimplemented protocol)
     */
    public ProviderAdapter get(String protocol) {
        ProviderAdapter a = adapters.get(protocol + "Adapter");   // openai→openaiAdapter, claude→claudeAdapter...
        if (a == null) throw new IllegalArgumentException("unsupported protocol: " + protocol);
        return a;
    }
}
