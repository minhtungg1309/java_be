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
    ConversationService conversationService; // service để lấy participants

    public void handleCallOffer(CallOffer callOffer) {
        log.info("📞 Call offer for conversation: {}", callOffer.getConversationId());
        log.info("📞 Caller: {}", callOffer.getCallerId());

        // **VALIDATE: callerId không được null**
        if (callOffer.getCallerId() == null || callOffer.getCallerId().isEmpty()) {
            log.error("❌ CallerId is null or empty");
            return;
        }

        // Lấy participants từ conversation
        List<String> participants = conversationService.getParticipantIds(callOffer.getConversationId());
        log.info("🔍 Participants: {}", participants);

        if (participants.isEmpty()) {
            log.warn("❌ No participants found for conversation: {}", callOffer.getConversationId());
            return;
        }

        // **VALIDATE: callerId phải là member của conversation**
        if (!participants.contains(callOffer.getCallerId())) {
            log.warn("❌ CallerId {} is not a participant of conversation {}", 
                    callOffer.getCallerId(), callOffer.getConversationId());
            return;
        }

        // Tạo callId nếu chưa có
        if (callOffer.getCallId() == null || callOffer.getCallId().isEmpty()) {
            callOffer.setCallId(UUID.randomUUID().toString());
        }

        // **CHỈ GỬI ĐẾN CÁC PARTICIPANTS KHÁC (không gửi lại cho caller)**
        for (String userId : participants) {
            if (!userId.equals(callOffer.getCallerId())) {
                log.info("📞 Sending call to user: {}", userId);
                sendToUser(userId, "incoming-call", callOffer);
            }
        }
    }

    public void handleCallAnswer(CallAnswer callAnswer) {
        List<String> participants = conversationService.getParticipantIds(callAnswer.getConversationId());
        for (String userId : participants) {
            sendToUser(userId, "call-answered", callAnswer);
        }
    }

    public void handleIceCandidate(IceCandidate iceCandidate) {
        List<String> participants = conversationService.getParticipantIds(iceCandidate.getConversationId());
        for (String userId : participants) {
            sendToUser(userId, "ice-candidate", iceCandidate);
        }
    }

    public void handleCallEvent(CallEvent callEvent) {
        List<String> participants = conversationService.getParticipantIds(callEvent.getConversationId());
        for (String userId : participants) {
            sendToUser(userId, "call-status", callEvent);
        }
    }

    private void sendToUser(String userId, String event, Object data) {
        List<WebSocketSession> sessions = webSocketSessionService.findByUserId(userId);
        log.info("🔍 Found {} sessions for user {}", sessions.size(), userId);

        for (WebSocketSession session : sessions) {
            SocketIOClient client = server.getClient(UUID.fromString(session.getSocketSessionId()));
            if (client != null && client.isChannelOpen()) {
                log.info("✅ Sending {} to session {}", event, session.getSocketSessionId());
                client.sendEvent(event, data);
            } else {
                log.warn("❌ Client not found or closed for session {}", session.getSocketSessionId());
            }
        }
    }
}