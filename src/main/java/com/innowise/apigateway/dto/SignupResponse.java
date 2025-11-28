package com.innowise.apigateway.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record SignupResponse(
        UUID id,
        String email,
        String name,
        String surname,
        LocalDate birthDate,
        String role,
        Instant createdAt,
        Instant updatedAt
) {
}
