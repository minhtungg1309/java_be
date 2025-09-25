package com.minhtung.java_be.mapper;

import com.minhtung.java_be.dto.response.ConversationResponse;
import com.minhtung.java_be.entity.Conversation;
import com.minhtung.java_be.entity.ParticipantInfo;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    comments = "version: 1.5.5.Final, compiler: javac, environment: Java 21.0.8 (Microsoft)"
)
@Component
public class ConversationMapperImpl implements ConversationMapper {

    @Override
    public ConversationResponse toConversationResponse(Conversation conversation) {
        if ( conversation == null ) {
            return null;
        }

        ConversationResponse.ConversationResponseBuilder conversationResponse = ConversationResponse.builder();

        conversationResponse.id( conversation.getId() );
        conversationResponse.type( conversation.getType() );
        conversationResponse.participantsHash( conversation.getParticipantsHash() );
        conversationResponse.conversationAvatar( conversation.getConversationAvatar() );
        conversationResponse.conversationName( conversation.getConversationName() );
        List<ParticipantInfo> list = conversation.getParticipants();
        if ( list != null ) {
            conversationResponse.participants( new ArrayList<ParticipantInfo>( list ) );
        }
        conversationResponse.createdDate( conversation.getCreatedDate() );
        conversationResponse.modifiedDate( conversation.getModifiedDate() );

        return conversationResponse.build();
    }

    @Override
    public List<ConversationResponse> toConversationResponseList(List<Conversation> conversations) {
        if ( conversations == null ) {
            return null;
        }

        List<ConversationResponse> list = new ArrayList<ConversationResponse>( conversations.size() );
        for ( Conversation conversation : conversations ) {
            list.add( toConversationResponse( conversation ) );
        }

        return list;
    }
}
