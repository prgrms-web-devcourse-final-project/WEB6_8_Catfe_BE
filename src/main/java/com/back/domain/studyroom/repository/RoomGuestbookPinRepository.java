package com.back.domain.studyroom.repository;

import com.back.domain.studyroom.entity.RoomGuestbookPin;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface RoomGuestbookPinRepository extends JpaRepository<RoomGuestbookPin, Long> {

    /**
     * 특정 사용자가 특정 방명록을 핀했는지 조회
     */
    Optional<RoomGuestbookPin> findByGuestbookIdAndUserId(Long guestbookId, Long userId);

    /**
     * 특정 사용자가 핀한 방명록 ID 목록 조회
     */
    @Query("SELECT p.guestbook.id FROM RoomGuestbookPin p WHERE p.user.id = :userId")
    Set<Long> findPinnedGuestbookIdsByUserId(@Param("userId") Long userId);

    /**
     * 특정 사용자가 특정 방의 방명록들 중 핀한 것들의 ID 조회
     */
    @Query("SELECT p.guestbook.id FROM RoomGuestbookPin p " +
           "WHERE p.user.id = :userId " +
           "AND p.guestbook.room.id = :roomId")
    Set<Long> findPinnedGuestbookIdsByUserIdAndRoomId(
            @Param("userId") Long userId, 
            @Param("roomId") Long roomId);

    /**
     * 특정 사용자가 핀한 방명록 개수
     */
    long countByUserId(Long userId);

    /**
     * 특정 방명록을 핀한 사용자 수
     */
    long countByGuestbookId(Long guestbookId);

    /**
     * 특정 사용자가 핀한 모든 핀 조회
     */
    List<RoomGuestbookPin> findByUserId(Long userId);
}
