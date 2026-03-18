package com.pedro.linkedin.connections.api.dto;

import java.util.List;

public record ImportResultResponse(
        int imported,
        int skipped,
        List<String> warnings
) {
}
