package com.minhtung.java_be.dto.signaling;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CallAnswer {
     String callerId;
     String calleeId;
     String sdp;
     String type; // "answer"
     String callId;
}