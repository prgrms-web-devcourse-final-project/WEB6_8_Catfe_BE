package com.back.domain.studyroom.service;

import com.back.domain.notification.event.studyroom.MemberKickedEvent;
import com.back.domain.notification.event.studyroom.MemberRoleChangedEvent;
import com.back.domain.notification.event.studyroom.OwnerTransferredEvent;
import com.back.domain.studyroom.config.StudyRoomProperties;
import com.back.domain.studyroom.dto.RoomResponse;
import com.back.domain.studyroom.entity.*;
import com.back.domain.studyroom.repository.*;
import com.back.domain.user.entity.User;
import com.back.domain.user.repository.UserRepository;
import com.back.global.exception.CustomException;
import com.back.global.exception.ErrorCode;
import com.back.global.websocket.service.RoomParticipantService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 - 방 생성, 입장, 퇴장 로직 처리
 - 멤버 권한 관리 (승격, 강등, 추방)
 - 방 상태 관리 (활성화, 일시정지, 종료)
 - 방장 위임 로직 (방장이 나갈 때 자동 위임)
 - 실시간 참가자 수 동기화

 - 모든 권한 검증을 서비스 레이어에서 처리
 - 비공개 방 접근 권한 체크
 - 방장/부방장 권한이 필요한 작업들의 권한 검증

 * 설정값 주입을 StudyRoomProperties를 통해 외부 설정 관리
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class RoomService {

    private final RoomRepository roomRepository;
    private final RoomMemberRepository roomMemberRepository;
    private final UserRepository userRepository;
    private final StudyRoomProperties properties;
    private final RoomParticipantService roomParticipantService;
    private final SimpMessagingTemplate messagingTemplate;
    private final ApplicationEventPublisher eventPublisher;
    private final AvatarService avatarService;
    private final RoomThumbnailService roomThumbnailService;

    /**
     * 방 생성 메서드
     * 생성 과정:
     * 1. 사용자 존재 확인
     * 2. Room 엔티티 생성 (외부 설정값 적용)
     * 3. 방장을 RoomMember로 등록
     * 4. 참가자 수 1로 설정

     * 기본 설정:
     - 상태: WAITING (대기 중)
     - WebRTC: useWebRTC 파라미터에 따라 카메라/오디오/화면공유 통합 제어
     - 참가자 수: 0명에서 시작 후 방장 추가로 1명
     */
    @Transactional
    public Room createRoom(String title, String description, boolean isPrivate, 
                          String password, int maxParticipants, Long creatorId, boolean useWebRTC, Long thumbnailAttachmentId) {
        
        User creator = userRepository.findById(creatorId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        // 방 생성 (썸네일 URL은 null로 시작)
        Room room = Room.create(title, description, isPrivate, password, 
                maxParticipants, creator, null, useWebRTC, null);
        Room savedRoom = roomRepository.save(room);

        // 썸네일 매핑 생성 및 URL 업데이트
        if (thumbnailAttachmentId != null) {
            String thumbnailUrl = roomThumbnailService.createThumbnailMapping(
                    savedRoom.getId(), thumbnailAttachmentId);
            savedRoom.updateSettings(title, description, maxParticipants, thumbnailUrl);
        }

        // 방장 등록
        RoomMember hostMember = RoomMember.createHost(savedRoom, creator);
        roomMemberRepository.save(hostMember);

        // savedRoom.incrementParticipant();  // Redis로 이관 - DB 업데이트 제거
        
        log.info("방 생성 완료 - RoomId: {}, Title: {}, CreatorId: {}, WebRTC: {}, ThumbnailId: {}", 
                savedRoom.getId(), title, creatorId, useWebRTC, 
                thumbnailAttachmentId != null ? thumbnailAttachmentId : "없음");
        
        return savedRoom;
    }

    /**
     * 방 입장 메서드 (WebSocket 연결과 함께 사용)
     * 
     * 입장 검증 과정:
     * 1. 방 존재 확인 (비관적 락으로 동시성 제어)
     * 2. 정원 초과 여부 확인 (Redis 기반)
     * 3. 방 입장 가능 여부 확인 (활성화 + 입장 가능한 상태)
     * 4. 비공개 방인 경우 비밀번호 확인
     * 5. 이미 참여 중인지 확인 (재입장 처리)

     * 멤버 등록:
     * - 신규 사용자 (DB에 없음): VISITOR로 입장 → DB 저장 안함, Redis에만 등록
     * - 기존 멤버 (DB에 있음): 저장된 역할로 재입장 → Redis에만 등록
     * 
     * 동시성 제어: 비관적 락(PESSIMISTIC_WRITE)으로 정원 초과 방지
     */
    @Transactional
    public RoomMember joinRoom(Long roomId, String password, Long userId) {
        return joinRoom(roomId, password, userId, true);
    }

    /**
     * 방 입장 메서드 (오버로드 - WebSocket 등록 여부 선택 가능)
     * 
     * @param roomId 방 ID
     * @param password 비밀번호 (비공개 방인 경우)
     * @param userId 사용자 ID
     * @param registerOnline WebSocket 세션 등록 여부 (true: Redis 등록, false: 권한 체크만)
     * @return RoomMember (메모리상 또는 DB 저장된 객체)
     */
    @Transactional
    public RoomMember joinRoom(Long roomId, String password, Long userId, boolean registerOnline) {
        
        // 1. 비관적 락으로 방 조회 - 동시 입장 시 정원 초과 방지
        Room room = roomRepository.findByIdWithLock(roomId)
                .orElseThrow(() -> new CustomException(ErrorCode.ROOM_NOT_FOUND));

        // 2. Redis에서 현재 온라인 사용자 수 조회 (WebSocket 등록하는 경우만)
        if (registerOnline) {
            long currentOnlineCount = roomParticipantService.getParticipantCount(roomId);

            // 3. 정원 확인 (Redis 기반)
            if (currentOnlineCount >= room.getMaxParticipants()) {
                throw new CustomException(ErrorCode.ROOM_FULL);
            }
        }

        // 4. 방 입장 가능 여부 확인 (활성화 + 입장 가능한 상태)
        if (!room.canJoin()) {
            if (room.getStatus() == RoomStatus.TERMINATED) {
                throw new CustomException(ErrorCode.ROOM_TERMINATED);
            } else if (!room.isActive()) {
                throw new CustomException(ErrorCode.ROOM_INACTIVE);
            }
            throw new CustomException(ErrorCode.ROOM_NOT_JOINABLE);
        }

        // 5. 비밀번호 확인 (초대 코드 입장 시에는 password가 null일 수 있음)
        if (room.needsPassword() && password != null && !room.getPassword().equals(password)) {
            throw new CustomException(ErrorCode.ROOM_PASSWORD_INCORRECT);
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        // 아바타 로드/생성
        Long avatarId = avatarService.loadOrCreateAvatar(roomId, userId);

        Optional<RoomMember> existingMember = roomMemberRepository.findByRoomIdAndUserId(roomId, userId);
        if (existingMember.isPresent()) {
            // 기존 멤버 재입장: DB에 있는 역할 그대로 사용
            RoomMember member = existingMember.get();
            
            // WebSocket 연결과 함께 입장하는 경우에만 Redis 등록
            if (registerOnline) {
                roomParticipantService.enterRoom(userId, roomId, avatarId);
                log.info("기존 멤버 재입장 (Redis 등록) - RoomId: {}, UserId: {}, Role: {}, AvatarId: {}", 
                        roomId, userId, member.getRole(), avatarId);
            } else {
                log.info("기존 멤버 권한 확인 (Redis 등록 건너뜀) - RoomId: {}, UserId: {}, Role: {}", 
                        roomId, userId, member.getRole());
            }
            
            return member;
        }

        // 신규 입장자: VISITOR로 입장 (DB 저장 안함!)
        RoomMember visitorMember = RoomMember.createVisitor(room, user);
        
        // WebSocket 연결과 함께 입장하는 경우에만 Redis 등록
        if (registerOnline) {
            roomParticipantService.enterRoom(userId, roomId, avatarId);
            log.info("신규 입장 (VISITOR, Redis 등록) - RoomId: {}, UserId: {}, AvatarId: {}", 
                    roomId, userId, avatarId);
        } else {
            log.info("신규 입장 권한 확인 (Redis 등록 건너뜀) - RoomId: {}, UserId: {}", 
                    roomId, userId);
        }
        
        // 메모리상 객체 반환 (DB에 저장되지 않음)
        return visitorMember;
    }

    /**
     * 방 나가기 메서드
     * 
     *  퇴장 처리:
     * - VISITOR: Redis에서만 제거 (DB에 없음)
     * - MEMBER 이상: Redis에서 제거 + DB 멤버십은 유지 (재입장 시 역할 유지)
     * - 방장: Redis에서 제거 + DB 멤버십 유지 + 방은 계속 존재
     * 
     *  방은 참가자 0명이어도 유지:
     * - 방장이 오프라인이어도 다른 사람들이 입장 가능
     * - 방 종료는 오직 방장만 명시적으로 가능
     */
    @Transactional
    public void leaveRoom(Long roomId, Long userId) {
        
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new CustomException(ErrorCode.ROOM_NOT_FOUND));

        // Redis에서 퇴장 처리 (모든 사용자)
        roomParticipantService.exitRoom(userId, roomId);

        log.info("방 퇴장 완료 - RoomId: {}, UserId: {}", roomId, userId);
    }

    public Page<Room> getJoinableRooms(Pageable pageable) {
        return roomRepository.findJoinablePublicRooms(pageable);
    }

    /**
     * 모든 방 조회 (공개 + 비공개 전체)
     * 비공개 방은 정보 마스킹
     */
    public Page<Room> getAllRooms(Pageable pageable) {
        return roomRepository.findAllRooms(pageable);
    }

    /**
     * 공개 방 전체 조회
     * @param includeInactive 닫힌 방 포함 여부 (기본: true)
     */
    public Page<Room> getPublicRooms(boolean includeInactive, Pageable pageable) {
        return roomRepository.findPublicRoomsWithStatus(includeInactive, pageable);
    }

    /**
     * 내가 멤버인 비공개 방 조회
     * @param includeInactive 닫힌 방 포함 여부 (기본: true)
     */
    public Page<Room> getMyPrivateRooms(Long userId, boolean includeInactive, Pageable pageable) {
        return roomRepository.findMyPrivateRooms(userId, includeInactive, pageable);
    }

    /**
     * 내가 호스트인 방 조회
     */
    public Page<Room> getMyHostingRooms(Long userId, Pageable pageable) {
        return roomRepository.findRoomsByHostId(userId, pageable);
    }

    /**
     * 모든 방을 RoomResponse로 변환 (비공개 방 마스킹 포함)
     * @param rooms 방 목록
     * @return 마스킹된 RoomResponse 리스트
     */
    public java.util.List<RoomResponse> toRoomResponseListWithMasking(java.util.List<Room> rooms) {
        java.util.List<Long> roomIds = rooms.stream()
                .map(Room::getId)
                .collect(java.util.stream.Collectors.toList());

        // Redis Pipeline으로 일괄 조회 (N+1 해결)
        java.util.Map<Long, Long> participantCounts = roomParticipantService.getParticipantCounts(roomIds);
        
        return rooms.stream()
                .map(room -> {
                    long count = participantCounts.getOrDefault(room.getId(), 0L);
                    
                    // 비공개 방이면 마스킹된 버전 반환
                    if (room.isPrivate()) {
                        return RoomResponse.fromMasked(room);
                    }
                    
                    // 공개 방은 일반 버전 반환
                    return RoomResponse.from(room, count);
                })
                .collect(java.util.stream.Collectors.toList());
    }

    public Room getRoomDetail(Long roomId, Long userId) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new CustomException(ErrorCode.ROOM_NOT_FOUND));

        return room;
    }

    public List<Room> getUserRooms(Long userId) {
        return roomRepository.findRoomsByUserId(userId);
    }

    @Transactional
    public void updateRoomSettings(Long roomId, String title, String description, 
                                  int maxParticipants, Long thumbnailAttachmentId, Long userId) {
        
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new CustomException(ErrorCode.ROOM_NOT_FOUND));

        if (!room.isOwner(userId)) {
            throw new CustomException(ErrorCode.NOT_ROOM_MANAGER);
        }

        // Redis에서 현재 온라인 사용자 수 조회
        long currentOnlineCount = roomParticipantService.getParticipantCount(roomId);
        
        if (maxParticipants < currentOnlineCount) {
            throw new CustomException(ErrorCode.BAD_REQUEST);
        }

        // 썸네일 변경 처리
        String thumbnailUrl = room.getRawThumbnailUrl();  // 기존 URL 유지
        if (thumbnailAttachmentId != null) {
            // 기존 매핑 삭제 + 새 매핑 생성
            thumbnailUrl = roomThumbnailService.updateThumbnailMapping(
                    roomId, thumbnailAttachmentId);
        }

        room.updateSettings(title, description, maxParticipants, thumbnailUrl);
        
        log.info("방 설정 변경 완료 - RoomId: {}, UserId: {}, ThumbnailId: {}", 
                roomId, userId, thumbnailAttachmentId != null ? thumbnailAttachmentId : "변경 없음");
    }

    /**
     * 방 비밀번호 변경
     * - 방장만 변경 가능
     * - 현재 비밀번호 검증 후 변경
     * @param roomId 방 ID
     * @param currentPassword 현재 비밀번호
     * @param newPassword 새 비밀번호
     * @param userId 요청자 ID (방장)
     */
    @Transactional
    public void updateRoomPassword(Long roomId, String currentPassword, String newPassword, Long userId) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new CustomException(ErrorCode.ROOM_NOT_FOUND));
        
        // 방장 권한 확인
        if (!room.isOwner(userId)) {
            throw new CustomException(ErrorCode.NOT_ROOM_HOST);
        }
        
        // 현재 비밀번호 검증
        if (!currentPassword.equals(room.getPassword())) {
            throw new CustomException(ErrorCode.ROOM_PASSWORD_MISMATCH);
        }
        
        // 새 비밀번호 설정
        room.updatePassword(newPassword);
        
        log.info("방 비밀번호 변경 완료 - RoomId: {}, UserId: {}", roomId, userId);
    }

    /**
     * 방 비밀번호 제거 (공개방으로 전환)
     * - 방장만 제거 가능
     * @param roomId 방 ID
     * @param userId 요청자 ID (방장)
     */
    @Transactional
    public void removeRoomPassword(Long roomId, Long userId) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new CustomException(ErrorCode.ROOM_NOT_FOUND));
        
        // 방장 권한 확인
        if (!room.isOwner(userId)) {
            throw new CustomException(ErrorCode.NOT_ROOM_HOST);
        }
        
        // 비밀번호 제거
        room.updatePassword(null);
        
        log.info("방 비밀번호 제거 완료 - RoomId: {}, UserId: {}", roomId, userId);
    }

    /**
     * 방 비밀번호 설정 (비밀번호가 없는 방에 비밀번호 추가)
     * - 방장만 설정 가능
     * - 기존 비밀번호가 없는 경우에만 사용
     * @param roomId 방 ID
     * @param newPassword 새 비밀번호
     * @param userId 요청자 ID (방장)
     */
    @Transactional
    public void setRoomPassword(Long roomId, String newPassword, Long userId) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new CustomException(ErrorCode.ROOM_NOT_FOUND));
        
        // 방장 권한 확인
        if (!room.isOwner(userId)) {
            throw new CustomException(ErrorCode.NOT_ROOM_HOST);
        }
        
        // 이미 비밀번호가 있는 경우 에러
        if (room.getPassword() != null && !room.getPassword().isEmpty()) {
            throw new CustomException(ErrorCode.ROOM_PASSWORD_ALREADY_EXISTS);
        }
        
        // 새 비밀번호 설정
        room.updatePassword(newPassword);
        
        log.info("방 비밀번호 설정 완료 - RoomId: {}, UserId: {}", roomId, userId);
    }

    @Transactional
    public void terminateRoom(Long roomId, Long userId) {
        
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new CustomException(ErrorCode.ROOM_NOT_FOUND));

        if (!room.isOwner(userId)) {
            throw new CustomException(ErrorCode.NOT_ROOM_MANAGER);
        }

        // 썸네일 매핑 삭제
        roomThumbnailService.deleteThumbnailMapping(roomId);

        room.terminate();
        
        // Redis에서 모든 온라인 사용자 제거
        Set<Long> onlineUserIds = roomParticipantService.getParticipants(roomId);
        for (Long onlineUserId : onlineUserIds) {
            roomParticipantService.exitRoom(onlineUserId, roomId);
        }
        
        log.info("방 종료 완료 - RoomId: {}, UserId: {}, 퇴장 처리: {}명", 
                roomId, userId, onlineUserIds.size());
    }

    /**
     * 방 일시정지 (방장만 가능)
     */
    @Transactional
    public void pauseRoom(Long roomId, Long userId) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new CustomException(ErrorCode.ROOM_NOT_FOUND));

        if (!room.isOwner(userId)) {
            throw new CustomException(ErrorCode.NOT_ROOM_MANAGER);
        }

        room.pause();
        
        log.info("방 일시정지 완료 - RoomId: {}, UserId: {}", roomId, userId);
    }

    /**
     * 방 재개/활성화 (방장만 가능)
     */
    @Transactional
    public void activateRoom(Long roomId, Long userId) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new CustomException(ErrorCode.ROOM_NOT_FOUND));

        if (!room.isOwner(userId)) {
            throw new CustomException(ErrorCode.NOT_ROOM_MANAGER);
        }

        room.activate();
        
        log.info("방 활성화 완료 - RoomId: {}, UserId: {}", roomId, userId);
    }

    /**
     * 멤버 역할 변경
     * 1. 방장만 역할 변경 가능
     * 2. VISITOR → 모든 역할 승격 가능 (HOST 포함)
     * 3. HOST로 변경 시:
     *    - 대상자가 DB에 없으면 DB에 저장
     *    - 기존 방장은 자동으로 MEMBER로 강등
     *    - 본인은 방장으로 변경 불가
     * 4. 방장 자신의 역할은 변경 불가
     * @param roomId 방 ID
     * @param targetUserId 대상 사용자 ID
     * @param newRole 새 역할
     * @param requesterId 요청자 ID (방장)
     */
    @Transactional
    public void changeUserRole(Long roomId, Long targetUserId, RoomRole newRole, Long requesterId) {
        
        // 1. 요청자가 방장인지 확인
        RoomMember requester = roomMemberRepository.findByRoomIdAndUserId(roomId, requesterId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_ROOM_MEMBER));

        if (!requester.isHost()) {
            throw new CustomException(ErrorCode.NOT_ROOM_MANAGER);
        }

        // 2. 본인을 변경하려는 경우 (방장 → 다른 역할 불가)
        if (targetUserId.equals(requesterId)) {
            throw new CustomException(ErrorCode.CANNOT_CHANGE_OWN_ROLE);
        }

        // 3. 대상자 확인 (DB 조회 - VISITOR는 DB에 없을 수 있음)
        Optional<RoomMember> targetMemberOpt = roomMemberRepository.findByRoomIdAndUserId(roomId, targetUserId);

        // 변경 전 역할 저장 (알림용)
        RoomRole oldRole = targetMemberOpt.map(RoomMember::getRole).orElse(RoomRole.VISITOR);

        // 4. HOST로 변경하는 경우 - 기존 방장 강등
        if (newRole == RoomRole.HOST) {
            // 기존 방장을 MEMBER로 강등
            requester.updateRole(RoomRole.MEMBER);
            log.info("기존 방장 강등 - RoomId: {}, UserId: {}, MEMBER로 변경", roomId, requesterId);

            // 방장 위임 이벤트 발행
            Room room = roomRepository.findById(roomId)
                    .orElseThrow(() -> new CustomException(ErrorCode.ROOM_NOT_FOUND));

            eventPublisher.publishEvent(
                    new OwnerTransferredEvent(
                            requesterId,      // 이전 방장
                            roomId,
                            targetUserId,     // 새 방장
                            room.getTitle()
                    )
            );
        }

        // 5. 대상자 처리
        if (targetMemberOpt.isPresent()) {
            // 기존 멤버 - 역할만 업데이트
            RoomMember targetMember = targetMemberOpt.get();
            targetMember.updateRole(newRole);
            
            log.info("멤버 권한 변경 - RoomId: {}, TargetUserId: {}, NewRole: {}", 
                    roomId, targetUserId, newRole);
        } else {
            // VISITOR → 승격 시 DB에 저장
            Room room = roomRepository.findById(roomId)
                    .orElseThrow(() -> new CustomException(ErrorCode.ROOM_NOT_FOUND));
            
            User targetUser = userRepository.findById(targetUserId)
                    .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
            
            // DB에 저장 (처음으로!)
            RoomMember newMember = RoomMember.create(room, targetUser, newRole);
            roomMemberRepository.save(newMember);
            
            log.info("VISITOR 승격 (DB 저장) - RoomId: {}, UserId: {}, NewRole: {}", 
                    roomId, targetUserId, newRole);
        }

        // 권한 변경 이벤트 발행 (HOST 위임이 아닌 경우만)
        if (newRole != RoomRole.HOST) {
            eventPublisher.publishEvent(
                    new MemberRoleChangedEvent(
                            requesterId,
                            roomId,
                            targetUserId,
                            newRole.name()
                    )
            );
        }
        
        // 6. WebSocket으로 역할 변경 알림 브로드캐스트
        User targetUser = userRepository.findById(targetUserId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        
        com.back.domain.studyroom.dto.RoleChangedNotification notification = 
                com.back.domain.studyroom.dto.RoleChangedNotification.of(
                        roomId,
                        targetUserId,
                        targetUser.getNickname(),
                        targetUser.getProfileImageUrl(),
                        oldRole,
                        newRole
                );
        
        messagingTemplate.convertAndSend(
                "/topic/room/" + roomId + "/role-changed",
                notification
        );
        
        log.info("역할 변경 알림 전송 완료 - RoomId: {}, UserId: {}, {} → {}", 
                roomId, targetUserId, oldRole, newRole);
    }

    /**
     * 방 멤버 목록 조회 (Redis + DB 조합)
     * 1. Redis에서 온라인 사용자 ID 조회
     * 2. DB에서 해당 사용자들의 멤버십 조회 (IN 절)
     * 3. DB에 없는 사용자 = VISITOR
     * 4. User 정보와 조합하여 반환
     * 
     * @param roomId 방 ID
     * @param userId 요청자 ID (권한 체크용)
     * @return 온라인 멤버 목록 (VISITOR 포함)
     */
    public List<RoomMember> getRoomMembers(Long roomId, Long userId) {
        
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new CustomException(ErrorCode.ROOM_NOT_FOUND));

        // 1. Redis에서 온라인 사용자 ID 조회
        Set<Long> onlineUserIds = roomParticipantService.getParticipants(roomId);
        
        if (onlineUserIds.isEmpty()) {
            return List.of();
        }

        // 2. DB에서 멤버십 조회 (MEMBER 이상만 DB에 있음)
        List<RoomMember> dbMembers = roomMemberRepository.findByRoomIdAndUserIdIn(roomId, onlineUserIds);
        
        // 3. DB에 있는 userId Set 생성
        Set<Long> dbUserIds = dbMembers.stream()
                .map(m -> m.getUser().getId())
                .collect(java.util.stream.Collectors.toSet());
        
        // 4. DB에 없는 userId = VISITOR들
        Set<Long> visitorUserIds = onlineUserIds.stream()
                .filter(id -> !dbUserIds.contains(id))
                .collect(java.util.stream.Collectors.toSet());
        
        // 5. VISITOR User 정보 조회 (일괄 조회)
        if (!visitorUserIds.isEmpty()) {
            List<User> visitorUsers = userRepository.findAllById(visitorUserIds);
            
            // 6. VISITOR RoomMember 객체 생성 (메모리상)
            List<RoomMember> visitorMembers = visitorUsers.stream()
                    .map(user -> RoomMember.createVisitor(room, user))
                    .collect(java.util.stream.Collectors.toList());
            
            // 7. DB 멤버 + VISITOR 합치기
            List<RoomMember> allMembers = new java.util.ArrayList<>(dbMembers);
            allMembers.addAll(visitorMembers);
            
            return allMembers;
        }
        
        return dbMembers;
    }

    public RoomRole getUserRoomRole(Long roomId, Long userId) {
        return roomMemberRepository.findByRoomIdAndUserId(roomId, userId)
                .map(RoomMember::getRole)
                .orElse(RoomRole.VISITOR);
    }

    /**
     * 사용자 정보 조회 (역할 변경 응답용)
     */
    public User getUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
    }

    /**
     * 인기 방 목록 조회 (참가자 수 기준)
     */
    public Page<Room> getPopularRooms(Pageable pageable) {
        return roomRepository.findPopularRooms(pageable);
    }

    /**
     * 멤버 추방 (방장, 부방장만 가능)
     */
    @Transactional
    public void kickMember(Long roomId, Long targetUserId, Long requesterId) {
        
        RoomMember requester = roomMemberRepository.findByRoomIdAndUserId(roomId, requesterId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_ROOM_MEMBER));

        if (!requester.canKickMember()) {
            throw new CustomException(ErrorCode.NOT_ROOM_MANAGER);
        }

        RoomMember targetMember = roomMemberRepository.findByRoomIdAndUserId(roomId, targetUserId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_ROOM_MEMBER));

        if (targetMember.isHost()) {
            throw new CustomException(ErrorCode.CANNOT_KICK_HOST);
        }

        // Redis에서 제거 (강제 퇴장)
        roomParticipantService.exitRoom(targetUserId, roomId);

        // 멤버 추방 이벤트 발행
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new CustomException(ErrorCode.ROOM_NOT_FOUND));

        eventPublisher.publishEvent(
                new MemberKickedEvent(
                        requesterId,
                        roomId,
                        targetUserId,
                        room.getTitle()
                )
        );
        
        log.info("멤버 추방 완료 - RoomId: {}, TargetUserId: {}, RequesterId: {}", 
                roomId, targetUserId, requesterId);
    }

    // ==================== DTO 생성 헬퍼 메서드 ====================

    /**
     * RoomResponse 생성 (Redis에서 실시간 참가자 수 조회)
     */
    public com.back.domain.studyroom.dto.RoomResponse toRoomResponse(Room room) {
        long onlineCount = roomParticipantService.getParticipantCount(room.getId());
        return com.back.domain.studyroom.dto.RoomResponse.from(room, onlineCount);
    }

    /**
     * RoomResponse 리스트 생성 (일괄 조회로 N+1 방지)
     */
    public java.util.List<com.back.domain.studyroom.dto.RoomResponse> toRoomResponseList(java.util.List<Room> rooms) {
        java.util.List<Long> roomIds = rooms.stream()
                .map(Room::getId)
                .collect(java.util.stream.Collectors.toList());

        // Redis Pipeline으로 일괄 조회 (N+1 해결)
        java.util.Map<Long, Long> participantCounts = roomParticipantService.getParticipantCounts(roomIds);
        
        return rooms.stream()
                .map(room -> com.back.domain.studyroom.dto.RoomResponse.from(
                        room, 
                        participantCounts.getOrDefault(room.getId(), 0L)
                ))
                .collect(java.util.stream.Collectors.toList());
    }

    /**
     * RoomDetailResponse 생성 (Redis에서 실시간 참가자 수 조회)
     */
    public com.back.domain.studyroom.dto.RoomDetailResponse toRoomDetailResponse(
            Room room, 
            java.util.List<com.back.domain.studyroom.entity.RoomMember> members) {
        long onlineCount = roomParticipantService.getParticipantCount(room.getId());
        
        java.util.List<com.back.domain.studyroom.dto.RoomMemberResponse> memberResponses = members.stream()
                .map(com.back.domain.studyroom.dto.RoomMemberResponse::from)
                .collect(java.util.stream.Collectors.toList());
        
        return com.back.domain.studyroom.dto.RoomDetailResponse.of(room, onlineCount, memberResponses);
    }

    /**
     * MyRoomResponse 생성 (Redis에서 실시간 참가자 수 조회)
     */
    public com.back.domain.studyroom.dto.MyRoomResponse toMyRoomResponse(Room room, RoomRole myRole) {
        long onlineCount = roomParticipantService.getParticipantCount(room.getId());
        return com.back.domain.studyroom.dto.MyRoomResponse.of(room, onlineCount, myRole);
    }

    /**
     * MyRoomResponse 리스트 생성 (일괄 조회로 N+1 방지)
     */
    public java.util.List<com.back.domain.studyroom.dto.MyRoomResponse> toMyRoomResponseList(
            java.util.List<Room> rooms, 
            Long userId) {
        java.util.List<Long> roomIds = rooms.stream()
                .map(Room::getId)
                .collect(java.util.stream.Collectors.toList());

        // Redis Pipeline으로 일괄 조회 (N+1 해결)
        java.util.Map<Long, Long> participantCounts = roomParticipantService.getParticipantCounts(roomIds);
        
        return rooms.stream()
                .map(room -> {
                    RoomRole role = getUserRoomRole(room.getId(), userId);
                    long count = participantCounts.getOrDefault(room.getId(), 0L);
                    return com.back.domain.studyroom.dto.MyRoomResponse.of(room, count, role);
                })
                .collect(java.util.stream.Collectors.toList());
    }
    
    /**
     * RoomMemberResponse 리스트 생성 (아바타 정보 포함, N+1 방지)
     * @param roomId 방 ID
     * @param members 멤버 목록
     * @return 아바타 정보가 포함된 RoomMemberResponse 리스트
     */
    public java.util.List<com.back.domain.studyroom.dto.RoomMemberResponse> toRoomMemberResponseList(
            Long roomId,
            java.util.List<RoomMember> members) {
        
        if (members.isEmpty()) {
            return java.util.List.of();
        }
        
        // 1. 모든 사용자 ID 추출
        Set<Long> userIds = members.stream()
                .map(m -> m.getUser().getId())
                .collect(java.util.stream.Collectors.toSet());
        
        // 2. Redis에서 아바타 ID 일괄 조회
        java.util.Map<Long, Long> avatarMap = roomParticipantService.getUserAvatars(roomId, userIds);
        
        // 3. 아바타 ID Set 생성
        Set<Long> avatarIds = new java.util.HashSet<>(avatarMap.values());
        
        // 4. Avatar 엔티티 일괄 조회
        java.util.Map<Long, com.back.domain.studyroom.entity.Avatar> avatarEntityMap = 
                avatarService.getAvatarsByIds(avatarIds);
        
        // 5. RoomMemberResponse 생성
        return members.stream()
                .map(member -> {
                    Long userId = member.getUser().getId();
                    Long avatarId = avatarMap.get(userId);
                    
                    String avatarImageUrl = null;
                    if (avatarId != null) {
                        com.back.domain.studyroom.entity.Avatar avatar = avatarEntityMap.get(avatarId);
                        avatarImageUrl = avatar != null ? avatar.getImageUrl() : null;
                    }
                    
                    return com.back.domain.studyroom.dto.RoomMemberResponse.of(
                            member, avatarId, avatarImageUrl);
                })
                .collect(java.util.stream.Collectors.toList());
    }
}
