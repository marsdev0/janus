package com.marsdev.janus.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Arrays;

/**
 * @author geyan
 * @date 2026/8/7
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TokenAuth {

    private Long id;

    private String models;

    private int status;

    private LocalDateTime expiresAt;

    public boolean isActive() {
        return this.status == 1;
    }

    public boolean modelAllowed(String model) {
        return Arrays.stream(this.models.split(","))
                .map(String::trim)
                .anyMatch(model::equals);
    }
}
