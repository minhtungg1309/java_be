package com.minhtung.java_be.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.Set;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Entity
@Table(name = "role")
public class Role {
    @Id
    String name;

    String description;

    @ManyToMany
    @JoinTable(
        name = "role_permissions",
        joinColumns = @JoinColumn(name = "role_name"),
        inverseJoinColumns = @JoinColumn(name = "permission_name")
    )
    Set<Permission> permissions;
}