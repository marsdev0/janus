package com.marsdev.janus.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 用量（prompt + completion token 数）
 *
 * @author geyan
 * @date 2026/8/7
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Usage {

    private long prompt;

    private long completion;

    public long actual() {
        return this.prompt + this.completion;
    }
}
