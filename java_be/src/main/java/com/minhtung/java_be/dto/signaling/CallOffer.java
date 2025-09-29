package com.minhtung.java_be.dto.signaling;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CallOffer {
    String conversationId;       // Bỏ callerId / calleeId → thay bằng conversationId
    String sdp;
    String type;                 // "offer"
    String callType;             // "audio" | "video"
    String callId;
    String callerId;
    CallerInfo callerInfo;
}