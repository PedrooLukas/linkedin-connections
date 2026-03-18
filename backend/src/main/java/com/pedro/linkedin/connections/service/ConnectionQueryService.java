package com.pedro.linkedin.connections.service;

import com.pedro.linkedin.connections.api.dto.ConnectionResponse;
import com.pedro.linkedin.connections.api.dto.ConnectionStatsResponse;
import com.pedro.linkedin.connections.api.dto.LinkedinProfileAnalysisResponse;
import com.pedro.linkedin.connections.api.dto.LinkedinProfileFlagResponse;
import com.pedro.linkedin.connections.api.dto.StatsCountItemResponse;
import com.pedro.linkedin.connections.domain.Connection;
import com.pedro.linkedin.connections.repository.ConnectionCountProjection;
import com.pedro.linkedin.connections.repository.ConnectionRepository;
import com.pedro.linkedin.connections.repository.ProjectRepository;
import com.pedro.linkedin.connections.repository.SkillRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;

@Service
public class ConnectionQueryService {

    private static final List<String> STRATEGIC_POSITION_KEYWORDS = List.of(
            "recruiter",
            "talent acquisition",
            "human resources",
            "people partner",
            "hr",
            "ceo",
            "chief executive officer",
            "founder",
            "co-founder",
            "cofounder",
            "owner",
            "dono",
            "proprietario",
            "proprietário"
    );

    private final ConnectionRepository connectionRepository;
    private final ProjectRepository projectRepository;
    private final SkillRepository skillRepository;

    public ConnectionQueryService(
            ConnectionRepository connectionRepository,
            ProjectRepository projectRepository,
            SkillRepository skillRepository
    ) {
        this.connectionRepository = connectionRepository;
        this.projectRepository = projectRepository;
        this.skillRepository = skillRepository;
    }

    public List<ConnectionResponse> findAll() {
        return connectionRepository.findAll().stream()
                .map(ConnectionResponse::from)
                .toList();
    }

    public List<ConnectionResponse> findByFilters(String company, String position) {
        boolean hasCompany = hasText(company);
        boolean hasPosition = hasText(position);

        List<Connection> connections;
        if (hasCompany && hasPosition) {
            connections = connectionRepository.findByCompanyContainingIgnoreCaseAndPositionContainingIgnoreCase(
                    company.trim(),
                    position.trim()
            );
        } else if (hasCompany) {
            connections = connectionRepository.findByCompanyContainingIgnoreCase(company.trim());
        } else if (hasPosition) {
            connections = connectionRepository.findByPositionContainingIgnoreCase(position.trim());
        } else {
            connections = connectionRepository.findAll();
        }

        return connections.stream()
                .map(ConnectionResponse::from)
                .toList();
    }

    public ConnectionStatsResponse buildStats(int topLimit) {
        int limit = Math.max(1, topLimit);

        List<StatsCountItemResponse> companyCounts = toCountItems(connectionRepository.countByCompany());
        List<StatsCountItemResponse> positionCounts = toCountItems(connectionRepository.countByPosition());

        return new ConnectionStatsResponse(
                connectionRepository.count(),
                companyCounts.stream().limit(limit).toList(),
                positionCounts.stream().limit(limit).toList(),
                companyCounts
        );
    }

    public List<ConnectionResponse> findStrategicContacts(
            String company,
            String positionKeyword,
            boolean recruitersOnly,
            int limit
    ) {
        List<Connection> matches = connectionRepository.findStrategicContacts(
                normalizeOptional(company),
                normalizeOptional(positionKeyword),
                recruitersOnly
        );

        return matches.stream()
                .limit(Math.max(1, limit))
                .map(ConnectionResponse::from)
                .toList();
    }

    public LinkedinProfileAnalysisResponse buildProfileAnalysis() {
        long totalConnections = connectionRepository.count();
        long strategicConnections = connectionRepository.findAll().stream()
                .filter(this::isStrategicConnection)
                .count();
        double strategicConnectionsRatio = totalConnections == 0
                ? 0.0
                : ((double) strategicConnections / (double) totalConnections) * 100.0;
        long totalProjects = projectRepository.count();
        long totalSkills = skillRepository.count();

        List<LinkedinProfileFlagResponse> flags = List.of(
                new LinkedinProfileFlagResponse(
                        "connections-minimum",
                        connectionSeverity(totalConnections),
                        totalConnections < 500,
                        "Base de conexoes",
                        connectionMessage(totalConnections)
                ),
                new LinkedinProfileFlagResponse(
                        "strategic-network",
                        strategicSeverity(strategicConnectionsRatio),
                        strategicConnectionsRatio < 15.0,
                        "Perfis estrategicos",
                        strategicMessage(strategicConnectionsRatio)
                ),
                new LinkedinProfileFlagResponse(
                        "projects-minimum",
                        totalProjects < 3 ? "warning" : "ok",
                        totalProjects < 3,
                        "Projetos publicados",
                        totalProjects < 3
                                ? "Seu perfil ainda tem menos de 3 projetos. Isso reduz a prova pratica do seu trabalho."
                                : "A quantidade de projetos ja ajuda a sustentar seu perfil com evidencias praticas."
                )
        );

        long activeFlags = flags.stream().filter(LinkedinProfileFlagResponse::active).count();
        String summary = activeFlags == 0
                ? "Seu perfil passa bem pelos principais sinais desta analise."
                : "A analise encontrou " + activeFlags + " ponto(s) de atencao para melhorar no LinkedIn.";

        return new LinkedinProfileAnalysisResponse(
                totalConnections,
                strategicConnections,
                strategicConnectionsRatio,
                totalProjects,
                totalSkills,
                flags,
                summary
        );
    }

    private List<StatsCountItemResponse> toCountItems(List<ConnectionCountProjection> projections) {
        return projections.stream()
                .map(item -> new StatsCountItemResponse(item.getValue(), item.getTotal()))
                .toList();
    }

    private boolean isStrategicConnection(Connection connection) {
        if (connection.getPosition() == null || connection.getPosition().isBlank()) {
            return false;
        }
        String normalizedPosition = connection.getPosition().toLowerCase(Locale.ROOT);
        return STRATEGIC_POSITION_KEYWORDS.stream().anyMatch(normalizedPosition::contains);
    }

    private String connectionSeverity(long totalConnections) {
        if (totalConnections < 100) {
            return "danger";
        }
        if (totalConnections < 500) {
            return "warning";
        }
        return "success";
    }

    private String connectionMessage(long totalConnections) {
        if (totalConnections < 100) {
            return "Seu numero de conexoes ainda esta abaixo de 100. Esse ponto precisa de mais atencao no LinkedIn.";
        }
        if (totalConnections < 500) {
            return "Voce ja passou de 100 conexoes, mas o ideal e caminhar para 500 ou mais.";
        }
        return "Seu numero de conexoes ja esta em um nivel forte para o perfil.";
    }

    private String strategicSeverity(double strategicConnectionsRatio) {
        if (strategicConnectionsRatio < 10.0) {
            return "danger";
        }
        if (strategicConnectionsRatio < 15.0) {
            return "warning";
        }
        return "success";
    }

    private String strategicMessage(double strategicConnectionsRatio) {
        if (strategicConnectionsRatio < 10.0) {
            return "Sua quantidade de conexoes estrategicas ainda esta baixa. O ideal e chegar a pelo menos 15%.";
        }
        if (strategicConnectionsRatio < 15.0) {
            return "Tem uma boa quantidade de conexoes estrategicas, mas o ideal e 15%.";
        }
        return "Sua proporcao de conexoes estrategicas esta em um nivel saudavel.";
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private String normalizeOptional(String value) {
        return hasText(value) ? value.trim() : null;
    }
}
