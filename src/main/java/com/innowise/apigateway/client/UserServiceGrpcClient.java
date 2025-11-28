package com.innowise.apigateway.client;

import com.innowise.userservice.generated.User;
import com.innowise.userservice.generated.UserServiceGrpc;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;

@Service
public class UserServiceGrpcClient {

    @GrpcClient("user-service")
    private UserServiceGrpc.UserServiceBlockingStub userServiceBlockingStub;

    @Value("${client.default-timeout:10s}")
    private Duration duration;

    public Mono<User.UserResponse> create(User.UserCreateRequest request) {
        return Mono.fromCallable(() -> userServiceBlockingStub.createUser(request))
                .subscribeOn(Schedulers.boundedElastic())
                .timeout(duration);
    }
}
