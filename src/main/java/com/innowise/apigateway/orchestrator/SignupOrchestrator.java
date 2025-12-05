package com.innowise.apigateway.orchestrator;

import com.innowise.apigateway.client.AuthServiceClient;
import com.innowise.apigateway.client.AuthServiceGrpcClient;
import com.innowise.apigateway.client.UserServiceGrpcClient;
import com.innowise.apigateway.dto.SignupRequest;
import com.innowise.apigateway.dto.SignupResponse;
import com.innowise.apigateway.dto.UserCreateRequest;
import com.innowise.apigateway.mapper.RequestMapper;
import com.innowise.apigateway.mapper.UserGrpcMapper;
import com.innowise.authservice.generated.Auth;
import com.innowise.userservice.generated.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@Slf4j
@RequiredArgsConstructor
public class SignupOrchestrator {
    private final RequestMapper requestMapper;
    private final AuthServiceGrpcClient authServiceGrpcClient;
    private final UserServiceGrpcClient userServiceGrpcClient;
    private final UserGrpcMapper userGrpcMapper;
    private final AuthServiceClient authServiceClient;

    public Mono<SignupResponse> orchestrateSignup(SignupRequest request) {
        var authSignupRequest = requestMapper.toAuthSignupRequest(request);
        var userCreateRequest = requestMapper.toUserCreateRequest(request);

        return authServiceClient.signup(authSignupRequest)
                .doOnNext(response -> log.debug("Auth service response: {}", response))
                .flatMap(authSignupResponse ->
                        createUser(userCreateRequest, authSignupResponse.getData().userId())
                        .doOnNext(response -> log.debug("User service response: {}", response))
                        .map(userGrpcResponse ->
                                requestMapper.toSignupResponse(authSignupResponse.getData(),
                                        userGrpcMapper.toResponse(userGrpcResponse)))
                        .onErrorResume(e -> {
                            var id = authSignupResponse.getData().userId();
                            log.error("Error creating user, rolling back auth user with id: {}", id);
                            return rollback(id).then(Mono.error(e));
                        })
                )
                .doOnNext(response -> log.info("Signup orchestration completed successfully: {}", response));
    }

    private Mono<User.UserResponse> createUser(UserCreateRequest request, String userId) {
        return userServiceGrpcClient.create(userGrpcMapper.toGrpcRequest(request, userId));
    }

    private Mono<Auth.DeleteUserResponse> rollback(String userId) {
        return authServiceGrpcClient.delete(userId)
                .doOnError(e -> log.error("Error rolling back signing up user with id: {}", userId))
                .onErrorResume(e -> Mono.empty());
    }
}
