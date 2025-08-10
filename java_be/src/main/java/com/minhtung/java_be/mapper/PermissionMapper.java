package com.minhtung.java_be.mapper;

import com.minhtung.java_be.dto.request.PermissionRequest;
import com.minhtung.java_be.dto.response.PermissionResponse;
import com.minhtung.java_be.entity.Permission;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface PermissionMapper {
    Permission toPermission(PermissionRequest request);

    PermissionResponse toPermissionResponse(Permission permission);
}