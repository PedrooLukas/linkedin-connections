package com.pedro.linkedin.connections.api.dto;

public record LinkedinProfileFlagResponse(
        String code,
        String severity,
        boolean active,
        String title,
        String message
) {
}
