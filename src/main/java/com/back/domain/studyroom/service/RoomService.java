package com.back.domain.studyroom.service;

import com.back.domain.studyroom.config.StudyRoomProperties;
import com.back.domain.studyroom.dto.RoomBroadcastMessage;
import com.back.domain.studyroom.dto.RoomMemberResponse;
import com.back.domain.studyroom.entity.*;
import com.back.domain.studyroom.repository.*;
import com.back.domain.user.entity.User;
import com.back.domain.user.repository.UserRepository;
import com.back.global.exception.CustomException;
import com.back.global.exception.ErrorCode;
import com.back.global.websocket.service.WebSocketBroadcastService;
import com.back.global.websocket.service.WebSocketSessionManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 스터디룸 서비스 - 방 생성, 입장, 퇴장 및 실시간 상태 관리
 * 
 * <h2>주요 기능:</h2>
 * <ul>
 *   <li>방 생성, 입장, 퇴장 로직 처리</li>
 *   <li>멤버 권한 관리 (승격, 강등, 추방)</li>
 *   <li>방 상태 관리 (활성화, 일시정지, 종료)</li>
 *   <li>방장 위임 로직 (방장이 나갈 때 자동 위임)</li>
 *   <li>🆕 WebSocket 기반 실시간 참가자 수 및 온라인 상태 동기화</li>
 * </ul>
 * 
 * <h2>권한 검증:</h2>
 * <ul>
 *   <li>모든 권한 검증을 서비스 레이어에서 처리</li>
 *   <li>비공개 방 접근 권한 체크</li>
 *   <li>방장/부방장 권한이 필요한 작업들의 권한 검증</li>
 * </ul>
 * 
 * <h2>🆕 WebSocket 연동 (PR #2):</h2>
 * <ul>
 *   <li><b>권장 메서드:</b> {@link #getOnlineMembersWithWebSocket(Long, Long)} - WebSocket + DB 통합 조회</li>
 *   <li><b>Deprecated:</b> {@link #getRoomMembers(Long, Long)} - DB만 조회 (하위 호환용으로만 유지)</li>
 * </ul>
 * 
 * <p><b>중요:</b> 온라인 멤버 목록 조회 시 반드시 {@code getOnlineMembersWithWebSocket()}를 사용하세요.
 * 이 메서드는 실시간 WebSocket 연결 상태와 DB 정보를 결합하여 정확한 온라인 상태를 제공합니다.</p>
 * 
 * <h2>설정값 관리:</h2>
 * <p>StudyRoomProperties를 통해 외부 설정 관리 (application.yml)</p>
 * 
 * @since 1.0
 * @see WebSocketSessionManager WebSocket 세션 관리
 * @see WebSocketBroadcastService 실시간 브로드캐스트
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
    private final WebSocketSessionManager sessionManager;
    private final WebSocketBroadcastService broadcastService;

    /**
     * 방 생성 메서드
     * 생성 과정:
     * 1. 사용자 존재 확인
     * 2. Room 엔티티 생성 (외부 설정값 적용)
     * 3. 방장을 RoomMember로 등록
     * 4. 참가자 수 1로 설정
     * <p>
     * 기본 설정:
     * - 상태: WAITING (대기 중)
     * - 카메라/오디오/화면공유: application.yml의 설정값 사용
     * - 참가자 수: 0명에서 시작 후 방장 추가로 1명
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

        savedRoom.incrementParticipant();

        log.info("방 생성 완료 - RoomId: {}, Title: {}, CreatorId: {}",
                savedRoom.getId(), title, creatorId);

        return savedRoom;
    }

    /**
     * 방 입장 메서드
     * <p>
     * 입장 검증 과정:
     * 1. 방 존재 및 활성 상태 확인 (비관적 락으로 동시성 제어)
     * 2. 방 상태가 입장 가능한지 확인 (WAITING, ACTIVE)
     * 3. 정원 초과 여부 확인
     * 4. 비공개 방인 경우 비밀번호 확인
     * 5. 이미 참여 중인지 확인 (재입장 처리)
     * <p>
     * 멤버 등록: (현재는 visitor로 등록이지만 추후 역할 부여가 안된 인원을 visitor로 띄우는 식으로 저장 데이터 줄일 예정)
     * - 신규 사용자: VISITOR 역할로 등록
     * - 기존 사용자: 온라인 상태로 변경
     * <p>
     * 동시성 제어: 비관적 락(PESSIMISTIC_WRITE)으로 정원 초과 방지
     * <p>
     * 🆕 WebSocket 연동: 입장 후 실시간 알림 및 세션 관리
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

        RoomMember member;
        boolean isReturningMember = false;

        // 기존 멤버십 확인
        Optional<RoomMember> existingMember = roomMemberRepository.findByRoomIdAndUserId(roomId, userId);
        
        if (existingMember.isPresent()) {
            // 이미 멤버인 경우 (재입장)
            member = existingMember.get();
            
            // Redis에서 이미 온라인인지 확인
            if (sessionManager.isUserConnected(userId)) {
                Long currentRoomId = sessionManager.getUserCurrentRoomId(userId);
                if (currentRoomId != null && currentRoomId.equals(roomId)) {
                    throw new CustomException(ErrorCode.ALREADY_JOINED_ROOM);
                }
            }
            
            // 마지막 활동 시간 업데이트
            member.updateLastActivity();
            room.incrementParticipant();
            isReturningMember = true;
            
        } else {
            // 신규 멤버 생성
            member = RoomMember.createVisitor(room, user);
            member = roomMemberRepository.save(member);
            room.incrementParticipant();
        }

        log.info("방 입장 완료 (DB 처리) - RoomId: {}, UserId: {}, Role: {}, 재입장: {}",
                roomId, userId, member.getRole(), isReturningMember);

        return member;
    }

    /**
     * 방 나가기 메서드
     * <p>
     * 🚪 퇴장 처리:
     * - 일반 멤버: 참가자 수 감소
     * - 방장: 특별 처리 로직 실행 (handleHostLeaving)
     * <p>
     * 🔄 방장 퇴장 시 처리:
     * - 다른 멤버가 없으면 → 방 자동 종료
     * - 다른 멤버가 있으면 → 새 방장 자동 위임
     * <p>
     * 📝 참고: 실제 온라인 상태는 Redis에서 관리
     */
    @Transactional
    public void leaveRoom(Long roomId, Long userId) {

        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new CustomException(ErrorCode.ROOM_NOT_FOUND));

        RoomMember member = roomMemberRepository.findByRoomIdAndUserId(roomId, userId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_ROOM_MEMBER));

        // Redis에서 온라인 상태 확인
        boolean isCurrentlyOnline = sessionManager.isUserConnected(userId);
        Long currentRoomId = sessionManager.getUserCurrentRoomId(userId);
        
        // 이 방에 있지 않으면 퇴장 처리 불필요
        if (!isCurrentlyOnline || currentRoomId == null || !currentRoomId.equals(roomId)) {
            log.debug("이미 오프라인 상태이거나 다른 방에 있음 - UserId: {}, CurrentRoomId: {}", userId, currentRoomId);
            return;
        }

        // 퇴장 전 멤버 정보 백업 (브로드캐스트용)
        String memberName = member.getUser().getNickname();
        boolean wasHost = member.isHost();

        if (member.isHost()) {
            handleHostLeaving(room, member);
        } else {
            // 일반 멤버 퇴장 처리
            room.decrementParticipant();
            member.updateLastActivity();
        }

        log.info("방 퇴장 완료 (DB 처리) - RoomId: {}, UserId: {}, 방장여부: {}", roomId, userId, wasHost);
    }

    private void handleHostLeaving(Room room, RoomMember hostMember) {
        // Redis에서 실제 온라인 사용자 조회
        Set<Long> onlineUserIds = sessionManager.getOnlineUsersInRoom(room.getId());
        
        // 온라인 사용자 중 방장 제외
        Set<Long> otherOnlineUserIds = onlineUserIds.stream()
                .filter(id -> !id.equals(hostMember.getUser().getId()))
                .collect(Collectors.toSet());

        if (otherOnlineUserIds.isEmpty()) {
            // 다른 온라인 멤버가 없으면 방 종료
            room.terminate();
            room.decrementParticipant();

            log.info("방 자동 종료 (온라인 멤버 없음) - RoomId: {}", room.getId());

            // 방 종료 알림 브로드캐스트
            try {
                broadcastService.broadcastToRoom(room.getId(), RoomBroadcastMessage.roomTerminated(room.getId()));
            } catch (Exception e) {
                log.warn("방 종료 브로드캐스트 실패 - 방: {}", room.getId(), e);
            }
            
        } else {
            // 다른 온라인 멤버가 있으면 새 방장 선정
            List<RoomMember> otherOnlineMembers = roomMemberRepository
                    .findByRoomIdAndUserIdIn(room.getId(), otherOnlineUserIds);

            // 우선순위: 부방장 > 가장 먼저 가입한 멤버
            RoomMember newHost = otherOnlineMembers.stream()
                    .filter(m -> m.getRole() == RoomRole.SUB_HOST)
                    .findFirst()
                    .orElse(otherOnlineMembers.stream()
                            .min((m1, m2) -> m1.getJoinedAt().compareTo(m2.getJoinedAt()))
                            .orElse(null));

            if (newHost != null) {
                newHost.updateRole(RoomRole.HOST);
                room.decrementParticipant();

                log.info("새 방장 지정 - RoomId: {}, NewHostId: {}",
                        room.getId(), newHost.getUser().getId());

                // 새 방장 지정 알림 브로드캐스트
                try {
                    broadcastService.broadcastToRoom(room.getId(), 
                            RoomBroadcastMessage.hostChanged(room.getId(), newHost));
                } catch (Exception e) {
                    log.warn("새 방장 지정 브로드캐스트 실패 - 방: {}", room.getId(), e);
                }
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

        // 🆕 방 설정 변경 알림 브로드캐스트
        try {
            String updateMessage = String.format("방 설정이 변경되었습니다. (제목: %s, 최대인원: %d명)",
                    title, maxParticipants);
            broadcastService.broadcastRoomUpdate(roomId, updateMessage);
        } catch (Exception e) {
            log.warn("방 설정 변경 브로드캐스트 실패 - 방: {}", roomId, e);
        }

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

        // 방 종료 알림 브로드캐스트 (WebSocket 세션 정리 전에 알림 전송)
        try {
            broadcastService.broadcastToRoom(roomId, RoomBroadcastMessage.roomTerminated(roomId));
        } catch (Exception e) {
            log.warn("방 종료 브로드캐스트 실패 - 방: {}", roomId, e);
        }

        // Redis에서 모든 세션 정리 (자동으로 방에서 퇴장 처리됨)
        Set<Long> onlineUserIds = sessionManager.getOnlineUsersInRoom(roomId);
        for (Long onlineUserId : onlineUserIds) {
            try {
                sessionManager.leaveRoom(onlineUserId, roomId);
            } catch (Exception e) {
                log.warn("방 종료 시 세션 정리 실패 - 방: {}, 사용자: {}", roomId, onlineUserId, e);
            }
        }

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

        // 🆕 멤버 역할 변경 알림 브로드캐스트
        try {
            broadcastService.broadcastToRoom(roomId, RoomBroadcastMessage.memberRoleChanged(roomId, targetMember));
        } catch (Exception e) {
            log.warn("멤버 역할 변경 브로드캐스트 실패 - 방: {}", roomId, e);
        }

        log.info("멤버 권한 변경 완료 - RoomId: {}, TargetUserId: {}, NewRole: {}, RequesterId: {}",
                roomId, targetUserId, newRole, requesterId);
    }

    /**
     * WebSocket 기반 온라인 멤버 목록 조회
     * 
     * <h2>동작 방식:</h2>
     * <ol>
     *   <li>Redis에서 온라인 사용자 ID 목록 조회 (실시간 상태)</li>
     *   <li>DB에서 해당 ID들의 멤버 상세 정보 조회</li>
     *   <li>두 정보를 결합하여 RoomMemberResponse DTO 반환</li>
     * </ol>
     * 
     * <h2>Redis가 Single Source of Truth:</h2>
     * <p>온라인 상태는 Redis만 신뢰하며, DB는 멤버십 정보만 제공</p>
     * 
     * @param roomId 조회할 방의 ID
     * @param userId 요청한 사용자의 ID (권한 체크용)
     * @return 실시간 온라인 멤버 목록
     * @throws CustomException ROOM_NOT_FOUND - 방이 존재하지 않음
     * @throws CustomException ROOM_FORBIDDEN - 비공개 방에 대한 접근 권한 없음
     */
    public List<RoomMemberResponse> getOnlineMembersWithWebSocket(Long roomId, Long userId) {

        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new CustomException(ErrorCode.ROOM_NOT_FOUND));

        if (room.isPrivate()) {
            boolean isMember = roomMemberRepository.existsByRoomIdAndUserId(roomId, userId);
            if (!isMember) {
                throw new CustomException(ErrorCode.ROOM_FORBIDDEN);
            }
        }

        try {
            // 1단계: Redis에서 온라인 사용자 ID 목록 조회 (실시간)
            Set<Long> onlineUserIds = sessionManager.getOnlineUsersInRoom(roomId);

            if (onlineUserIds.isEmpty()) {
                log.debug("온라인 멤버 없음 - 방: {}", roomId);
                return List.of();
            }

            // 2단계: DB에서 해당 사용자들의 멤버 상세 정보 조회
            List<RoomMember> onlineMembers = roomMemberRepository
                    .findByRoomIdAndUserIdIn(roomId, onlineUserIds);

            // 3단계: DTO 변환
            List<RoomMemberResponse> response = onlineMembers.stream()
                    .map(RoomMemberResponse::from)
                    .collect(Collectors.toList());

            log.debug("온라인 멤버 조회 성공 - 방: {}, Redis: {}명, DB 매칭: {}명",
                    roomId, onlineUserIds.size(), onlineMembers.size());

            return response;

        } catch (CustomException e) {
            // CustomException은 다시 던져서 상위에서 처리
            throw e;
            
        } catch (Exception e) {
            log.error("온라인 멤버 조회 실패 - 방: {}, 오류: {}", roomId, e.getMessage(), e);
            // 실패 시 빈 목록 반환 (서비스 중단 방지)
            return List.of();
        }
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

        // 추방 전 멤버 정보 백업 (브로드캐스트용)
        String memberName = targetMember.getUser().getNickname();

        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new CustomException(ErrorCode.ROOM_NOT_FOUND));
        
        // 참가자 수 감소
        room.decrementParticipant();

        // WebSocket 세션 정리 (강제 퇴장)
        try {
            sessionManager.leaveRoom(targetUserId, roomId);
            
            // 멤버 추방 알림 브로드캐스트
            broadcastService.broadcastToRoom(roomId, RoomBroadcastMessage.memberKicked(roomId, memberName));

        } catch (Exception e) {
            log.warn("추방 처리 중 WebSocket 연동 실패 - 방: {}, 대상: {}", roomId, targetUserId, e);
        }

        log.info("멤버 추방 완료 - RoomId: {}, TargetUserId: {}, RequesterId: {}",
                roomId, targetUserId, requesterId);
    }

    // ======================== 삭제 예정: WebSocket 헬퍼 메서드 ========================
    // 이 메서드들은 Phase 2에서 이벤트 기반으로 재구성될 예정입니다.
    // 현재는 RoomService에서 직접 sessionManager와 broadcastService를 호출합니다.
}
