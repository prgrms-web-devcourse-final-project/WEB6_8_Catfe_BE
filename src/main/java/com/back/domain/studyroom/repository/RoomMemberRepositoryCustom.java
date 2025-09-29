package com.back.domain.studyroom.repository;

import com.back.domain.studyroom.entity.RoomMember;
import com.back.domain.studyroom.entity.RoomRole;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 멤버십 정보 조회 (영구 데이터)
 역할 기반 쿼리
 실시간 온라인 상태 (WebSocketSessionManager 사용)
 온라인 상태는 Redis(WebSocketSessionManager)에서 관리
 */
public interface RoomMemberRepositoryCustom {

    /**
     * 방의 특정 사용자 멤버십 조회
     */
    Optional<RoomMember> findByRoomIdAndUserId(Long roomId, Long userId);

    /**
     * 방의 모든 멤버 조회 (역할순 정렬)
     */
    List<RoomMember> findByRoomIdOrderByRole(Long roomId);

    /**
     * 방의 멤버 중 특정 사용자 ID 목록에 해당하는 멤버만 조회
     WebSocket에서 온라인 사용자 ID 목록을 받아와서 해당 멤버들의 상세 정보 조회
     * @param roomId 방 ID
     * @param userIds 조회할 사용자 ID 목록
     * @return 해당하는 멤버 목록
     */
    List<RoomMember> findByRoomIdAndUserIdIn(Long roomId, Set<Long> userIds);

    /**
     * 특정 역할의 멤버 조회
     */
    List<RoomMember> findByRoomIdAndRole(Long roomId, RoomRole role);

    /**
     * 방장 조회
     */
    Optional<RoomMember> findHostByRoomId(Long roomId);

    /**
     * 관리자 권한을 가진 멤버들 조회 (HOST, SUB_HOST)
     */
    List<RoomMember> findManagersByRoomId(Long roomId);

    /**
     * 사용자가 특정 방에서 관리자 권한을 가지고 있는지 확인
     */
    boolean isManager(Long roomId, Long userId);

    /**
     * 사용자가 이미 해당 방의 멤버인지 확인
     */
    boolean existsByRoomIdAndUserId(Long roomId, Long userId);

    /**
     * 특정 역할의 멤버 수 조회
     */
    int countByRoomIdAndRole(Long roomId, RoomRole role);
}
