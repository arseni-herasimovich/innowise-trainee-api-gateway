package com.innowise.apigateway.dto;

import java.time.LocalDate;

public record UserCreateResponse(
        String userId,
        String name,
        String surname,
        LocalDate birthDate,
        String email
) {
}
