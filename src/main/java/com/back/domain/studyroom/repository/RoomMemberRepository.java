package com.back.domain.studyroom.repository;

import com.back.domain.studyroom.entity.RoomMember;
import com.back.domain.studyroom.entity.RoomRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface RoomMemberRepository extends JpaRepository<RoomMember, Long> {

    // 방의 특정 사용자 멤버십 조회
    @Query("SELECT rm FROM RoomMember rm WHERE rm.room.id = :roomId AND rm.user.id = :userId")
    Optional<RoomMember> findByRoomIdAndUserId(@Param("roomId") Long roomId, @Param("userId") Long userId);

    // 방의 모든 멤버 조회
    @Query("SELECT rm FROM RoomMember rm WHERE rm.room.id = :roomId ORDER BY rm.role, rm.joinedAt")
    List<RoomMember> findByRoomIdOrderByRole(@Param("roomId") Long roomId);

    // 방의 온라인 멤버 조회
    // JOIN FETCH로 N+1 문제 해결 (user를 미리 로딩)
    @Query("SELECT rm FROM RoomMember rm " +
           "JOIN FETCH rm.user " +
           "WHERE rm.room.id = :roomId AND rm.isOnline = true " +
           "ORDER BY rm.role, rm.lastActiveAt DESC")
    List<RoomMember> findOnlineMembersByRoomId(@Param("roomId") Long roomId);

    // 방의 활성 멤버 수 조회
    @Query("SELECT COUNT(rm) FROM RoomMember rm WHERE rm.room.id = :roomId AND rm.isOnline = true")
    int countActiveMembersByRoomId(@Param("roomId") Long roomId);

    // 사용자가 참여 중인 모든 방의 멤버십 조회
    @Query("SELECT rm FROM RoomMember rm WHERE rm.user.id = :userId AND rm.isOnline = true")
    List<RoomMember> findActiveByUserId(@Param("userId") Long userId);

    // 특정 역할의 멤버 조회
    @Query("SELECT rm FROM RoomMember rm WHERE rm.room.id = :roomId AND rm.role = :role")
    List<RoomMember> findByRoomIdAndRole(@Param("roomId") Long roomId, @Param("role") RoomRole role);

    // 방장 조회
    @Query("SELECT rm FROM RoomMember rm WHERE rm.room.id = :roomId AND rm.role = 'HOST'")
    Optional<RoomMember> findHostByRoomId(@Param("roomId") Long roomId);

    // 관리자 권한을 가진 멤버들 조회 (HOST, SUB_HOST)
    @Query("SELECT rm FROM RoomMember rm WHERE rm.room.id = :roomId " +
           "AND rm.role IN ('HOST', 'SUB_HOST') ORDER BY rm.role")
    List<RoomMember> findManagersByRoomId(@Param("roomId") Long roomId);

    // 사용자가 특정 방에서 관리자 권한을 가지고 있는지 확인
    @Query("SELECT CASE WHEN COUNT(rm) > 0 THEN true ELSE false END FROM RoomMember rm " +
           "WHERE rm.room.id = :roomId AND rm.user.id = :userId " +
           "AND rm.role IN ('HOST', 'SUB_HOST')")
    boolean isManager(@Param("roomId") Long roomId, @Param("userId") Long userId);

    // 사용자가 이미 해당 방의 멤버인지 확인
    @Query("SELECT CASE WHEN COUNT(rm) > 0 THEN true ELSE false END FROM RoomMember rm " +
           "WHERE rm.room.id = :roomId AND rm.user.id = :userId")
    boolean existsByRoomIdAndUserId(@Param("roomId") Long roomId, @Param("userId") Long userId);

    // WebSocket 연결 ID로 멤버 조회
    Optional<RoomMember> findByConnectionId(String connectionId);

    // 방 퇴장 처리
    @Modifying
    @Query("UPDATE RoomMember rm SET rm.isOnline = false, rm.connectionId = null " +
           "WHERE rm.room.id = :roomId AND rm.user.id = :userId")
    void leaveRoom(@Param("roomId") Long roomId, @Param("userId") Long userId);

    // 방의 모든 멤버를 오프라인 처리 (방 종료 시)
    @Modifying
    @Query("UPDATE RoomMember rm SET rm.isOnline = false, rm.connectionId = null " +
           "WHERE rm.room.id = :roomId")
    void disconnectAllMembers(@Param("roomId") Long roomId);

    // 특정 역할의 멤버 수 조회
    @Query("SELECT COUNT(rm) FROM RoomMember rm WHERE rm.room.id = :roomId " +
           "AND rm.role = :role AND rm.isOnline = true")
    int countByRoomIdAndRole(@Param("roomId") Long roomId, @Param("role") RoomRole role);
}
