package com.innowise.apigateway.controller;

import com.innowise.apigateway.dto.ApiResponse;
import com.innowise.apigateway.dto.SignupRequest;
import com.innowise.apigateway.dto.SignupResponse;
import com.innowise.apigateway.orchestrator.SignupOrchestrator;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class SignupController {

    private final SignupOrchestrator signupOrchestrator;

    @PostMapping("/signup")
    public Mono<ApiResponse<SignupResponse>> signup(@RequestBody @Valid SignupRequest request) {
        return signupOrchestrator.orchestrateSignup(request)
                .map(response -> ApiResponse.success("Signup successful", response));
    }
}
