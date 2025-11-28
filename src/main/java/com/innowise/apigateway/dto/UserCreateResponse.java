package com.innowise.apigateway.dto;

import java.time.LocalDate;
import java.util.UUID;

public record UserCreateResponse(
        UUID id,
        String name,
        String surname,
        LocalDate birthDate,
        String email
) {
}
