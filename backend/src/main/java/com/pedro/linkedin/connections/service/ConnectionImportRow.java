package com.pedro.linkedin.connections.service;

import java.time.LocalDate;

record ConnectionImportRow(
        String firstName,
        String lastName,
        String emailAddress,
        String company,
        String position,
        LocalDate connectedOn
) {
}
