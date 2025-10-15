package com.back.domain.studyroom.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Random;

/**
 * 아바타 관리 서비스 (완전 분리 버전)
 * - DB 저장 없음, 숫자만 관리
 * - 프론트엔드에서 avatarId로 이미지 매핑
 * - Redis에만 저장 (WebSocket 연결 시)
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class AvatarService {
    
    private final com.back.global.websocket.service.RoomParticipantService roomParticipantService;
    
    // 기본 아바타 ID 범위 (1~3)
    private static final int MIN_AVATAR_ID = 1;
    private static final int MAX_AVATAR_ID = 3;
    
    /**
     * 방 입장 시 아바타 ID 로드 또는 생성
     * - Redis에 저장된 아바타가 있으면 반환
     * - 없으면 랜덤 생성 (1~3)
     * 
     * @param roomId 방 ID
     * @param userId 사용자 ID
     * @return 사용할 아바타 ID (숫자)
     */
    public Long loadOrCreateAvatar(Long roomId, Long userId) {
        // 1. Redis에서 기존 아바타 조회
        Long existingAvatarId = roomParticipantService.getUserAvatar(roomId, userId);
        
        if (existingAvatarId != null) {
            log.debug("기존 아바타 사용 - RoomId: {}, UserId: {}, AvatarId: {}", 
                     roomId, userId, existingAvatarId);
            return existingAvatarId;
        }
        
        // 2. 없으면 랜덤 생성
        Long randomAvatarId = assignRandomAvatar();
        log.debug("새 아바타 배정 - RoomId: {}, UserId: {}, AvatarId: {}", 
                 roomId, userId, randomAvatarId);
        
        return randomAvatarId;
    }
    
    /**
     * 랜덤 아바타 배정 (1~3 중 랜덤)
     * @return 아바타 ID
     */
    public Long assignRandomAvatar() {
        Random random = new Random();
        int avatarId = random.nextInt(MAX_AVATAR_ID - MIN_AVATAR_ID + 1) + MIN_AVATAR_ID;
        return (long) avatarId;
    }
    
    /**
     * 아바타 변경
     * - DB 검증 없이 숫자만 저장
     * - Redis에만 저장 (VISITOR든 MEMBER든 동일)
     * 
     * @param roomId 방 ID
     * @param userId 사용자 ID
     * @param newAvatarId 새 아바타 ID (프론트에서 전달한 숫자)
     */
    @Transactional
    public void updateRoomAvatar(Long roomId, Long userId, Long newAvatarId) {
        // Redis에 아바타 업데이트 (모든 사용자 공통)
        roomParticipantService.updateUserAvatar(roomId, userId, newAvatarId);
        
        log.info("아바타 변경 완료 (Redis) - RoomId: {}, UserId: {}, AvatarId: {}", 
                roomId, userId, newAvatarId);
    }
}
