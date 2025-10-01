package com.minhtung.java_be.repository;

import com.minhtung.java_be.entity.WebSocketSession;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WebSocketSessionRepository
        extends MongoRepository<WebSocketSession, String> {

    void deleteBySocketSessionId(String socketId);

    void deleteByUserId(String userId);

    List<WebSocketSession> findAllByUserId(String userId);

    List<WebSocketSession> findAllByUserIdIn(List<String> userIds);
}
