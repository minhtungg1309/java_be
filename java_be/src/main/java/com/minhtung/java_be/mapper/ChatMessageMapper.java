package com.minhtung.java_be.mapper;

import com.minhtung.java_be.dto.request.ChatMessageRequest;
import com.minhtung.java_be.dto.response.ChatMessageResponse;
import com.minhtung.java_be.entity.ChatMessage;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ChatMessageMapper {
    ChatMessageResponse toChatMessageResponse(ChatMessage chatMessage);

    ChatMessage toChatMessage(ChatMessageRequest request);

    List<ChatMessageResponse> toChatMessageResponses(List<ChatMessage> chatMessages);
}
