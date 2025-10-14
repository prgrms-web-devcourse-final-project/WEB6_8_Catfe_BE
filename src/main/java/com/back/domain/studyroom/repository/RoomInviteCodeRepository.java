package com.back.domain.studyroom.repository;

import com.back.domain.studyroom.entity.RoomInviteCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface RoomInviteCodeRepository extends JpaRepository<RoomInviteCode, Long> {

    /**
     * 초대 코드로 조회
     */
    @Query("SELECT ric FROM RoomInviteCode ric " +
           "JOIN FETCH ric.room " +
           "JOIN FETCH ric.createdBy " +
           "WHERE ric.inviteCode = :inviteCode")
    Optional<RoomInviteCode> findByInviteCode(@Param("inviteCode") String inviteCode);

    /**
     * 초대 코드 중복 확인
     */
    boolean existsByInviteCode(String inviteCode);

    /**
     * 사용자가 특정 방에서 생성한 활성 초대 코드 조회
     */
    @Query("SELECT ric FROM RoomInviteCode ric " +
           "JOIN FETCH ric.room " +
           "JOIN FETCH ric.createdBy " +
           "WHERE ric.room.id = :roomId " +
           "AND ric.createdBy.id = :userId " +
           "AND ric.isActive = true")
    Optional<RoomInviteCode> findByRoomIdAndCreatedByIdAndIsActiveTrue(
            @Param("roomId") Long roomId, 
            @Param("userId") Long userId);

    /**
     * 만료된 초대 코드 자동 비활성화 (배치용)
     */
    @Modifying
    @Query("UPDATE RoomInviteCode ric " +
           "SET ric.isActive = false " +
           "WHERE ric.expiresAt < :now " +
           "AND ric.isActive = true")
    int deactivateExpiredCodes(@Param("now") LocalDateTime now);
}
