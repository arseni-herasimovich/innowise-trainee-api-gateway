package com.innowise.apigateway.client;

import com.innowise.apigateway.dto.ApiResponse;
import com.innowise.apigateway.dto.AuthSignupRequest;
import com.innowise.apigateway.dto.AuthSignupResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Service
public class AuthServiceClient {
    private final WebClient webClient;

    @Value("${client.default-timeout:10s}")
    private Duration duration;

    @Value("${services.auth-service-url}")
    private String authServiceUrl;

    public AuthServiceClient() {
        this.webClient = WebClient.builder().build();
    }

    public Mono<ApiResponse<AuthSignupResponse>> signup(AuthSignupRequest request) {
        return webClient.post()
                .uri(authServiceUrl + "/api/v1/auth/credentials")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<ApiResponse<AuthSignupResponse>>() {})
                .timeout(duration);
    }
}
