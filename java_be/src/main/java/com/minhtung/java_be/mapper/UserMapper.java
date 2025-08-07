package com.minhtung.java_be.mapper;

import com.minhtung.java_be.dto.request.UserCreationRequest;
import com.minhtung.java_be.dto.request.UserUpdateRequest;
import com.minhtung.java_be.dto.response.UserResponse;
import com.minhtung.java_be.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface UserMapper {
    User toUser(UserCreationRequest request);

    UserResponse toUserResponse(User user);

   @Mapping(target = "roles", ignore = true)
    void updateUser(@MappingTarget User user, UserUpdateRequest request);
}