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
            log.error("❌ Thiếu token, ngắt kết nối {}", client.getSessionId());
            client.disconnect();
            return;
        }

        IntrospectResponse introspectResponse;
        try {
            introspectResponse = authenticationService.introspect(
                    IntrospectRequest.builder().token(token).build()
            );
        } catch (ParseException e) {
            log.error("❌ Xác thực token thất bại, ngắt kết nối {}", client.getSessionId(), e);
            client.disconnect();
            return;
        }

        if (introspectResponse.isValid()) {
            String userId = introspectResponse.getUserId();
            log.info("✅ Client {} kết nối thành công với userId={}", client.getSessionId(), userId);

            client.set("userId", userId);

            WebSocketSession webSocketSession = WebSocketSession.builder()
                    .socketSessionId(client.getSessionId().toString())
                    .userId(userId)
                    .createdAt(Instant.now())
                    .build();
            webSocketSessionService.create(webSocketSession);

            log.info("💾 WebSocketSession được lưu cho userId={} (session={})", userId, client.getSessionId());
        } else {
            log.error("❌ Token không hợp lệ, ngắt kết nối {}", client.getSessionId());
            client.disconnect();
        }
    }

    @OnDisconnect
    public void clientDisconnected(SocketIOClient client) {
        log.info("❌ Client ngắt kết nối: {}", client.getSessionId());
        webSocketSessionService.deleteSession(client.getSessionId().toString());
    }

    // === Các event từ FE gửi lên ===
    @OnEvent("call-offer")
    public void onCallOffer(SocketIOClient client, CallOffer callOffer) {
        String callerId = client.get("userId");
        log.info("⬆️ [FE->BE] call-offer từ userId={} session={}, data={}",
                callerId, client.getSessionId(), callOffer);

        callOffer.setCallerId(callerId);
        signalingService.handleCallOffer(callOffer);
    }

    @OnEvent("call-answer")
    public void onCallAnswer(SocketIOClient client, CallAnswer callAnswer) {
        String userId = client.get("userId");
        log.info("⬆️ [FE->BE] call-answer từ userId={} session={}, data={}",
                userId, client.getSessionId(), callAnswer);

        signalingService.handleCallAnswer(callAnswer);
    }

    @OnEvent("ice-candidate")
    public void onIceCandidate(SocketIOClient client, IceCandidate iceCandidate) {
        String userId = client.get("userId");
        log.info("⬆️ [FE->BE] ice-candidate từ userId={} session={}, data={}",
                userId, client.getSessionId(), iceCandidate);

        if (iceCandidate.getFromUserId() == null) {
            iceCandidate.setFromUserId(userId);
        }

        signalingService.handleIceCandidate(iceCandidate);
    }

    @OnEvent("call-event")
    public void onCallEvent(SocketIOClient client, CallEvent callEvent) {
        String userId = client.get("userId");
        log.info("⬆️ [FE->BE] call-event={} từ userId={} session={}, data={}",
                callEvent.getEvent(), userId, client.getSessionId(), callEvent);

        if (callEvent.getFromUserId() == null) {
            callEvent.setFromUserId(userId);
        }

        signalingService.handleCallEvent(callEvent);
    }

    @PostConstruct
    public void startServer() {
        server.start();
        server.addListeners(this);
        log.info("🚀 Socket server đã khởi động");
    }

    @PreDestroy
    public void stopServer() {
        server.stop();
        log.info("🛑 Socket server đã dừng");
    }
}
