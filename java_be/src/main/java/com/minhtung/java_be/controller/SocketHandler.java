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
        // Lấy token từ query
        String token = client.getHandshakeData().getSingleUrlParam("token");
        if (token == null || token.isBlank()) {
            log.error("Missing token, disconnect {}", client.getSessionId());
            client.disconnect();
            return;
        }

        // Khai báo trước để dùng sau try-catch
        IntrospectResponse introspectResponse;

        try {
            introspectResponse = authenticationService.introspect(
                    IntrospectRequest.builder()
                            .token(token)
                            .build()
            );
        } catch (ParseException e) {
            log.error("Token introspection failed, disconnect {}", client.getSessionId(), e);
            client.disconnect();
            return;
        }

        // If Token is invalid disconnect
        if (introspectResponse.isValid()) {
            log.info("Client connected: {}", client.getSessionId());
            // Persist webSocketSession
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
        log.info("Received call offer from client: {}", client.getSessionId());
        signalingService.handleCallOffer(callOffer);
    }

    @OnEvent("call-answer")
    public void onCallAnswer(SocketIOClient client, CallAnswer callAnswer) {
        log.info("Received call answer from client: {}", client.getSessionId());
        signalingService.handleCallAnswer(callAnswer);
    }

    @OnEvent("ice-candidate")
    public void onIceCandidate(SocketIOClient client, IceCandidate iceCandidate) {
        log.info("Received ICE candidate from client: {}", client.getSessionId());
        signalingService.handleIceCandidate(iceCandidate);
    }

    @OnEvent("call-event")
    public void onCallEvent(SocketIOClient client, CallEvent callEvent) {
        log.info("Received call event: {} from client: {}", callEvent.getEvent(), client.getSessionId());
        signalingService.handleCallEvent(callEvent);
    }

    @OnEvent("check-user-status")
    public void onCheckUserStatus(SocketIOClient client, String userId) {
        boolean isOnline = signalingService.isUserOnline(userId);
        client.sendEvent("user-status", isOnline ? "online" : "offline");
    }

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

