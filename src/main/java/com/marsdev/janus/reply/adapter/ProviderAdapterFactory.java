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
 * Adapter 工厂（§4.4）。按渠道的 <b>protocol</b>（openai/claude/gemini）选 adapter。
 *
 * <p>注意 protocol ≠ provider：provider 是商业身份（智谱/通义/ollama…），值很多；
 * protocol 是协议类型，值就 3 个。多个 provider 共用一个 protocol（智谱/ollama 都走 openai 协议），
 * 所以这里按 protocol 而非 provider 取 adapter。
 *
 * <p>实现技巧：Spring 会把所有 {@link ProviderAdapter} Bean 按 <b>bean 名</b>注入 {@code Map<String, ProviderAdapter>}，
 * key 是 bean 名（如 "openaiAdapter"）。所以新增协议只要加一个 {@code @Component("xxxAdapter")}，
 * 这里零改动自动收集 —— 开闭原则的落地。
 *
 * @author geyan
 * @date 2026/8/9
 */
@Component
@RequiredArgsConstructor
public class ProviderAdapterFactory {

    /**
     * Spring 自动注入：key = bean 名（openaiAdapter/claudeAdapter/geminiAdapter），value = 对应实例
     */
    private final Map<String, ProviderAdapter> adapters;


    /**
     * 按 protocol 取 adapter。
     *
     * @param protocol 渠道协议：openai / claude / gemini（来自 {@code Channel.protocol}）
     * @return 对应的 ProviderAdapter
     * @throws IllegalArgumentException protocol 不支持（说明 channel 表配了未实现的协议）
     */
    public ProviderAdapter get(String protocol) {
        ProviderAdapter a = adapters.get(protocol + "Adapter");   // openai→openaiAdapter, claude→claudeAdapter...
        if (a == null) throw new IllegalArgumentException("unsupported protocol: " + protocol);
        return a;
    }
}
