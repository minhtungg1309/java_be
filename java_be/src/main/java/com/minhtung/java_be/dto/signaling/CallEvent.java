package com.minhtung.java_be.dto.signaling;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CallEvent {
    String callId;
    String callerId;
    String calleeId;
    String event; // "reject", "accept", "end", "busy"
    String reason;
}