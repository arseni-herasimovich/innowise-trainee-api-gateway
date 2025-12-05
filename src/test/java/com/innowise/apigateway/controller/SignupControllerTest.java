package com.innowise.apigateway.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.common.ConsoleNotifier;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import com.innowise.apigateway.dto.ApiResponse;
import com.innowise.apigateway.dto.AuthSignupRequest;
import com.innowise.apigateway.dto.AuthSignupResponse;
import com.innowise.apigateway.dto.SignupRequest;
import com.innowise.authservice.generated.Auth;
import com.innowise.userservice.generated.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.wiremock.grpc.GrpcExtensionFactory;
import org.wiremock.grpc.dsl.WireMockGrpcService;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.wiremock.grpc.dsl.WireMockGrpc.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class SignupControllerTest {

    @Autowired
    private WebTestClient client;

    @Autowired
    private ObjectMapper objectMapper;

    @RegisterExtension
    static WireMockExtension wm = WireMockExtension.newInstance()
            .options(wireMockConfig().
                    dynamicPort()
                    .notifier(new ConsoleNotifier(true))
                    .withRootDirectory("src/test/resources/wiremock")
                    .extensions(new GrpcExtensionFactory())
            )
            .build();

    @DynamicPropertySource
    static void discoveryProps(DynamicPropertyRegistry registry) {
        registry.add("eureka.client.enabled", () -> false);

        registry.add("spring.cloud.discovery.client.simple.instances.auth-service[0].instanceId", () -> "auth1");
        registry.add("spring.cloud.discovery.client.simple.instances.auth-service[0].serviceId", () -> "auth-service");
        registry.add("spring.cloud.discovery.client.simple.instances.auth-service[0].host", () -> "localhost");
        registry.add("spring.cloud.discovery.client.simple.instances.auth-service[0].port", () -> wm.getPort());
        registry.add("spring.cloud.discovery.client.simple.instances.auth-service[0].secure", () -> false);

        registry.add("grpc.server.port", () -> 0);
        registry.add("grpc.client.user-service.address", () -> "static://localhost:" + wm.getPort());
        registry.add("grpc.client.user-service.negotiation-type", () -> "plaintext");
        registry.add("grpc.client.auth-service.address", () -> "static://localhost:" + wm.getPort());
        registry.add("grpc.client.auth-service.negotiation-type", () -> "plaintext");
    }

    WireMockGrpcService userService;
    WireMockGrpcService authService;

    @BeforeEach
    void initStubs() {
        userService = new WireMockGrpcService(
                new WireMock(wm.getPort()),
                "com.innowise.userservice.generated.UserService"
        );
        authService = new WireMockGrpcService(
                new WireMock(wm.getPort()),
                "com.innowise.authservice.generated.AuthService"
        );
    }

    @Test
    @DisplayName("Should sign up when all services available and request is valid")
    void givenValidRequest_whenSignup_thenReturnSignupResponse() throws JsonProcessingException {
        var userId = UUID.randomUUID().toString();

        var saveCredentialsRequest = getSaveCredentialsRequest();
        var saveCredentialsResponse = getSaveCredentialsResponse(userId);

        var saveUserRequest = getSaveUserRequestBuilder(userId);
        var saveUserResponse = getSaveUserResponseBuilder(userId);

        wm.stubFor(WireMock.post("/api/v1/auth/credentials")
                .withRequestBody(equalToJson(objectMapper.writeValueAsString(saveCredentialsRequest)))
                .willReturn(
                        aResponse()
                                .withStatus(200)
                                .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                                .withBody(objectMapper.writeValueAsString(saveCredentialsResponse))
                )
        );

        userService.stubFor(
                method("CreateUser")
                        .withRequestMessage(equalToMessage(saveUserRequest))
                        .willReturn(message(saveUserResponse))
        );

        var request = new SignupRequest(
                saveCredentialsRequest.email(),
                saveCredentialsRequest.password(),
                saveUserRequest.getName(),
                saveUserRequest.getSurname(),
                LocalDate.parse(saveUserRequest.getBirthDate())
        );

        client.post()
                .uri("/api/v1/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(objectMapper.writeValueAsString(request))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.success").isEqualTo(true)
                .jsonPath("$.data").isNotEmpty()
                .jsonPath("$.data.userId").isEqualTo(userId)
                .jsonPath("$.data.email").isEqualTo(request.email())
                .jsonPath("$.data.name").isEqualTo(request.name())
                .jsonPath("$.data.birthDate").isEqualTo(request.birthDate())
                .jsonPath("$.data.role").isEqualTo("ROLE_USER")
                .jsonPath("$.data.createdAt").isEqualTo(saveCredentialsResponse.getData().createdAt());

        wm.verify(1, postRequestedFor(urlEqualTo("/api/v1/auth/credentials")));
        userService.verify(1, "CreateUser");
        authService.verify(0, "DeleteUser");
    }

    @Test
    @DisplayName("Should rollback creating user when user service is not available")
    void givenUnavailableUserService_whenSignup_thenRollback() throws JsonProcessingException {
        var userId = UUID.randomUUID().toString();

        var saveCredentialsRequest = getSaveCredentialsRequest();
        var saveCredentialsResponse = getSaveCredentialsResponse(userId);

        var saveUserRequest = getSaveUserRequestBuilder(userId);

        wm.stubFor(WireMock.post("/api/v1/auth/credentials")
                .withRequestBody(equalToJson(objectMapper.writeValueAsString(saveCredentialsRequest)))
                .willReturn(
                        aResponse()
                                .withStatus(200)
                                .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                                .withBody(objectMapper.writeValueAsString(saveCredentialsResponse))
                )
        );

        userService.stubFor(
                method("CreateUser")
                        .withRequestMessage(equalToMessage(saveUserRequest))
                        .willReturn(Status.UNAVAILABLE, "user-service is not available")
        );

        authService.stubFor(
                method("DeleteUser")
                        .withRequestMessage(equalToMessage(getDeleteUserRequest(userId)))
                        .willReturn(message(getDeleteUserResponse(true)))
        );

        var request = new SignupRequest(
                saveCredentialsRequest.email(),
                saveCredentialsRequest.password(),
                saveUserRequest.getName(),
                saveUserRequest.getSurname(),
                LocalDate.parse(saveUserRequest.getBirthDate())
        );

        client.post()
                .uri("/api/v1/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(objectMapper.writeValueAsString(request))
                .exchange()
                .expectStatus().is5xxServerError()
                .expectBody()
                .jsonPath("$.success").isEqualTo(false)
                .jsonPath("$.data").doesNotExist();

        wm.verify(1, postRequestedFor(urlEqualTo("/api/v1/auth/credentials")));
        userService.verify(1, "CreateUser");
        authService.verify(1, "DeleteUser");
    }

    @Test
    @DisplayName("Should return conflict when register existing user")
    void givenConflictRequest_whenSignup_thenReturnConflictResponse() throws JsonProcessingException {
        var saveCredentialsRequest = getSaveCredentialsRequest();
        var saveCredentialsResponse = ApiResponse.error("User already exists");

        wm.stubFor(WireMock.post("/api/v1/auth/credentials")
                .withRequestBody(equalToJson(objectMapper.writeValueAsString(saveCredentialsRequest)))
                .willReturn(
                        aResponse()
                                .withStatus(409)
                                .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                                .withBody(objectMapper.writeValueAsString(saveCredentialsResponse))
                )
        );

        var request = new SignupRequest(
                saveCredentialsRequest.email(),
                saveCredentialsRequest.password(),
                "TEST_NAME",
                "TEST_SURNAME",
                LocalDate.now().minusDays(1)
        );

        client.post()
                .uri("/api/v1/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(objectMapper.writeValueAsString(request))
                .exchange()
                .expectStatus().is4xxClientError()
                .expectBody()
                .jsonPath("$.success").isEqualTo(false)
                .jsonPath("$.data").doesNotExist();

        wm.verify(1, postRequestedFor(urlEqualTo("/api/v1/auth/credentials")));
        userService.verify(0, "CreateUser");
        authService.verify(0, "DeleteUser");
    }

    @Test
    @DisplayName("Should return bad request when validation is failed")
    void givenInvalidRequest_whenSignup_thenReturnBadRequest() throws JsonProcessingException {
        var request = new SignupRequest(
                "TEST",
                "password",
                "TEST_NAME",
                "TEST_SURNAME",
                LocalDate.now().minusDays(1)
        );

        client.post()
                .uri("/api/v1/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(objectMapper.writeValueAsString(request))
                .exchange()
                .expectStatus().is4xxClientError()
                .expectBody()
                .jsonPath("$.success").isEqualTo(false)
                .jsonPath("$.data").exists();

        wm.verify(0, postRequestedFor(urlEqualTo("/api/v1/auth/credentials")));
        userService.verify(0, "CreateUser");
        authService.verify(0, "DeleteUser");
    }

    @Test
    @DisplayName("Should return bad request when birthdate is corrupted")
    void givenCorruptedBirthdate_whenSignup_thenReturnBadRequest() throws JsonProcessingException {
        var request = Map.of(
                "email", "TEST@EMAIL",
                "password", "Test_Password1",
                "name", "TEST_NAME",
                "surname", "TEST_SURNAME",
                "birthDate", "CORRUPTED_BIRTHDATE"
        );

        client.post()
                .uri("/api/v1/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(objectMapper.writeValueAsString(request))
                .exchange()
                .expectStatus().is4xxClientError()
                .expectBody()
                .jsonPath("$.success").isEqualTo(false)
                .jsonPath("$.data").doesNotExist();

        wm.verify(0, postRequestedFor(urlEqualTo("/api/v1/auth/credentials")));
        userService.verify(0, "CreateUser");
        authService.verify(0, "DeleteUser");
    }

    @Test
    @DisplayName("Should return bad request when request is corrupted")
    void givenCorruptedRequest_whenSignup_thenReturnBadRequest() throws JsonProcessingException {
        var request = """
                {
                /.das
                
                """;

        client.post()
                .uri("/api/v1/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(objectMapper.writeValueAsString(request))
                .exchange()
                .expectStatus().is4xxClientError()
                .expectBody()
                .jsonPath("$.success").isEqualTo(false)
                .jsonPath("$.data").doesNotExist();

        wm.verify(0, postRequestedFor(urlEqualTo("/api/v1/auth/credentials")));
        userService.verify(0, "CreateUser");
        authService.verify(0, "DeleteUser");
    }

    @Test
    @DisplayName("Should return notfound when accessing non-existent endpoint")
    void givenNonExistentEndpoint_whenRequest_thenNotFoundResponse() {
        client.get()
                .uri("/notexistingendpoint")
                .exchange()
                .expectStatus().isNotFound()
                .expectBody()
                .jsonPath("$.success").isEqualTo(false);
    }

   private AuthSignupRequest getSaveCredentialsRequest() {
        return new AuthSignupRequest("TEST@EMAIL", "Test_Password1");
    }

    private ApiResponse<AuthSignupResponse> getSaveCredentialsResponse(String userId) {
        var saveUserRequest = getSaveUserRequestBuilder(userId);
        return ApiResponse.success(
                "Credentials saved successfully",
                new AuthSignupResponse(
                        userId,
                        saveUserRequest.getEmail(),
                        "ROLE_USER",
                        Instant.now(), Instant.now()
                )
        );
    }

    private User.UserCreateRequest.Builder getSaveUserRequestBuilder(String userId) {
        return User.UserCreateRequest.newBuilder()
                .setEmail("TEST@EMAIL")
                .setName("TEST_NAME")
                .setSurname("TEST_SURNAME")
                .setBirthDate(LocalDate.now().minusDays(1).toString())
                .setUserId(userId);
    }

    private User.UserResponse.Builder getSaveUserResponseBuilder(String userId) {
        var saveUserRequest = getSaveUserRequestBuilder(userId);
        return User.UserResponse.newBuilder()
                .setEmail(saveUserRequest.getEmail())
                .setName(saveUserRequest.getName())
                .setSurname(saveUserRequest.getSurname())
                .setBirthDate(saveUserRequest.getBirthDate())
                .setUserId(saveUserRequest.getUserId());
    }

    private Auth.DeleteUserRequest getDeleteUserRequest(String userId) {
        return Auth.DeleteUserRequest.newBuilder()
                .setUserId(userId)
                .build();
    }

    private Auth.DeleteUserResponse getDeleteUserResponse(boolean success) {
        return Auth.DeleteUserResponse.newBuilder()
                .setSuccess(success)
                .build();
    }
}
