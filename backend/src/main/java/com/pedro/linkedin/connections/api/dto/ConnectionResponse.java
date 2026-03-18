package com.pedro.linkedin.connections.api.dto;

import com.pedro.linkedin.connections.domain.Connection;

import java.time.LocalDate;

public record ConnectionResponse(
        Long id,
        String firstName,
        String lastName,
        String emailAddress,
        String company,
        String position,
        LocalDate connectedOn
) {
    public static ConnectionResponse from(Connection connection) {
        return new ConnectionResponse(
                connection.getId(),
                connection.getFirstName(),
                connection.getLastName(),
                connection.getEmailAddress(),
                connection.getCompany(),
                connection.getPosition(),
                connection.getConnectedOn()
        );
    }
}
