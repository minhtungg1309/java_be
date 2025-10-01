package com.minhtung.java_be.dto.signaling;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CallerInfo {
    String username;
    String firstName;
    String lastName;
    String avatar;
    String displayName;

}