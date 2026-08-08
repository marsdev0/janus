/*
 * Copyright (c) 2026 marsdev0
 *
 * Licensed under the MIT License.
 * See the LICENSE file in the project root for full license information.
 */

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
