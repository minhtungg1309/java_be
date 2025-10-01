package com.minhtung.java_be.mapper;

import com.minhtung.java_be.dto.response.ConversationResponse;
import com.minhtung.java_be.entity.Conversation;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ConversationMapper {
    ConversationResponse toConversationResponse(Conversation conversation);

    List<ConversationResponse> toConversationResponseList(List<Conversation> conversations);
}
