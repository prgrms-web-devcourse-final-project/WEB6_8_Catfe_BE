package com.back.global.websocket.webrtc.service;

import com.back.domain.studyroom.entity.RoomMember;
import com.back.domain.studyroom.repository.RoomMemberRepository;
import com.back.global.exception.CustomException;
import com.back.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * WebRTC 시그널링 메시지 검증
 * - 같은 방에 있는지 확인
 * - 자기 자신에게 보내는지 확인
 * - 온라인 상태인지 확인
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WebRTCSignalValidator {

    private final RoomMemberRepository roomMemberRepository;

    // WebRTC 시그널 검증
    public void validateSignal(Long roomId, Long fromUserId, Long targetUserId) {
        // 1. 자기 자신에게 보내는지 확인
        if (fromUserId.equals(targetUserId)) {
            log.warn("자기 자신에게 시그널 전송 시도 - userId: {}", fromUserId);
            throw new CustomException(ErrorCode.BAD_REQUEST);
        }

        // 2. 발신자가 방에 속해있는지 확인
        // TODO: Redis에서 온라인 상태 확인하도록 변경
        Optional<RoomMember> fromMember = roomMemberRepository.findByRoomIdAndUserId(roomId, fromUserId);
        if (fromMember.isEmpty()) {
            log.warn("방에 속하지 않은 사용자의 시그널 전송 시도 - roomId: {}, userId: {}", roomId, fromUserId);
            throw new CustomException(ErrorCode.NOT_ROOM_MEMBER);
        }

        // 3. 수신자가 같은 방에 속해있는지 확인
        // TODO: Redis에서 온라인 상태 확인하도록 변경
        Optional<RoomMember> targetMember = roomMemberRepository.findByRoomIdAndUserId(roomId, targetUserId);
        if (targetMember.isEmpty()) {
            log.warn("수신자가 방에 없거나 오프라인 상태 - roomId: {}, targetUserId: {}", roomId, targetUserId);
            throw new CustomException(ErrorCode.NOT_ROOM_MEMBER);
        }

        log.debug("WebRTC 시그널 검증 통과 - roomId: {}, from: {}, to: {}", roomId, fromUserId, targetUserId);
    }

    // 미디어 상태 변경 검증
    public void validateMediaStateChange(Long roomId, Long userId) {
        // TODO: Redis에서 온라인 상태 확인하도록 변경
        Optional<RoomMember> member = roomMemberRepository.findByRoomIdAndUserId(roomId, userId);

        if (member.isEmpty()) {
            log.warn("방에 속하지 않은 사용자의 미디어 상태 변경 시도 - roomId: {}, userId: {}", roomId, userId);
            throw new CustomException(ErrorCode.NOT_ROOM_MEMBER);
        }

        log.debug("미디어 상태 변경 검증 통과 - roomId: {}, userId: {}", roomId, userId);
    }
}