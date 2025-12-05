package com.innowise.apigateway.dto;

import java.time.Instant;

public record AuthSignupResponse(
        String userId,
        String email,
        String role,
        Instant createdAt,
        Instant updatedAt
) {
}
