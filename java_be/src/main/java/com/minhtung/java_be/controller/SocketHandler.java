package com.minhtung.java_be.controller;

import com.corundumstudio.socketio.SocketIOClient;
import com.corundumstudio.socketio.SocketIOServer;
import com.corundumstudio.socketio.annotation.OnConnect;
import com.corundumstudio.socketio.annotation.OnDisconnect;
import com.corundumstudio.socketio.annotation.OnEvent;
import com.minhtung.java_be.dto.request.IntrospectRequest;
import com.minhtung.java_be.dto.response.IntrospectResponse;
import com.minhtung.java_be.dto.signaling.*;
import com.minhtung.java_be.entity.WebSocketSession;
import com.minhtung.java_be.service.AuthenticationService;
import com.minhtung.java_be.service.SignalingService;
import com.minhtung.java_be.service.WebSocketSessionService;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.text.ParseException;
import java.time.Instant;

@Slf4j
@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class SocketHandler {
    SocketIOServer server;
    WebSocketSessionService webSocketSessionService;
    AuthenticationService authenticationService;
    SignalingService signalingService;

    @OnConnect
    public void clientConnected(SocketIOClient client) {
        String token = client.getHandshakeData().getSingleUrlParam("token");
        if (token == null || token.isBlank()) {
            log.error("Missing token, disconnect {}", client.getSessionId());
            client.disconnect();
            return;
        }

        IntrospectResponse introspectResponse;
        try {
            introspectResponse = authenticationService.introspect(
                    IntrospectRequest.builder().token(token).build()
            );
        } catch (ParseException e) {
            log.error("Token introspection failed, disconnect {}", client.getSessionId(), e);
            client.disconnect();
            return;
        }

        if (introspectResponse.isValid()) {
            log.info("Client connected: {}", client.getSessionId());

            // **LƯU userId vào client để dùng sau**
            client.set("userId", introspectResponse.getUserId());

            WebSocketSession webSocketSession = WebSocketSession.builder()
                    .socketSessionId(client.getSessionId().toString())
                    .userId(introspectResponse.getUserId())
                    .createdAt(Instant.now())
                    .build();
            webSocketSession = webSocketSessionService.create(webSocketSession);

            log.info("WebSocketSession created with id: {}", webSocketSession.getId());
        } else {
            log.error("Authentication fail: {}", client.getSessionId());
            client.disconnect();
        }
    }

    @OnDisconnect
    public void clientDisconnected(SocketIOClient client) {
        log.info("Client disConnected: {}", client.getSessionId());
        webSocketSessionService.deleteSession(client.getSessionId().toString());
    }

    // Signaling Events
    @OnEvent("call-offer")
    public void onCallOffer(SocketIOClient client, CallOffer callOffer) {
        try {
            // **LẤY userId từ client attributes (đã verify từ token)**
            String callerId = client.get("userId");
            if (callerId == null) {
                log.error("❌ No userId found in client session: {}", client.getSessionId());
                return;
            }

            callOffer.setCallerId(callerId);
            log.info("📞 Call offer from {} to conversation {}", callerId, callOffer.getConversationId());

            signalingService.handleCallOffer(callOffer);
        } catch (Exception e) {
            log.error("❌ Error handling call offer", e);
        }
    }

    @OnEvent("call-answer")
    public void onCallAnswer(SocketIOClient client, CallAnswer callAnswer) {
        String userId = client.get("userId");
        log.info("Received call answer from user: {} (session: {})", userId, client.getSessionId());
        signalingService.handleCallAnswer(callAnswer);
    }

    @OnEvent("ice-candidate")
    public void onIceCandidate(SocketIOClient client, IceCandidate iceCandidate) {
        String userId = client.get("userId");
        log.info("Received ICE candidate from user: {} (session: {})", userId, client.getSessionId());

        // **SET userId nếu cần**
        if (iceCandidate.getFromUserId() == null) {
            iceCandidate.setFromUserId(userId);
        }

        signalingService.handleIceCandidate(iceCandidate);
    }

    @OnEvent("call-event")
    public void onCallEvent(SocketIOClient client, CallEvent callEvent) {
        String userId = client.get("userId");
        log.info("Received call event: {} from user: {} (session: {})",
                callEvent.getEvent(), userId, client.getSessionId());

        // **SET userId nếu cần**
        if (callEvent.getFromUserId() == null) {
            callEvent.setFromUserId(userId);
        }

        signalingService.handleCallEvent(callEvent);
    }

//    @OnEvent("check-user-status")
//    public void onCheckUserStatus(SocketIOClient client, String userId) {
//        boolean isOnline = signalingService.isUserOnline(userId);
//        client.sendEvent("user-status", isOnline ? "online" : "offline");
//    }

    @PostConstruct
    public void startServer() {
        server.start();
        server.addListeners(this);
        log.info("Socket server started");
    }

    @PreDestroy
    public void stopServer() {
        server.stop();
        log.info("Socket server stopped");
    }
}

