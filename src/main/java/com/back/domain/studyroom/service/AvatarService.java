package com.back.domain.studyroom.service;

import com.back.domain.studyroom.dto.AvatarResponse;
import com.back.domain.studyroom.entity.Avatar;
import com.back.domain.studyroom.entity.RoomMember;
import com.back.domain.studyroom.entity.RoomMemberAvatar;
import com.back.domain.studyroom.entity.RoomRole;
import com.back.domain.studyroom.repository.AvatarRepository;
import com.back.domain.studyroom.repository.RoomMemberAvatarRepository;
import com.back.domain.studyroom.repository.RoomMemberRepository;
import com.back.domain.user.entity.User;
import com.back.domain.user.repository.UserRepository;
import com.back.global.exception.CustomException;
import com.back.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 아바타 관리 서비스
 * - VISITOR: 랜덤 아바타 배정 (DB 저장 안함)
 * - MEMBER 이상: 아바타 변경 시 DB 저장
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class AvatarService {
    
    private final AvatarRepository avatarRepository;
    private final RoomMemberAvatarRepository roomMemberAvatarRepository;
    private final RoomMemberRepository roomMemberRepository;
    private final UserRepository userRepository;
    private final com.back.global.websocket.service.RoomParticipantService roomParticipantService;  // ⭐ 추가
    
    // 기본 아바타 ID 캐시 (애플리케이션 시작 시 로드)
    private List<Long> defaultAvatarIds = null;
    
    /**
     * 방 입장 시 아바타 로드 또는 생성
     * @param roomId 방 ID
     * @param userId 사용자 ID
     * @return 사용할 아바타 ID
     */
    public Long loadOrCreateAvatar(Long roomId, Long userId) {
        // 1. MEMBER 이상인지 확인
        Optional<RoomMember> memberOpt = 
            roomMemberRepository.findByRoomIdAndUserId(roomId, userId);
        
        if (memberOpt.isEmpty()) {
            // VISITOR → 랜덤 아바타 배정
            log.debug("VISITOR 입장 - RoomId: {}, UserId: {}, 랜덤 아바타 배정", roomId, userId);
            return assignRandomAvatar();
        }
        
        RoomMember member = memberOpt.get();
        
        // 2. MEMBER 이상 → DB에서 저장된 아바타 조회
        Optional<RoomMemberAvatar> savedAvatar = 
            roomMemberAvatarRepository.findByRoomIdAndUserId(roomId, userId);
        
        if (savedAvatar.isPresent()) {
            // 이전에 설정한 아바타 있음
            Long avatarId = savedAvatar.get().getSelectedAvatar().getId();
            log.debug("MEMBER 재입장 - RoomId: {}, UserId: {}, 저장된 아바타: {}", 
                     roomId, userId, avatarId);
            return avatarId;
        }
        
        // 3. MEMBER 이상이지만 아직 아바타 미설정 → 랜덤 배정 (DB 저장 안함)
        log.debug("MEMBER 첫 입장 - RoomId: {}, UserId: {}, 랜덤 아바타 배정", roomId, userId);
        return assignRandomAvatar();
    }
    
    /**
     * 랜덤 아바타 배정 (기본 아바타 중 랜덤 선택)
     * @return 아바타 ID
     */
    public Long assignRandomAvatar() {
        // 기본 아바타 목록 캐싱
        if (defaultAvatarIds == null || defaultAvatarIds.isEmpty()) {
            loadDefaultAvatars();
        }
        
        if (defaultAvatarIds.isEmpty()) {
            // 기본 아바타가 없으면 첫 번째 아바타 반환
            log.warn("기본 아바타가 없습니다. 첫 번째 아바타를 사용합니다.");
            Avatar firstAvatar = avatarRepository.findAll().stream()
                .findFirst()
                .orElseThrow(() -> new CustomException(ErrorCode.AVATAR_NOT_FOUND));
            return firstAvatar.getId();
        }
        
        Random random = new Random();
        int index = random.nextInt(defaultAvatarIds.size());
        Long selectedId = defaultAvatarIds.get(index);
        
        log.debug("랜덤 아바타 선택: {} (총 {}개 중)", selectedId, defaultAvatarIds.size());
        return selectedId;
    }
    
    /**
     * 기본 아바타 목록 로드
     */
    private void loadDefaultAvatars() {
        List<Avatar> defaultAvatars = avatarRepository.findByIsDefaultTrueOrderBySortOrderAsc();
        defaultAvatarIds = defaultAvatars.stream()
            .map(Avatar::getId)
            .collect(Collectors.toList());
        
        log.info("기본 아바타 로드 완료: {}개", defaultAvatarIds.size());
    }
    
    /**
     * 아바타 변경
     * - VISITOR: Redis에만 저장 (퇴장 시 삭제)
     * - MEMBER 이상: Redis + DB 저장 (재입장 시 유지)
     * @param roomId 방 ID
     * @param userId 사용자 ID
     * @param newAvatarId 새 아바타 ID
     */
    @Transactional
    public void updateRoomAvatar(Long roomId, Long userId, Long newAvatarId) {
        // 1. 선택한 아바타 존재 확인
        Avatar newAvatar = avatarRepository.findById(newAvatarId)
            .orElseThrow(() -> new CustomException(ErrorCode.AVATAR_NOT_FOUND));
        
        // 2. 방 멤버 여부 확인 (VISITOR도 가능하도록 Optional 사용)
        Optional<RoomMember> memberOpt = roomMemberRepository
            .findByRoomIdAndUserId(roomId, userId);
        
        // 3-1. MEMBER 이상인 경우: DB에 저장
        if (memberOpt.isPresent()) {
            RoomMember member = memberOpt.get();
            
            // DB에 저장 (최초 또는 업데이트)
            RoomMemberAvatar roomAvatar = roomMemberAvatarRepository
                .findByRoomIdAndUserId(roomId, userId)
                .orElse(RoomMemberAvatar.builder()
                        .room(member.getRoom())
                        .user(member.getUser())
                        .build());
            
            roomAvatar.setSelectedAvatar(newAvatar);
            roomMemberAvatarRepository.save(roomAvatar);
            
            log.info("아바타 변경 완료 (DB 저장) - RoomId: {}, UserId: {}, Role: {}, AvatarId: {}", 
                     roomId, userId, member.getRole(), newAvatarId);
        } 
        // 3-2. VISITOR인 경우: Redis에만 저장 (DB 저장 안함)
        else {
            log.info("아바타 변경 완료 (Redis만 저장) - RoomId: {}, UserId: {}, Role: VISITOR, AvatarId: {}", 
                     roomId, userId, newAvatarId);
        }
        
        // 4. Redis에 아바타 업데이트 (모든 사용자 공통)
        roomParticipantService.updateUserAvatar(roomId, userId, newAvatarId);
    }
    
    /**
     * 사용 가능한 아바타 목록 조회
     */
    public List<AvatarResponse> getAvailableAvatars() {
        return avatarRepository.findAllByOrderBySortOrderAsc()
            .stream()
            .map(AvatarResponse::from)
            .collect(Collectors.toList());
    }
    
    /**
     * 특정 아바타 조회
     */
    public Avatar getAvatarById(Long avatarId) {
        return avatarRepository.findById(avatarId)
            .orElse(null);
    }
    
    /**
     * 여러 아바타 일괄 조회 (N+1 방지)
     */
    public Map<Long, Avatar> getAvatarsByIds(Set<Long> avatarIds) {
        return avatarRepository.findAllById(avatarIds)
            .stream()
            .collect(Collectors.toMap(Avatar::getId, avatar -> avatar));
    }
}
