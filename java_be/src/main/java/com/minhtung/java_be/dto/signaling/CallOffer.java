package com.minhtung.java_be.dto.signaling;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CallOffer {
    String callerId;
    String calleeId;
    String sdp;
    String type;
    String callType; // "audio" or "video"
    String callId;
    
    // Add caller information
    CallerInfo callerInfo;
    

}