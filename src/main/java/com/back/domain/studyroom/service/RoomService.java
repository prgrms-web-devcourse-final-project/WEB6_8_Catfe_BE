package com.back.domain.studyroom.service;

import com.back.domain.studyroom.config.StudyRoomProperties;
import com.back.domain.studyroom.entity.*;
import com.back.domain.studyroom.repository.*;
import com.back.domain.user.entity.User;
import com.back.domain.user.repository.UserRepository;
import com.back.global.exception.CustomException;
import com.back.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

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
    private final com.back.global.websocket.service.WebSocketSessionManager sessionManager;

    /**
     * 방 생성 메서드
     * 생성 과정:
     * 1. 사용자 존재 확인
     * 2. Room 엔티티 생성 (외부 설정값 적용)
     * 3. 방장을 RoomMember로 등록
     * 4. 참가자 수 1로 설정

     * 기본 설정:
     - 상태: WAITING (대기 중)
     - 카메라/오디오/화면공유: application.yml의 설정값 사용
     - 참가자 수: 0명에서 시작 후 방장 추가로 1명
     */
    @Transactional
    public Room createRoom(String title, String description, boolean isPrivate, 
                          String password, int maxParticipants, Long creatorId) {
        
        User creator = userRepository.findById(creatorId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        Room room = Room.create(title, description, isPrivate, password, maxParticipants, creator, null);
        Room savedRoom = roomRepository.save(room);

        RoomMember hostMember = RoomMember.createHost(savedRoom, creator);
        roomMemberRepository.save(hostMember);

        // savedRoom.incrementParticipant();  // Redis로 이관 - DB 업데이트 제거
        
        log.info("방 생성 완료 - RoomId: {}, Title: {}, CreatorId: {}", 
                savedRoom.getId(), title, creatorId);
        
        return savedRoom;
    }

    /**
     * 방 입장 메서드
     * 
     * 입장 검증 과정:
     * 1. 방 존재 및 활성 상태 확인 (비관적 락으로 동시성 제어)
     * 2. 방 상태가 입장 가능한지 확인 (WAITING, ACTIVE)
     * 3. 정원 초과 여부 확인
     * 4. 비공개 방인 경우 비밀번호 확인
     * 5. 이미 참여 중인지 확인 (재입장 처리)

     * 멤버 등록: (현재는 visitor로 등록이지만 추후 역할 부여가 안된 인원을 visitor로 띄우는 식으로 저장 데이터 줄일 예정)
     * - 신규 사용자: VISITOR 역할로 등록
     * - 기존 사용자: 온라인 상태로 변경
     * 
     * 동시성 제어: 비관적 락(PESSIMISTIC_WRITE)으로 정원 초과 방지
     */
    @Transactional
    public RoomMember joinRoom(Long roomId, String password, Long userId) {
        
        // 비관적 락으로 방 조회 - 동시 입장 시 정원 초과 방지
        Room room = roomRepository.findByIdWithLock(roomId)
                .orElseThrow(() -> new CustomException(ErrorCode.ROOM_NOT_FOUND));

        if (!room.isActive()) {
            throw new CustomException(ErrorCode.ROOM_INACTIVE);
        }

        if (room.getStatus() == RoomStatus.TERMINATED) {
            throw new CustomException(ErrorCode.ROOM_TERMINATED);
        }

        if (!room.canJoin()) {
            if (room.isFull()) {
                throw new CustomException(ErrorCode.ROOM_FULL);
            }
            throw new CustomException(ErrorCode.ROOM_INACTIVE);
        }

        if (room.needsPassword() && !room.getPassword().equals(password)) {
            throw new CustomException(ErrorCode.ROOM_PASSWORD_INCORRECT);
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        Optional<RoomMember> existingMember = roomMemberRepository.findByRoomIdAndUserId(roomId, userId);
        if (existingMember.isPresent()) {
            RoomMember member = existingMember.get();
            // TODO: Redis에서 온라인 여부 확인하도록 변경
            // 현재는 기존 멤버 재입장 허용
            // room.incrementParticipant();  // Redis로 이관 - DB 업데이트 제거
            return member;
        }

        RoomMember newMember = RoomMember.createVisitor(room, user);
        RoomMember savedMember = roomMemberRepository.save(newMember);

        // room.incrementParticipant();  // Redis로 이관 - DB 업데이트 제거
        
        log.info("방 입장 완료 - RoomId: {}, UserId: {}, Role: {}", 
                roomId, userId, newMember.getRole());
        
        return savedMember;
    }

    /**
     * 방 나가기 메서드
     * 
     * 🚪 퇴장 처리:
     * - 일반 멤버: 단순 오프라인 처리 및 참가자 수 감소
     * - 방장: 특별 처리 로직 실행 (handleHostLeaving)
     * 
     * 🔄 방장 퇴장 시 처리:
     * - 다른 멤버가 없으면 → 방 자동 종료
     * - 다른 멤버가 있으면 → 새 방장 자동 위임
     */
    @Transactional
    public void leaveRoom(Long roomId, Long userId) {
        
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new CustomException(ErrorCode.ROOM_NOT_FOUND));

        RoomMember member = roomMemberRepository.findByRoomIdAndUserId(roomId, userId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_ROOM_MEMBER));

        // TODO: Redis에서 온라인 상태 확인하도록 변경

        if (member.isHost()) {
            handleHostLeaving(room, member);
        } else {
            // TODO: Redis에서 제거하도록 변경
            // room.decrementParticipant();  // Redis로 이관 - DB 업데이트 제거
        }

        log.info("방 퇴장 완료 - RoomId: {}, UserId: {}", roomId, userId);
    }

    private void handleHostLeaving(Room room, RoomMember hostMember) {
        // TODO: Redis에서 온라인 멤버 조회하도록 변경
        List<RoomMember> onlineMembers = roomMemberRepository.findOnlineMembersByRoomId(room.getId());
        
        List<RoomMember> otherOnlineMembers = onlineMembers.stream()
                .filter(m -> !m.getId().equals(hostMember.getId()))
                .toList();

        if (otherOnlineMembers.isEmpty()) {
            room.terminate();
            // TODO: Redis에서 제거하도록 변경
            // room.decrementParticipant();  // Redis로 이관 - DB 업데이트 제거
        } else {
            RoomMember newHost = otherOnlineMembers.stream()
                    .filter(m -> m.getRole() == RoomRole.SUB_HOST)
                    .findFirst()
                    .orElse(otherOnlineMembers.stream()
                            .min((m1, m2) -> m1.getJoinedAt().compareTo(m2.getJoinedAt()))
                            .orElse(null));

            if (newHost != null) {
                newHost.updateRole(RoomRole.HOST);
                // TODO: Redis에서 제거하도록 변경
                // room.decrementParticipant();  // Redis로 이관 - DB 업데이트 제거
                
                log.info("새 방장 지정 - RoomId: {}, NewHostId: {}", 
                        room.getId(), newHost.getUser().getId());
            }
        }
    }

    public Page<Room> getJoinableRooms(Pageable pageable) {
        return roomRepository.findJoinablePublicRooms(pageable);
    }

    public Room getRoomDetail(Long roomId, Long userId) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new CustomException(ErrorCode.ROOM_NOT_FOUND));

        if (room.isPrivate()) {
            boolean isMember = roomMemberRepository.existsByRoomIdAndUserId(roomId, userId);
            if (!isMember) {
                throw new CustomException(ErrorCode.ROOM_FORBIDDEN);
            }
        }

        return room;
    }

    public List<Room> getUserRooms(Long userId) {
        return roomRepository.findRoomsByUserId(userId);
    }

    @Transactional
    public void updateRoomSettings(Long roomId, String title, String description, 
                                  int maxParticipants, boolean allowCamera, 
                                  boolean allowAudio, boolean allowScreenShare, Long userId) {
        
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new CustomException(ErrorCode.ROOM_NOT_FOUND));

        if (!room.isOwner(userId)) {
            throw new CustomException(ErrorCode.NOT_ROOM_MANAGER);
        }

        if (maxParticipants < room.getCurrentParticipants()) {
            throw new CustomException(ErrorCode.BAD_REQUEST);
        }

        room.updateSettings(title, description, maxParticipants, 
                           allowCamera, allowAudio, allowScreenShare);
        
        log.info("방 설정 변경 완료 - RoomId: {}, UserId: {}", roomId, userId);
    }

    @Transactional
    public void terminateRoom(Long roomId, Long userId) {
        
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new CustomException(ErrorCode.ROOM_NOT_FOUND));

        if (!room.isOwner(userId)) {
            throw new CustomException(ErrorCode.NOT_ROOM_MANAGER);
        }

        room.terminate();
        // TODO: Redis에서 모든 멤버 제거하도록 변경
        // roomMemberRepository.disconnectAllMembers(roomId);
        
        log.info("방 종료 완료 - RoomId: {}, UserId: {}", roomId, userId);
    }

    @Transactional
    public void changeUserRole(Long roomId, Long targetUserId, RoomRole newRole, Long requesterId) {
        
        RoomMember requester = roomMemberRepository.findByRoomIdAndUserId(roomId, requesterId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_ROOM_MEMBER));

        if (!requester.canManageRoom()) {
            throw new CustomException(ErrorCode.NOT_ROOM_MANAGER);
        }

        RoomMember targetMember = roomMemberRepository.findByRoomIdAndUserId(roomId, targetUserId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_ROOM_MEMBER));

        if (targetMember.isHost()) {
            throw new CustomException(ErrorCode.CANNOT_CHANGE_HOST_ROLE);
        }

        targetMember.updateRole(newRole);
        
        log.info("멤버 권한 변경 완료 - RoomId: {}, TargetUserId: {}, NewRole: {}, RequesterId: {}", 
                roomId, targetUserId, newRole, requesterId);
    }

    public List<RoomMember> getRoomMembers(Long roomId, Long userId) {
        
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new CustomException(ErrorCode.ROOM_NOT_FOUND));

        if (room.isPrivate()) {
            boolean isMember = roomMemberRepository.existsByRoomIdAndUserId(roomId, userId);
            if (!isMember) {
                throw new CustomException(ErrorCode.ROOM_FORBIDDEN);
            }
        }

        return roomMemberRepository.findOnlineMembersByRoomId(roomId);
    }

    public RoomRole getUserRoomRole(Long roomId, Long userId) {
        return roomMemberRepository.findByRoomIdAndUserId(roomId, userId)
                .map(RoomMember::getRole)
                .orElse(RoomRole.VISITOR);
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

        // TODO: Redis에서 제거하도록 변경
        
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new CustomException(ErrorCode.ROOM_NOT_FOUND));
        // room.decrementParticipant();  // Redis로 이관 - DB 업데이트 제거
        
        log.info("멤버 추방 완료 - RoomId: {}, TargetUserId: {}, RequesterId: {}", 
                roomId, targetUserId, requesterId);
    }

    // ==================== DTO 생성 헬퍼 메서드 ====================

    /**
     * RoomResponse 생성 (Redis에서 실시간 참가자 수 조회)
     */
    public com.back.domain.studyroom.dto.RoomResponse toRoomResponse(Room room) {
        long onlineCount = sessionManager.getRoomOnlineUserCount(room.getId());
        return com.back.domain.studyroom.dto.RoomResponse.from(room, onlineCount);
    }

    /**
     * RoomResponse 리스트 생성 (일괄 조회로 N+1 방지)
     */
    public java.util.List<com.back.domain.studyroom.dto.RoomResponse> toRoomResponseList(java.util.List<Room> rooms) {
        java.util.List<Long> roomIds = rooms.stream()
                .map(Room::getId)
                .collect(java.util.stream.Collectors.toList());
        
        java.util.Map<Long, Long> participantCounts = sessionManager.getBulkRoomOnlineUserCounts(roomIds);
        
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
        long onlineCount = sessionManager.getRoomOnlineUserCount(room.getId());
        
        java.util.List<com.back.domain.studyroom.dto.RoomMemberResponse> memberResponses = members.stream()
                .map(com.back.domain.studyroom.dto.RoomMemberResponse::from)
                .collect(java.util.stream.Collectors.toList());
        
        return com.back.domain.studyroom.dto.RoomDetailResponse.of(room, onlineCount, memberResponses);
    }

    /**
     * MyRoomResponse 생성 (Redis에서 실시간 참가자 수 조회)
     */
    public com.back.domain.studyroom.dto.MyRoomResponse toMyRoomResponse(Room room, RoomRole myRole) {
        long onlineCount = sessionManager.getRoomOnlineUserCount(room.getId());
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
        
        java.util.Map<Long, Long> participantCounts = sessionManager.getBulkRoomOnlineUserCounts(roomIds);
        
        return rooms.stream()
                .map(room -> {
                    RoomRole role = getUserRoomRole(room.getId(), userId);
                    long count = participantCounts.getOrDefault(room.getId(), 0L);
                    return com.back.domain.studyroom.dto.MyRoomResponse.of(room, count, role);
                })
                .collect(java.util.stream.Collectors.toList());
    }
}
