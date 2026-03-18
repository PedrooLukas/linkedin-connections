package com.pedro.linkedin.connections.api.dto;

import java.util.List;

public record ConnectionStatsResponse(
        long totalConnections,
        List<StatsCountItemResponse> topCompanies,
        List<StatsCountItemResponse> topPositions,
        List<StatsCountItemResponse> companyDistribution
) {
}
