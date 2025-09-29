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
    private final WebSocketSessionManager sessionManager;

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

        savedRoom.incrementParticipant();
        
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
     * 
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

        Optional<RoomMember> existingMember = roomMemberRepository.findByRoomIdAndUserId(roomId, userId);
        if (existingMember.isPresent()) {
            member = existingMember.get();
            if (member.isOnline()) {
                throw new CustomException(ErrorCode.ALREADY_JOINED_ROOM);
            }
            member.updateOnlineStatus(true);
            room.incrementParticipant();
            isReturningMember = true;
        } else {
            member = RoomMember.createVisitor(room, user);
            member = roomMemberRepository.save(member);
            room.incrementParticipant();
        }

        // 🆕 WebSocket 세션 연동
        try {
            syncWebSocketSession(userId, roomId, member);
            
            // 🆕 실시간 입장 알림 브로드캐스트
            broadcastMemberJoined(roomId, member, isReturningMember);
            
        } catch (Exception e) {
            log.warn("WebSocket 연동 실패하지만 입장은 계속 진행 - 사용자: {}, 방: {}, 오류: {}", 
                    userId, roomId, e.getMessage());
        }
        
        log.info("방 입장 완료 - RoomId: {}, UserId: {}, Role: {}, 재입장: {}", 
                roomId, userId, member.getRole(), isReturningMember);
        
        return member;
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
     * 
     * 🆕 WebSocket 연동: 퇴장 후 실시간 알림 및 세션 정리
     */
    @Transactional
    public void leaveRoom(Long roomId, Long userId) {
        
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new CustomException(ErrorCode.ROOM_NOT_FOUND));

        RoomMember member = roomMemberRepository.findByRoomIdAndUserId(roomId, userId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_ROOM_MEMBER));

        if (!member.isOnline()) {
            return;
        }

        // 퇴장 전 멤버 정보 백업 (브로드캐스트용)
        String memberName = member.getUser().getNickname();
        boolean wasHost = member.isHost();

        if (member.isHost()) {
            handleHostLeaving(room, member);
        } else {
            member.leave();
            room.decrementParticipant();
        }

        // 🆕 WebSocket 세션 정리
        try {
            cleanupWebSocketSession(userId, roomId);
            
            // 🆕 실시간 퇴장 알림 브로드캐스트 (방이 종료되지 않은 경우에만)
            if (room.getStatus() != RoomStatus.TERMINATED) {
                broadcastMemberLeft(roomId, memberName, wasHost);
            }
            
        } catch (Exception e) {
            log.warn("WebSocket 세션 정리 실패하지만 퇴장은 계속 진행 - 사용자: {}, 방: {}, 오류: {}", 
                    userId, roomId, e.getMessage());
        }

        log.info("방 퇴장 완료 - RoomId: {}, UserId: {}, 방장여부: {}", roomId, userId, wasHost);
    }

    private void handleHostLeaving(Room room, RoomMember hostMember) {
        List<RoomMember> onlineMembers = roomMemberRepository.findOnlineMembersByRoomId(room.getId());
        
        List<RoomMember> otherOnlineMembers = onlineMembers.stream()
                .filter(m -> !m.getId().equals(hostMember.getId()))
                .toList();

        if (otherOnlineMembers.isEmpty()) {
            room.terminate();
            hostMember.leave();
            room.decrementParticipant();
            
            // 🆕 방 종료 알림 브로드캐스트
            try {
                sessionManager.broadcastToRoom(room.getId(), RoomBroadcastMessage.roomTerminated(room.getId()));
            } catch (Exception e) {
                log.warn("방 종료 브로드캐스트 실패 - 방: {}", room.getId(), e);
            }
        } else {
            RoomMember newHost = otherOnlineMembers.stream()
                    .filter(m -> m.getRole() == RoomRole.SUB_HOST)
                    .findFirst()
                    .orElse(otherOnlineMembers.stream()
                            .min((m1, m2) -> m1.getJoinedAt().compareTo(m2.getJoinedAt()))
                            .orElse(null));

            if (newHost != null) {
                newHost.updateRole(RoomRole.HOST);
                hostMember.leave();
                room.decrementParticipant();
                
                log.info("새 방장 지정 - RoomId: {}, NewHostId: {}", 
                        room.getId(), newHost.getUser().getId());
                
                // 🆕 새 방장 지정 알림 브로드캐스트
                try {
                    sessionManager.broadcastToRoom(room.getId(), RoomBroadcastMessage.hostChanged(room.getId(), newHost));
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
            sessionManager.broadcastRoomUpdate(roomId, updateMessage);
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
        roomMemberRepository.disconnectAllMembers(roomId);
        
        // 🆕 방 종료 알림 브로드캐스트
        try {
            sessionManager.broadcastToRoom(roomId, RoomBroadcastMessage.roomTerminated(roomId));
        } catch (Exception e) {
            log.warn("방 종료 브로드캐스트 실패 - 방: {}", roomId, e);
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
            sessionManager.broadcastToRoom(roomId, RoomBroadcastMessage.memberRoleChanged(roomId, targetMember));
        } catch (Exception e) {
            log.warn("멤버 역할 변경 브로드캐스트 실패 - 방: {}", roomId, e);
        }
        
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

    /**
     * 🆕 WebSocket 기반 온라인 멤버 목록 조회
     * DB의 멤버 목록과 WebSocket 세션 상태를 결합하여 정확한 온라인 상태 제공
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
            // DB에서 모든 멤버 조회
            List<RoomMember> allMembers = roomMemberRepository.findOnlineMembersByRoomId(roomId);
            
            // WebSocket에서 실제 온라인 상태 조회
            Set<Long> webSocketOnlineUsers = sessionManager.getOnlineUsersInRoom(roomId);
            
            // 두 정보를 결합하여 정확한 온라인 상태 반영
            return allMembers.stream()
                    .filter(member -> webSocketOnlineUsers.contains(member.getUser().getId()))
                    .map(RoomMemberResponse::from)
                    .collect(Collectors.toList());
                    
        } catch (Exception e) {
            log.warn("WebSocket 기반 멤버 목록 조회 실패, DB 정보만 사용 - 방: {}", roomId, e);
            // WebSocket 연동 실패 시 기존 방식으로 폴백
            return roomMemberRepository.findOnlineMembersByRoomId(roomId).stream()
                    .map(RoomMemberResponse::from)
                    .collect(Collectors.toList());
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

        targetMember.leave();
        
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new CustomException(ErrorCode.ROOM_NOT_FOUND));
        room.decrementParticipant();
        
        // 🆕 WebSocket 세션 정리
        try {
            cleanupWebSocketSession(targetUserId, roomId);
            
            // 🆕 멤버 추방 알림 브로드캐스트
            sessionManager.broadcastToRoom(roomId, RoomBroadcastMessage.memberKicked(roomId, memberName));
            
        } catch (Exception e) {
            log.warn("추방 처리 중 WebSocket 연동 실패 - 방: {}, 대상: {}", roomId, targetUserId, e);
        }
        
        log.info("멤버 추방 완료 - RoomId: {}, TargetUserId: {}, RequesterId: {}", 
                roomId, targetUserId, requesterId);
    }

    // ======================== WebSocket 연동 헬퍼 메서드 ========================

    /**
     * WebSocket 세션과 RoomMember 상태 동기화
     */
    private void syncWebSocketSession(Long userId, Long roomId, RoomMember member) {
        try {
            // WebSocket 세션 매니저에 방 입장 등록
            sessionManager.joinRoom(userId, roomId);
            
            // RoomMember의 연결 상태 업데이트
            member.heartbeat(); // 마지막 활동 시간 갱신
            
            log.debug("WebSocket 세션 동기화 완료 - 사용자: {}, 방: {}", userId, roomId);
            
        } catch (Exception e) {
            log.error("WebSocket 세션 동기화 실패 - 사용자: {}, 방: {}", userId, roomId, e);
            throw new CustomException(ErrorCode.WS_ROOM_JOIN_FAILED);
        }
    }

    /**
     * WebSocket 세션 정리
     */
    private void cleanupWebSocketSession(Long userId, Long roomId) {
        try {
            // WebSocket 세션 매니저에서 방 퇴장 처리
            sessionManager.leaveRoom(userId, roomId);
            
            log.debug("WebSocket 세션 정리 완료 - 사용자: {}, 방: {}", userId, roomId);
            
        } catch (Exception e) {
            log.error("WebSocket 세션 정리 실패 - 사용자: {}, 방: {}", userId, roomId, e);
            throw new CustomException(ErrorCode.WS_ROOM_LEAVE_FAILED);
        }
    }

    /**
     * 멤버 입장 실시간 브로드캐스트
     */
    private void broadcastMemberJoined(Long roomId, RoomMember member, boolean isReturning) {
        try {
            RoomBroadcastMessage message = RoomBroadcastMessage.memberJoined(roomId, member);
            sessionManager.broadcastToRoom(roomId, message);
            
            // 온라인 멤버 목록도 함께 업데이트
            sessionManager.broadcastOnlineMembersUpdate(roomId);
            
            log.debug("멤버 입장 브로드캐스트 완료 - 방: {}, 사용자: {}, 재입장: {}", 
                    roomId, member.getUser().getId(), isReturning);
                    
        } catch (Exception e) {
            log.error("멤버 입장 브로드캐스트 실패 - 방: {}, 사용자: {}", roomId, member.getUser().getId(), e);
        }
    }

    /**
     * 멤버 퇴장 실시간 브로드캐스트
     */
    private void broadcastMemberLeft(Long roomId, String memberName, boolean wasHost) {
        try {
            // 퇴장 알림 생성 (방장인 경우 특별 메시지)
            String message = wasHost ? 
                String.format("방장 %s님이 방을 나갔습니다.", memberName) :
                String.format("%s님이 방을 나갔습니다.", memberName);
                
            RoomBroadcastMessage broadcastMessage = RoomBroadcastMessage.roomUpdated(roomId, message);
            sessionManager.broadcastToRoom(roomId, broadcastMessage);
            
            // 온라인 멤버 목록도 함께 업데이트
            sessionManager.broadcastOnlineMembersUpdate(roomId);
            
            log.debug("멤버 퇴장 브로드캐스트 완료 - 방: {}, 멤버: {}, 방장여부: {}", 
                    roomId, memberName, wasHost);
                    
        } catch (Exception e) {
            log.error("멤버 퇴장 브로드캐스트 실패 - 방: {}, 멤버: {}", roomId, memberName, e);
        }
    }
}
