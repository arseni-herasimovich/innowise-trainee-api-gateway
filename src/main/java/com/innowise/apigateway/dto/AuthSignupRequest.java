package com.innowise.apigateway.dto;

public record AuthSignupRequest(
        String email,
        String password
) {
}
