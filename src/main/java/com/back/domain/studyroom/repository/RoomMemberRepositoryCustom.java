package com.back.domain.studyroom.repository;

import com.back.domain.studyroom.entity.RoomMember;
import com.back.domain.studyroom.entity.RoomRole;

import java.util.List;
import java.util.Optional;

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
     * 사용자가 참여 중인 모든 방의 멤버십 조회
     * DB에 저장된 멤버십만 조회 (MEMBER 이상)
     */
    List<RoomMember> findActiveByUserId(Long userId);

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
     * 여러 사용자의 멤버십 일괄 조회 (IN 절)
     * Redis에서 온라인 사용자 목록을 받아서 DB 멤버십 조회 시 사용
     * @param roomId 방 ID
     * @param userIds 사용자 ID 목록
     * @return 멤버십 목록 (MEMBER 이상만 DB에 있음)
     */
    List<RoomMember> findByRoomIdAndUserIdIn(Long roomId, java.util.Set<Long> userIds);
}
