package com.minhtung.java_be.repository;

import com.minhtung.java_be.entity.ChatMessage;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChatMessageRepository extends MongoRepository<ChatMessage, String> {
    
    List<ChatMessage> findAllByConversationIdOrderByCreatedDateDesc(String conversationId);
    
    // Lấy tin nhắn cuối cùng của conversation
    Optional<ChatMessage> findFirstByConversationIdOrderByCreatedDateDesc(String conversationId);
    
    // Chỉ giữ lại method này, bỏ method countUnreadMessages
    List<ChatMessage> findByConversationIdAndSenderUserIdNotAndIsReadFalse(String conversationId, String senderUserId);
}