package com.minhtung.java_be.service;

import com.minhtung.java_be.dto.request.ConversationRequest;
import com.minhtung.java_be.dto.response.ConversationResponse;
import com.minhtung.java_be.entity.ChatMessage;
import com.minhtung.java_be.entity.Conversation;
import com.minhtung.java_be.entity.ParticipantInfo;
import com.minhtung.java_be.entity.User;
import com.minhtung.java_be.exception.AppException;
import com.minhtung.java_be.exception.ErrorCode;
import com.minhtung.java_be.mapper.ConversationMapper;
import com.minhtung.java_be.repository.ChatMessageRepository;
import com.minhtung.java_be.repository.ConversationRepository;
import com.minhtung.java_be.repository.UserRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.StringJoiner;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ConversationService {
    ConversationRepository conversationRepository;
    UserRepository userRepository;
    ConversationMapper conversationMapper;
    ChatMessageRepository chatMessageRepository;

    public List<ConversationResponse> myConversations() {
        String userId = SecurityContextHolder.getContext().getAuthentication().getName();
        List<Conversation> conversations = conversationRepository.findAllByParticipantIdsContains(userId);

        return conversations.stream().map(this::toConversationResponse).toList();
    }

    public ConversationResponse create(ConversationRequest request) {
        final String currentUserId = SecurityContextHolder.getContext().getAuthentication().getName();

        if ("GROUP".equals(request.getType())) {
            List<String> participantIds = new ArrayList<>(request.getParticipantIds());
            if (!participantIds.contains(currentUserId)) {
                participantIds.add(currentUserId);
            }
            if (participantIds.size() < 3) { // nhóm phải >= 3 người (bao gồm cả mình)
                throw new AppException(ErrorCode.USER_NOT_EXISTED); // hoặc lỗi riêng "GROUP_TOO_FEW_MEMBERS"
            }
            List<ParticipantInfo> participants = participantIds.stream().map(id -> {
                User user = userRepository.findById(id)
                        .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
                return ParticipantInfo.builder()
                        .userId(user.getId())
                        .username(user.getUsername())
                        .firstName(user.getFirstName())
                        .lastName(user.getLastName())
                        .avatar(user.getAvatar())
                        .build();
            }).toList();

            String participantsHash = generateParticipantHash(participantIds.stream().sorted().toList());

            Conversation conversation = Conversation.builder()
                    .type("GROUP")
                    .participantsHash(participantsHash)
                    .participants(participants)
                    .conversationName(request.getName())
                    .conversationAvatar(request.getAvatarGroup())
                    .createdDate(Instant.now())
                    .modifiedDate(Instant.now())
                    .build();

            conversation = conversationRepository.save(conversation);
            return toConversationResponse(conversation);
        }

        // 2) Lấy participantId đầu tiên từ request và validate
        final String participantId = request.getParticipantIds()
                .stream().findFirst()
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED)); // hoặc lỗi riêng "EMPTY_PARTICIPANTS"

        if (currentUserId.equals(participantId)) {
            throw new AppException(ErrorCode.USER_NOT_EXISTED); // hoặc lỗi riêng "SELF_CONVERSATION_NOT_ALLOWED"
        }

        // 3) Load 2 user từ DB
        final User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
        final User participantUser = userRepository.findById(participantId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        // 4) Tạo hash theo 2 userId (sort để tránh A-B khác B-A)
        final List<String> sortedIds = List.of(currentUserId, participantId).stream().sorted().toList();
        final String participantsHash = generateParticipantHash(sortedIds);

        // 5) Tìm conversation cũ hoặc tạo mới
        Conversation conversation = conversationRepository.findByParticipantsHash(participantsHash)
                .orElseGet(() -> {
                    List<ParticipantInfo> participants = List.of(
                            ParticipantInfo.builder()
                                    .userId(currentUser.getId())        // hoặc getUserId() tùy entity
                                    .username(currentUser.getUsername())
                                    .firstName(currentUser.getFirstName())
                                    .lastName(currentUser.getLastName())
                                    .avatar(currentUser.getAvatar())
                                    .build(),
                            ParticipantInfo.builder()
                                    .userId(participantUser.getId())
                                    .username(participantUser.getUsername())
                                    .firstName(participantUser.getFirstName())
                                    .lastName(participantUser.getLastName())
                                    .avatar(participantUser.getAvatar())
                                    .build()
                    );

                    Conversation newConv = Conversation.builder()
                            .type(request.getType())
                            .participantsHash(participantsHash)
                            .createdDate(Instant.now())
                            .modifiedDate(Instant.now())
                            .participants(participants)
                            .build();

                    return conversationRepository.save(newConv);
                });

        // 6) Trả response
        return toConversationResponse(conversation);
    }
    private String generateParticipantHash(List<String> ids) {
        StringJoiner stringJoiner = new StringJoiner("_");
        ids.forEach(stringJoiner::add);

        // SHA 256

        return stringJoiner.toString();
    }

    private ConversationResponse toConversationResponse(Conversation conversation) {
        String currentUserId = SecurityContextHolder.getContext().getAuthentication().getName();

        // Lấy tin nhắn cuối cùng
        ChatMessage lastMessage = chatMessageRepository
                .findFirstByConversationIdOrderByCreatedDateDesc(conversation.getId())
                .orElse(null);

        // Map cơ bản
        ConversationResponse conversationResponse = conversationMapper.toConversationResponse(conversation);

        if ("DIRECT".equals(conversation.getType())) {
            // Gán tên đối phương (nếu là chat 1-1)
            conversation.getParticipants().stream()
                    .filter(participantInfo -> !participantInfo.getUserId().equals(currentUserId))
                    .findFirst().ifPresent(participantInfo -> {
                        conversationResponse.setConversationName(participantInfo.getUsername());
                    });
        } else if ("GROUP".equals(conversation.getType())) {
            // Gán tên nhóm (nếu là group)
            conversationResponse.setConversationName(conversation.getConversationName());
        }

        // Gán thêm thông tin lastMessage
        if (lastMessage != null) {
            conversationResponse.setLastMessage(lastMessage.getMessage());
            conversationResponse.setLastMessageSender(
                    lastMessage.getSender() != null ? lastMessage.getSender().getUsername() : null
            );
        }

        return conversationResponse;
    }
    public List<String> getParticipantIds(String conversationId) {
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new AppException(ErrorCode.CONVERSATION_NOT_FOUND));

        return conversation.getParticipants()
                .stream()
                .map(ParticipantInfo::getUserId)
                .toList();
    }


}
