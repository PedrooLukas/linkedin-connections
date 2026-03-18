package com.pedro.linkedin.connections.api.dto;

import java.util.List;

public record LinkedinProfileAnalysisResponse(
        long totalConnections,
        long strategicConnections,
        double strategicConnectionsRatio,
        long totalProjects,
        long totalSkills,
        List<LinkedinProfileFlagResponse> flags,
        String summary
) {
}
