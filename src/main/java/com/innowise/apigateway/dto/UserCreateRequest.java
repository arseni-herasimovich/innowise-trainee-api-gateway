package com.innowise.apigateway.dto;

import java.time.LocalDate;

public record UserCreateRequest(
        String name,
        String surname,
        LocalDate birthDate,
        String email
) {
}
