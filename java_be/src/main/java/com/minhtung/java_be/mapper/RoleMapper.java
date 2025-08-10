package com.minhtung.java_be.mapper;

import com.minhtung.java_be.dto.request.RoleRequest;
import com.minhtung.java_be.dto.response.RoleResponse;
import com.minhtung.java_be.entity.Role;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface RoleMapper {
    @Mapping(target = "permissions", ignore = true)
    Role toRole(RoleRequest request);

    RoleResponse toRoleResponse(Role role);
}
