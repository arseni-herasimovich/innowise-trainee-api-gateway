package com.innowise.apigateway.dto;

import jakarta.validation.constraints.*;

import java.time.LocalDate;

public record SignupRequest(
        @NotBlank(message = "Email is required")
        @Email(message = "Email should be valid")
        @Size(max = 255)
        String email,
        @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).{8,255}$",
                message = "Password must contain at least one lowercase letter, one uppercase letter, and one digit, and be between 8 and 255 characters long")
        @NotBlank(message = "Password is required")
        String password,
        @NotBlank(message = "Name is required")
        String name,
        @NotBlank(message = "Surname is required")
        String surname,
        @Past(message = "Birth date must be in the past")
        @NotNull(message = "Birth date is required")
        LocalDate birthDate
) {
}
