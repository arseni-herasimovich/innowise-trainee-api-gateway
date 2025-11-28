package com.innowise.apigateway.orchestrator;

import com.innowise.apigateway.client.AuthServiceClient;
import com.innowise.apigateway.client.AuthServiceGrpcClient;
import com.innowise.apigateway.client.UserServiceGrpcClient;
import com.innowise.apigateway.dto.SignupRequest;
import com.innowise.apigateway.dto.SignupResponse;
import com.innowise.apigateway.dto.UserCreateRequest;
import com.innowise.apigateway.mapper.RequestMapper;
import com.innowise.apigateway.mapper.UserGrpcMapper;
import com.innowise.apigateway.util.IdGenerator;
import com.innowise.authservice.generated.Auth;
import com.innowise.userservice.generated.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class SignupOrchestrator {
    private final IdGenerator idGenerator;
    private final RequestMapper requestMapper;
    private final AuthServiceGrpcClient authServiceGrpcClient;
    private final UserServiceGrpcClient userServiceGrpcClient;
    private final UserGrpcMapper userGrpcMapper;
    private final AuthServiceClient authServiceClient;

    public Mono<SignupResponse> orchestrateSignup(SignupRequest request) {
        var id = idGenerator.generate();
        var authSignupRequest = requestMapper.toAuthSignupRequest(id, request);
        var userCreateRequest = requestMapper.toUserCreateRequest(id, request);

        return authServiceClient.signup(authSignupRequest)
                .doOnNext(response -> log.info("Auth service response: {}", response))
                .flatMap(authSignupResponse -> createUser(userCreateRequest)
                        .doOnNext(response -> log.info("User service response: {}", response))
                        .map(userGrpcResponse ->
                                requestMapper.toSignupResponse(authSignupResponse.getData(),
                                        userGrpcMapper.toResponse(userGrpcResponse)))
                        .onErrorResume(e -> {
                            log.error("Error creating user, rolling back auth user with id: {}", id, e);
                            return rollback(id).then(Mono.error(e));
                        })
                )
                .doOnNext(response -> log.info("Signup orchestration completed successfully: {}", response));
    }

    private Mono<User.UserResponse> createUser(UserCreateRequest request) {
        return userServiceGrpcClient.create(userGrpcMapper.toGrpcRequest(request));
    }

    private Mono<Auth.DeleteUserResponse> rollback(UUID id) {
        return authServiceGrpcClient.delete(id)
                .doOnError(e -> log.error("Error rolling back signing up user with id: {}", id, e))
                .onErrorResume(e -> Mono.empty());
    }
}
