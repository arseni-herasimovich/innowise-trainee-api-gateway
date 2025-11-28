package com.innowise.apigateway.client;

import com.innowise.authservice.generated.Auth;
import com.innowise.authservice.generated.AuthServiceGrpc;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.util.UUID;

@Service
public class AuthServiceGrpcClient {

    @GrpcClient("auth-service")
    private AuthServiceGrpc.AuthServiceBlockingStub authServiceBlockingStub;

    @Value("${client.default-timeout:10s}")
    private Duration duration;

    public Mono<Auth.DeleteUserResponse> delete(UUID userId) {
        return Mono.fromCallable(() -> authServiceBlockingStub.deleteUser(
                Auth.DeleteUserRequest.newBuilder().setId(userId.toString()).build()))
                .subscribeOn(Schedulers.boundedElastic())
                .timeout(duration);
    }
}
