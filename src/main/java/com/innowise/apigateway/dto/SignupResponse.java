package com.innowise.apigateway.dto;

import java.time.Instant;
import java.time.LocalDate;

public record SignupResponse(
        String userId,
        String email,
        String name,
        String surname,
        LocalDate birthDate,
        String role,
        Instant createdAt,
        Instant updatedAt
) {
}
