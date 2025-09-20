package com.minhtung.java_be.service;

import com.corundumstudio.socketio.SocketIOClient;
import com.corundumstudio.socketio.SocketIOServer;
import com.minhtung.java_be.dto.signaling.*;
import com.minhtung.java_be.entity.WebSocketSession;
import com.minhtung.java_be.entity.User;
import com.minhtung.java_be.repository.UserRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class SignalingService {
    SocketIOServer server;
    WebSocketSessionService webSocketSessionService;
    UserService userService; // Add this dependency
    UserRepository userRepository;

    public void handleCallOffer(CallOffer callOffer) {
        log.info("Handling call offer from {} to {}", callOffer.getCallerId(), callOffer.getCalleeId());
        
        // Validate call offer
        if (callOffer.getCallerId() == null || callOffer.getCalleeId() == null) {
            log.error("Invalid call offer: missing caller or callee ID");
            return;
        }

        try {
            // Enrich caller information
            Optional<User> callerUser = userRepository.findById(callOffer.getCallerId());
            if (callerUser.isPresent()) {
                User caller = callerUser.get();
                CallerInfo callerInfo = CallerInfo.builder()
                        .username(caller.getUsername())
                        .firstName(caller.getFirstName())
                        .lastName(caller.getLastName())
                        .avatar(caller.getAvatar())
                        .displayName(getDisplayName(caller))
                        .build();
                
                callOffer.setCallerInfo(callerInfo);
                log.info("Enriched caller info: {}", callerInfo.getDisplayName());
            } else {
                log.warn("Caller user not found: {}", callOffer.getCallerId());
                // Set fallback caller info
                CallerInfo fallbackInfo = CallerInfo.builder()
                        .username("Unknown User")
                        .displayName("Unknown User")
                        .build();
                callOffer.setCallerInfo(fallbackInfo);
            }

            // Check if callee is online
            List<WebSocketSession> calleeSession = webSocketSessionService.findByUserId(callOffer.getCalleeId());
            if (calleeSession.isEmpty()) {
                log.warn("Callee {} is not online", callOffer.getCalleeId());
                // Send caller that callee is offline
                sendToUser(callOffer.getCallerId(), "call-status", 
                    CallEvent.builder()
                        .callId(callOffer.getCallId())
                        .callerId(callOffer.getCallerId())
                        .calleeId(callOffer.getCalleeId())
                        .event("offline")
                        .reason("User is not online")
                        .build());
                return;
            }

            // Generate call ID if not provided
            if (callOffer.getCallId() == null || callOffer.getCallId().isEmpty()) {
                callOffer.setCallId(UUID.randomUUID().toString());
            }

            // Forward offer to callee
            sendToUser(callOffer.getCalleeId(), "incoming-call", callOffer);
            
            log.info("Call offer forwarded to {} with caller info: {}", 
                    callOffer.getCalleeId(), 
                    callOffer.getCallerInfo().getDisplayName());

        } catch (Exception e) {
            log.error("Error handling call offer: {}", e.getMessage(), e);
            
            // Send error to caller
            sendToUser(callOffer.getCallerId(), "call-status", 
                CallEvent.builder()
                    .callId(callOffer.getCallId())
                    .callerId(callOffer.getCallerId())
                    .calleeId(callOffer.getCalleeId())
                    .event("error")
                    .reason("Internal server error")
                    .build());
        }
    }

    private String getDisplayName(User user) {
        if (user.getFirstName() != null && user.getLastName() != null) {
            return user.getFirstName() + " " + user.getLastName();
        } else if (user.getFirstName() != null) {
            return user.getFirstName();
        } else if (user.getLastName() != null) {
            return user.getLastName();
        } else {
            return user.getUsername();
        }
    }

    public void handleCallAnswer(CallAnswer callAnswer) {
        log.info("Handling call answer from {} to {}", callAnswer.getCalleeId(), callAnswer.getCallerId());
        sendToUser(callAnswer.getCallerId(), "call-answered", callAnswer);
    }

    public void handleIceCandidate(IceCandidate iceCandidate) {
        log.info("Handling ICE candidate from {} to {}", iceCandidate.getFromUserId(), iceCandidate.getToUserId());
        sendToUser(iceCandidate.getToUserId(), "ice-candidate", iceCandidate);
    }

    public void handleCallEvent(CallEvent callEvent) {
        log.info("Handling call event: {} for call {}", callEvent.getEvent(), callEvent.getCallId());

        switch (callEvent.getEvent()) {
            case "reject":
            case "busy":
            case "accept":
                sendToUser(callEvent.getCallerId(), "call-status", callEvent);
                break;
            case "end":
                sendToUser(callEvent.getCallerId(), "call-ended", callEvent);
                sendToUser(callEvent.getCalleeId(), "call-ended", callEvent);
                break;
        }
    }

    private void sendToUser(String userId, String event, Object data) {
        List<WebSocketSession> sessions = webSocketSessionService.findByUserId(userId);
        if (sessions.isEmpty()) {
            log.warn("User {} is not online", userId);
            return;
        }

        for (WebSocketSession session : sessions) {
            try {
                UUID sessionUUID = UUID.fromString(session.getSocketSessionId());
                SocketIOClient client = server.getClient(sessionUUID);

                if (client != null && client.isChannelOpen()) {
                    client.sendEvent(event, data);
                    log.debug("Sent {} event to user {} (session {})", event, userId, sessionUUID);
                } else {
                    log.warn("Dead session {} for user {}", sessionUUID, userId);
                    webSocketSessionService.deleteSession(session.getSocketSessionId());
                }
            } catch (IllegalArgumentException e) {
                log.error("Invalid UUID format for session: {}", session.getSocketSessionId());
            }
        }
    }

    public boolean isUserOnline(String userId) {
        return webSocketSessionService.isUserOnline(userId);
    }
}
