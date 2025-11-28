package com.innowise.apigateway.mapper;

import com.innowise.apigateway.dto.*;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

import java.util.UUID;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface RequestMapper {
    AuthSignupRequest toAuthSignupRequest(UUID id, SignupRequest request);
    UserCreateRequest toUserCreateRequest(UUID id, SignupRequest request);
    @Mapping(target = "id", source = "auth.id")
    @Mapping(target = "email", source = "auth.email")
    SignupResponse toSignupResponse(AuthSignupResponse auth, UserCreateResponse user);
}
