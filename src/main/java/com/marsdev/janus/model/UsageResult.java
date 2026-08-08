package com.marsdev.janus.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Settlement result
 *
 * @author geyan
 * @date 2026/8/2
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UsageResult {

    private long actual;

    private long delta;

    private boolean alreadySettled;
}
