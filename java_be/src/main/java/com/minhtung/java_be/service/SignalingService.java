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
    UserService userService; // Thêm dependency này
    UserRepository userRepository;

    public void handleCallOffer(CallOffer callOffer) {
        log.info("Xử lý call offer từ {} đến {}", callOffer.getCallerId(), callOffer.getCalleeId());

        // Kiểm tra hợp lệ của call offer
        if (callOffer.getCallerId() == null || callOffer.getCalleeId() == null) {
            log.error("Call offer không hợp lệ: thiếu callerId hoặc calleeId");
            return;
        }

        try {
            // Bổ sung thông tin cho caller
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
                log.info("Đã bổ sung thông tin caller: {}", callerInfo.getDisplayName());
            } else {
                log.warn("Không tìm thấy caller: {}", callOffer.getCallerId());
                // Gán thông tin caller mặc định
                CallerInfo fallbackInfo = CallerInfo.builder()
                        .username("Unknown User")
                        .displayName("Unknown User")
                        .build();
                callOffer.setCallerInfo(fallbackInfo);
            }

            // Kiểm tra callee có đang online không
            List<WebSocketSession> calleeSession = webSocketSessionService.findByUserId(callOffer.getCalleeId());
            if (calleeSession.isEmpty()) {
                log.warn("Callee {} không online", callOffer.getCalleeId());
                // Gửi cho caller biết callee offline
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

            // Tạo call ID nếu chưa có
            if (callOffer.getCallId() == null || callOffer.getCallId().isEmpty()) {
                callOffer.setCallId(UUID.randomUUID().toString());
            }

            // Gửi offer đến callee
            sendToUser(callOffer.getCalleeId(), "incoming-call", callOffer);

            log.info("Đã forward call offer đến {} với thông tin caller: {}",
                    callOffer.getCalleeId(),
                    callOffer.getCallerInfo().getDisplayName());

        } catch (Exception e) {
            log.error("Lỗi khi xử lý call offer: {}", e.getMessage(), e);

            // Gửi lỗi về cho caller
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
        log.info("Xử lý call answer từ {} đến {}", callAnswer.getCalleeId(), callAnswer.getCallerId());
        sendToUser(callAnswer.getCallerId(), "call-answered", callAnswer);
    }

    public void handleIceCandidate(IceCandidate iceCandidate) {
        log.info("Xử lý ICE candidate từ {} đến {}", iceCandidate.getFromUserId(), iceCandidate.getToUserId());
        sendToUser(iceCandidate.getToUserId(), "ice-candidate", iceCandidate);
    }

    public void handleCallEvent(CallEvent callEvent) {
        log.info("Xử lý call event: {} cho cuộc gọi {}", callEvent.getEvent(), callEvent.getCallId());

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
            log.warn("User {} không online", userId);
            return;
        }

        for (WebSocketSession session : sessions) {
            try {
                UUID sessionUUID = UUID.fromString(session.getSocketSessionId());
                SocketIOClient client = server.getClient(sessionUUID);

                if (client != null && client.isChannelOpen()) {
                    client.sendEvent(event, data);
                    log.debug("Đã gửi event {} đến user {} (session {})", event, userId, sessionUUID);
                } else {
                    log.warn("Session {} của user {} đã chết", sessionUUID, userId);
                    webSocketSessionService.deleteSession(session.getSocketSessionId());
                }
            } catch (IllegalArgumentException e) {
                log.error("UUID không hợp lệ cho session: {}", session.getSocketSessionId());
            }
        }
    }

    public boolean isUserOnline(String userId) {
        return webSocketSessionService.isUserOnline(userId);
    }
}
