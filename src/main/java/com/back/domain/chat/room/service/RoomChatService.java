package com.back.domain.chat.room.service;

import com.back.domain.chat.room.dto.ChatClearedNotification;
import com.back.domain.studyroom.entity.Room;
import com.back.domain.studyroom.entity.RoomChatMessage;
import com.back.domain.studyroom.entity.RoomMember;
import com.back.domain.studyroom.entity.RoomRole;
import com.back.domain.studyroom.repository.RoomChatMessageRepository;
import com.back.domain.studyroom.repository.RoomMemberRepository;
import com.back.domain.studyroom.repository.RoomRepository;
import com.back.domain.user.entity.User;
import com.back.domain.user.repository.UserRepository;
import com.back.domain.chat.room.dto.RoomChatMessageDto;
import com.back.domain.chat.room.dto.RoomChatPageResponse;
import com.back.global.exception.CustomException;
import com.back.global.exception.ErrorCode;
import com.back.global.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RoomChatService {

    private final RoomChatMessageRepository roomChatMessageRepository;
    private final RoomMemberRepository roomMemberRepository;
    private final RoomRepository roomRepository;
    private final UserRepository userRepository;
    private final CurrentUser currentUser;

    // 페이징 설정 상수
    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 100;

    // 방 채팅 메시지 저장
    @Transactional
    public RoomChatMessage saveRoomChatMessage(RoomChatMessageDto roomChatMessageDto) {

        // 방 존재 여부 확인
        Room room = roomRepository.findById(roomChatMessageDto.roomId())
                .orElseThrow(() -> new CustomException(ErrorCode.ROOM_NOT_FOUND));

        // 사용자 존재 여부 확인
        User user = userRepository.findById(roomChatMessageDto.userId())
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        // RoomChatMessage 엔티티 생성 및 저장
        RoomChatMessage message = new RoomChatMessage(room, user, roomChatMessageDto.content());
        RoomChatMessage savedMessage = roomChatMessageRepository.save(message);

        return savedMessage;
    }

    // 방 채팅 기록 조회
    public RoomChatPageResponse getRoomChatHistory(Long roomId, int page, int size, LocalDateTime before) {

        // 방 존재 여부 확인
        roomRepository.findById(roomId)
                .orElseThrow(() -> new CustomException(ErrorCode.ROOM_NOT_FOUND));

        // size 값 검증 및 제한
        int validatedSize = validateAndLimitPageSize(size);

        Pageable pageable = PageRequest.of(page, validatedSize);

        // before 파라미터가 있으면 해당 시점 이전 메시지만 조회
        Page<RoomChatMessage> messagesPage;
        if (before != null) {
            messagesPage = roomChatMessageRepository.findMessagesByRoomIdBefore(roomId, before, pageable);
        } else {
            messagesPage = roomChatMessageRepository.findMessagesByRoomId(roomId, pageable);
        }

        List<RoomChatMessageDto> convertedContent = messagesPage.getContent()
                .stream()
                .map(this::convertToDto)
                .toList();

        return RoomChatPageResponse.from(messagesPage, convertedContent);
    }

    // 방 채팅 메시지 전체 삭제
    @Transactional
    public ChatClearedNotification.ClearedByDto clearRoomChat(Long roomId, Long userId) {

        // 방 존재 여부 확인
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 방입니다"));

        // 사용자 존재 여부 확인
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        // 사용자가 해당 방의 멤버인지 확인
        RoomMember roomMember = roomMemberRepository.findByRoomAndUser(room, user)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_ROOM_MEMBER));

        // 권한 확인 - 방장(HOST) 또는 부방장(SUB_HOST)만 가능
        if (!canManageChat(roomMember.getRole())) {
            throw new SecurityException("채팅 삭제 권한이 없습니다");
        }

        // 삭제 전 메시지 수 조회 (로깅용)
        int messageCountBefore = roomChatMessageRepository.countByRoomId(roomId);

        try {
            // 해당 방의 모든 채팅 메시지 삭제
            int deletedCount = roomChatMessageRepository.deleteAllByRoomId(roomId);

            // 삭제를 실행한 사용자 정보 반환
            return new ChatClearedNotification.ClearedByDto(
                    user.getId(),
                    user.getNickname(),
                    user.getProfileImageUrl(),
                    roomMember.getRole().name()
            );

        } catch (Exception e) {
            throw new CustomException(ErrorCode.CHAT_DELETE_FAILED);
        }
    }

    // 채팅 관리 권한 확인 (방장 또는 부방장)
    private boolean canManageChat(RoomRole role) {
        return role == RoomRole.HOST || role == RoomRole.SUB_HOST;
    }

    // size 값 검증 및 최대값 제한
    private int validateAndLimitPageSize(int size) {
        if (size <= 0) {
            return DEFAULT_PAGE_SIZE; // 0 이하면 기본값 사용
        }
        return Math.min(size, MAX_PAGE_SIZE); // 최대값 제한
    }

    // 메시지 엔티티를 DTO로 변환
    private RoomChatMessageDto convertToDto(RoomChatMessage message) {
        return RoomChatMessageDto.createResponse(
                message.getId(),
                message.getRoom().getId(),
                message.getUser().getId(),
                message.getUser().getNickname(),
                message.getUser().getProfileImageUrl(),
                message.getContent(),
                "TEXT", // 현재는 텍스트만 지원
                null,   // 텍스트 채팅에서는 null
                message.getCreatedAt()
        );
    }

}