package com.minhtung.java_be.service;

import com.minhtung.java_be.entity.WebSocketSession;
import com.minhtung.java_be.repository.WebSocketSessionRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class WebSocketSessionService {

    WebSocketSessionRepository webSocketSessionRepository;

    public WebSocketSession create(WebSocketSession webSocketSession) {
        return webSocketSessionRepository.save(webSocketSession);
    }

    public void deleteSession(String sessionId) {
        webSocketSessionRepository.deleteBySocketSessionId(sessionId);
    }

    public void deleteSessionsByUserId(String userId) {
        webSocketSessionRepository.deleteByUserId(userId);
    }

    public List<WebSocketSession> findByUserId(String userId) {
        return webSocketSessionRepository.findAllByUserId(userId);
    }

    public List<WebSocketSession> findAllActiveSessions() {
        return webSocketSessionRepository.findAll();
    }

    public boolean isUserOnline(String userId) {
        return !webSocketSessionRepository.findAllByUserId(userId).isEmpty();
    }
}
