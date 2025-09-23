package com.back.domain.chat.service;

import com.back.domain.studyroom.entity.Room;
import com.back.domain.studyroom.entity.RoomChatMessage;
import com.back.domain.studyroom.repository.RoomChatMessageRepository;
import com.back.domain.studyroom.repository.RoomRepository;
import com.back.domain.user.entity.User;
import com.back.domain.user.repository.UserRepository;
import com.back.domain.chat.dto.ChatMessageDto;
import com.back.domain.chat.dto.ChatPageResponse;
import com.back.global.exception.CustomException;
import com.back.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatService {

    private final RoomChatMessageRepository roomChatMessageRepository;
    private final RoomRepository roomRepository;
    private final UserRepository userRepository;

    // 방 채팅 메시지 저장
    @Transactional
    public RoomChatMessage saveRoomChatMessage(ChatMessageDto chatMessageDto) {

        // 방 존재 여부 확인
        Room room = roomRepository.findById(chatMessageDto.getRoomId())
                .orElseThrow(() -> new CustomException(ErrorCode.ROOM_NOT_FOUND));

        // 사용자 존재 여부 확인
        User user = userRepository.findById(chatMessageDto.getUserId())
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        // RoomChatMessage 엔티티 생성 및 저장
        RoomChatMessage message = new RoomChatMessage(room, user, chatMessageDto.getContent());
        RoomChatMessage savedMessage = roomChatMessageRepository.save(message);

        return savedMessage;
    }

    // 방 채팅 기록 조회
    public ChatPageResponse getRoomChatHistory(Long roomId, int page, int size, LocalDateTime before) {

        // 방 존재 여부 확인
        roomRepository.findById(roomId)
                .orElseThrow(() -> new CustomException(ErrorCode.ROOM_NOT_FOUND));

        Pageable pageable = PageRequest.of(page, size);

        // before 파라미터가 있으면 해당 시점 이전 메시지만 조회
        Page<RoomChatMessage> messagesPage;
        if (before != null) {
            // TODO: before 조건 추가한 Repository 메서드 필요
            messagesPage = roomChatMessageRepository.findByRoomIdOrderByCreatedAtDesc(roomId, pageable);
        } else {
            messagesPage = roomChatMessageRepository.findByRoomIdOrderByCreatedAtDesc(roomId, pageable);
        }

        Page<ChatMessageDto> dtoPage = messagesPage.map(this::convertToDto);

        return ChatPageResponse.from(dtoPage);
    }

    // 메시지 엔티티를 DTO로 변환
    private ChatMessageDto convertToDto(RoomChatMessage message) {
        return ChatMessageDto.builder()
                .messageId(message.getId())
                .roomId(message.getRoom().getId())
                .userId(message.getUser().getId())
                .nickname(message.getUser().getNickname())
                .profileImageUrl(message.getUser().getProfileImageUrl())
                .content(message.getContent())
                .messageType("TEXT") // 현재는 텍스트만 지원
                .attachment(null)    // 텍스트 채팅에서는 null
                .createdAt(message.getCreatedAt())
                .build();
    }

    // 방 채팅 메시지 삭제
    @Transactional
    public void deleteRoomMessage(Long roomId, Long messageId, Long currentUserId) {
        // 메시지 존재 여부 확인
        RoomChatMessage message = roomChatMessageRepository.findById(messageId)
                .orElseThrow(() -> new CustomException(ErrorCode.MESSAGE_NOT_FOUND));

        // 방 ID 검증
        if (!message.getRoom().getId().equals(roomId)) {
            throw new CustomException(ErrorCode.BAD_REQUEST);
        }

        // 작성자 권한 확인
        if (!message.getUser().getId().equals(currentUserId)) {
            throw new CustomException(ErrorCode.MESSAGE_FORBIDDEN);
        }

        // 메시지 삭제
        roomChatMessageRepository.delete(message);
    }
}