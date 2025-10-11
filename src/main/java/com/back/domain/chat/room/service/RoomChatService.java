package com.back.domain.chat.room.service;

import com.back.domain.chat.room.dto.ChatClearedNotification;
import com.back.domain.chat.room.dto.RoomChatMessageRequest;
import com.back.domain.chat.room.dto.RoomChatMessageResponse;
import com.back.domain.studyroom.entity.Room;
import com.back.domain.studyroom.entity.RoomChatMessage;
import com.back.domain.studyroom.entity.RoomMember;
import com.back.domain.studyroom.entity.RoomRole;
import com.back.domain.studyroom.repository.RoomChatMessageRepository;
import com.back.domain.studyroom.repository.RoomMemberRepository;
import com.back.domain.studyroom.repository.RoomRepository;
import com.back.domain.user.entity.User;
import com.back.domain.user.repository.UserRepository;
import com.back.domain.chat.room.dto.RoomChatPageResponse;
import com.back.global.exception.CustomException;
import com.back.global.exception.ErrorCode;
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
public class RoomChatService {

    private final RoomChatMessageRepository roomChatMessageRepository;
    private final RoomMemberRepository roomMemberRepository;
    private final RoomRepository roomRepository;
    private final UserRepository userRepository;

    // 페이징 설정 상수
    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 100;

    // 방 채팅 메시지 저장
    @Transactional
    public RoomChatMessage saveRoomChatMessage(Long roomId, Long userId, RoomChatMessageRequest request) {

        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new CustomException(ErrorCode.ROOM_NOT_FOUND));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        // DTO에서 직접 content를 가져와 사용
        RoomChatMessage message = new RoomChatMessage(room, user, request.content());
        return roomChatMessageRepository.save(message);
    }

    // 방 채팅 기록 조회
    @Transactional(readOnly = true)
    public RoomChatPageResponse getRoomChatHistory(Long roomId, int page, int size, LocalDateTime before) {

        roomRepository.findById(roomId)
                .orElseThrow(() -> new CustomException(ErrorCode.ROOM_NOT_FOUND));

        int validatedSize = validateAndLimitPageSize(size);
        Pageable pageable = PageRequest.of(page, validatedSize);

        Page<RoomChatMessage> messagesPage;
        if (before != null) {
            messagesPage = roomChatMessageRepository.findMessagesByRoomIdBefore(roomId, before, pageable);
        } else {
            messagesPage = roomChatMessageRepository.findMessagesByRoomId(roomId, pageable);
        }

        List<RoomChatMessageResponse> convertedContent = messagesPage.getContent()
                .stream()
                .map(RoomChatMessageResponse::from)
                .toList();

        return RoomChatPageResponse.from(messagesPage, convertedContent);
    }

    // 방 채팅 메시지 전체 삭제
    @Transactional
    public ChatClearedNotification.ClearedByDto clearRoomChat(Long roomId, Long userId) {

        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new CustomException(ErrorCode.ROOM_NOT_FOUND));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        RoomMember roomMember = roomMemberRepository.findByRoomIdAndUserId(roomId, userId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_ROOM_MEMBER));

        if (!canManageChat(roomMember.getRole())) {
            throw new CustomException(ErrorCode.CHAT_DELETE_FORBIDDEN);
        }

        roomChatMessageRepository.deleteAllByRoomId(roomId);

        return new ChatClearedNotification.ClearedByDto(
                user.getId(),
                user.getNickname(),
                user.getProfileImageUrl(),
                roomMember.getRole().name()
        );
    }

    // 방의 현재 채팅 메시지 수 조회
    @Transactional(readOnly = true)
    public int getRoomChatCount(Long roomId) {
        return roomChatMessageRepository.countByRoomId(roomId);
    }

    // 채팅 관리 권한 확인 (방장 또는 부방장)
    private boolean canManageChat(RoomRole role) {
        return role == RoomRole.HOST || role == RoomRole.SUB_HOST;
    }

    // size 값 검증 및 최대값 제한
    private int validateAndLimitPageSize(int size) {
        if (size <= 0) {
            return DEFAULT_PAGE_SIZE;
        }
        return Math.min(size, MAX_PAGE_SIZE);
    }
}