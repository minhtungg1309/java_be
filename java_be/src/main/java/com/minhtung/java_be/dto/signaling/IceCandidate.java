package com.minhtung.java_be.dto.signaling;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class IceCandidate {
    String candidate;
    String sdpMid;
    Integer sdpMLineIndex;
    String callId;
    String fromUserId;
    String toUserId;
}