package com.minhtung.java_be.service;

import com.corundumstudio.socketio.SocketIOClient;
import com.corundumstudio.socketio.SocketIOServer;
import com.minhtung.java_be.dto.signaling.*;
import com.minhtung.java_be.entity.ParticipantInfo;
import com.minhtung.java_be.entity.WebSocketSession;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class SignalingService {
    SocketIOServer server;
    WebSocketSessionService webSocketSessionService;
    ConversationService conversationService;

    public void handleCallOffer(CallOffer callOffer) {
        log.info("📞 [BE] Nhận call-offer từ userId={} conversationId={}",
                callOffer.getCallerId(), callOffer.getConversationId());

        // Lấy danh sách participant
        List<String> participants = conversationService.getParticipantIds(callOffer.getConversationId());
        log.info("👥 Participants trong conversation: {}", participants);

        if (participants.isEmpty()) {
            log.warn("⚠️ Không có participant nào trong conversation {}", callOffer.getConversationId());
            return;
        }

        // Kiểm tra callerId có hợp lệ không
        if (!participants.contains(callOffer.getCallerId())) {
            log.warn("⚠️ CallerId {} không nằm trong conversation {}",
                    callOffer.getCallerId(), callOffer.getConversationId());
            return;
        }

        // Lấy thông tin callerInfo từ Conversation
        ParticipantInfo caller = conversationService.getParticipantInfo(
                callOffer.getConversationId(), callOffer.getCallerId());

        CallerInfo callerInfo = CallerInfo.builder()
                .username(caller.getUsername())
                .firstName(caller.getFirstName())
                .lastName(caller.getLastName())
                .avatar(caller.getAvatar())
                .displayName(
                        (caller.getFirstName() != null ? caller.getFirstName() + " " : "")
                                + (caller.getLastName() != null ? caller.getLastName() : caller.getUsername())
                )
                .build();

        callOffer.setCallerInfo(callerInfo);
        log.info("✅ CallerInfo được gắn vào callOffer: {}", callOffer.getCallerInfo());

        // Gửi cho tất cả participant khác
        for (String userId : participants) {
            if (!userId.equals(callOffer.getCallerId())) {
                sendToUser(userId, "incoming-call", callOffer);
            }
        }
    }


    public void handleCallAnswer(CallAnswer callAnswer) {
        log.info("📞 [BE] Nhận call-answer từ conversationId={}, data={}",
                callAnswer.getConversationId(), callAnswer);

        List<String> participants = conversationService.getParticipantIds(callAnswer.getConversationId());
        for (String userId : participants) {
            sendToUser(userId, "call-answered", callAnswer);
        }
    }

    public void handleIceCandidate(IceCandidate iceCandidate) {
        log.info("📡 [BE] Nhận ice-candidate từ conversationId={}, data={}",
                iceCandidate.getConversationId(), iceCandidate);

        List<String> participants = conversationService.getParticipantIds(iceCandidate.getConversationId());
        for (String userId : participants) {
            sendToUser(userId, "ice-candidate", iceCandidate);
        }
    }

    public void handleCallEvent(CallEvent callEvent) {
        log.info("📢 [BE] Nhận call-event={} từ conversationId={}, data={}",
                callEvent.getEvent(), callEvent.getConversationId(), callEvent);

        List<String> participants = conversationService.getParticipantIds(callEvent.getConversationId());
        for (String userId : participants) {
            sendToUser(userId, "call-status", callEvent);
        }
    }

    private void sendToUser(String userId, String event, Object data) {
        List<WebSocketSession> sessions = webSocketSessionService.findByUserId(userId);
        log.info("🔍 Tìm thấy {} sessions cho user {}", sessions.size(), userId);

        for (WebSocketSession session : sessions) {
            SocketIOClient client = server.getClient(UUID.fromString(session.getSocketSessionId()));
            if (client != null && client.isChannelOpen()) {
                log.info("⬇️ [BE->FE] Emit event [{}] tới user={} session={}, data={}",
                        event, userId, session.getSocketSessionId(), data);
                client.sendEvent(event, data);
            } else {
                log.warn("❌ Client không tồn tại hoặc đã đóng cho session {}", session.getSocketSessionId());
            }
        }
    }
}
