package com.innowise.apigateway.mapper;

import com.innowise.userservice.generated.User;
import com.innowise.apigateway.dto.UserCreateRequest;
import com.innowise.apigateway.dto.UserCreateResponse;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface UserGrpcMapper {
    default User.UserCreateRequest toGrpcRequest(UserCreateRequest request, String userId) {
        return User.UserCreateRequest.newBuilder()
                .setUserId(userId)
                .setName(request.name())
                .setSurname(request.surname())
                .setBirthDate(request.birthDate().toString())
                .setEmail(request.email())
                .build();
    }

    UserCreateResponse toResponse(User.UserResponse response);
}
