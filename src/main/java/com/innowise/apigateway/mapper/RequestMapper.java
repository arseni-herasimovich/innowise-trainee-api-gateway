package com.innowise.apigateway.mapper;

import com.innowise.apigateway.dto.*;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface RequestMapper {
    AuthSignupRequest toAuthSignupRequest(SignupRequest request);

    UserCreateRequest toUserCreateRequest(SignupRequest request);

    @Mapping(target = "userId", source = "auth.userId")
    @Mapping(target = "email", source = "auth.email")
    SignupResponse toSignupResponse(AuthSignupResponse auth, UserCreateResponse user);
}
