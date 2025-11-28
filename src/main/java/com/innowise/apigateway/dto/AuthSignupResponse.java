package com.innowise.apigateway.dto;

import java.time.Instant;
import java.util.UUID;

public record AuthSignupResponse(
        UUID id,
        String email,
        String role,
        Instant createdAt,
        Instant updatedAt
) {
}
