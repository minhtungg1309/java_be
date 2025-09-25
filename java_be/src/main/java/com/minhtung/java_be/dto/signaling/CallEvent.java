package com.minhtung.java_be.dto.signaling;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CallEvent {
    String conversationId;
    String callId;
    String event;   // "reject", "accept", "end", "busy", "offline", "error"
    String reason;
    String fromUserId;
}