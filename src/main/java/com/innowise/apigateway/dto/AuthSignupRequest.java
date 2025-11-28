package com.innowise.apigateway.dto;

import java.util.UUID;

public record AuthSignupRequest(
        UUID id,
        String email,
        String password
) {
}
