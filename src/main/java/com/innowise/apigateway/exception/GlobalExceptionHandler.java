package com.innowise.apigateway.exception;

import com.innowise.apigateway.dto.ApiResponse;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.codec.DecodingException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.reactive.resource.NoResourceFoundException;
import org.springframework.web.server.ServerWebInputException;

import java.util.List;


@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(WebExchangeBindException.class)
    public ResponseEntity<ApiResponse<List<String>>> handleValidationException(WebExchangeBindException e) {
        log.debug("Validation exception occurred: {}", e.getMessage());
        List<String> errors = e.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(err -> err.getField() + ": " + err.getDefaultMessage())
                .toList();
        return ResponseEntity
                .badRequest()
                .body(ApiResponse.error("Validation failed", errors));
    }

    @ExceptionHandler(StatusRuntimeException.class)
    public ResponseEntity<ApiResponse<Void>> handleGrpcException(StatusRuntimeException e) {
        log.error("gRPC exception occurred. Code={}, Description={}, Cause={}",
                e.getStatus().getCode(),
                e.getStatus().getDescription(),
                e.getCause() != null ? e.getCause().getMessage() : "null");
        return ResponseEntity
                .status(mapGrpcToHttp(e.getStatus().getCode()))
                .body(ApiResponse.error(getGrpcErrorMessage(e.getStatus().getCode(), e.getStatus().getDescription())));
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNoResourceFoundException(NoResourceFoundException e) {
        log.debug("No resource found. {}", e.getMessage());
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error("Resource not found"));
    }

    @ExceptionHandler(ServerWebInputException.class)
    public ResponseEntity<ApiResponse<Void>> handleServerWebInputException(ServerWebInputException e) {
        String message = "Invalid request value";

        if (e.getCause() instanceof DecodingException) {
            log.debug("JSON decoding error: {}", e.getMessage());
            message = "Invalid JSON format or structure";
        }

        return ResponseEntity
                .badRequest()
                .body(ApiResponse.error(message));
    }

    private HttpStatus mapGrpcToHttp(Status.Code code) {
        return switch (code) {
            case NOT_FOUND -> HttpStatus.NOT_FOUND;
            case INVALID_ARGUMENT -> HttpStatus.BAD_REQUEST;
            case ALREADY_EXISTS -> HttpStatus.CONFLICT;
            case PERMISSION_DENIED -> HttpStatus.FORBIDDEN;
            case UNAUTHENTICATED -> HttpStatus.UNAUTHORIZED;
            case UNAVAILABLE -> HttpStatus.SERVICE_UNAVAILABLE;
            case DEADLINE_EXCEEDED -> HttpStatus.GATEWAY_TIMEOUT;
            default -> HttpStatus.INTERNAL_SERVER_ERROR;
        };
    }

    private String getGrpcErrorMessage(Status.Code code, String description) {
        return switch (code) {
            case NOT_FOUND -> getMessageOrDefault(description, "Not found");
            case INVALID_ARGUMENT -> getMessageOrDefault(description, "Invalid argument");
            case ALREADY_EXISTS -> getMessageOrDefault(description, "Already exists");
            case PERMISSION_DENIED -> "Permission denied";
            case UNAUTHENTICATED -> "Unauthenticated";
            case UNAVAILABLE -> "Service is currently unavailable";
            case DEADLINE_EXCEEDED -> "The request timed out";
            default -> "Internal server error";
        };
    }

    private String getMessageOrDefault(String message, String defaultMessage) {
        return message != null ? message : defaultMessage;
    }

    @ExceptionHandler(WebClientResponseException.class)
    public ResponseEntity<ApiResponse<Void>> handleWebClientException(WebClientResponseException e) {
        log.debug("WebClient exception occurred: {}", e.getMessage());
        var errorMessage = "Unknown error";
        try {
            var response = e.getResponseBodyAs(ApiResponse.class);
            if (response != null && response.getMessage() != null) {
                errorMessage = response.getMessage();
            }
        } catch (Exception ex) {
            log.debug("Error parsing response body: {}", ex.getMessage());
        }
        return ResponseEntity
                .status(e.getStatusCode())
                .body(ApiResponse.error(errorMessage));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGeneralException(Exception e) {
        log.error("Exception occurred: {}. Message: {}", e.getClass(), e.getMessage());
        log.debug("Exception:", e);
        return ResponseEntity
                .internalServerError()
                .body(ApiResponse.error("Internal server error"));
    }
}
