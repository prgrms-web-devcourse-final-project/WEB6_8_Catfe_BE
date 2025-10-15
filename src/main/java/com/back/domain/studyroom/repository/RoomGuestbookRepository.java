package com.back.domain.studyroom.repository;

import com.back.domain.studyroom.entity.RoomGuestbook;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface RoomGuestbookRepository extends JpaRepository<RoomGuestbook, Long> {

    /**
     * 특정 방의 방명록 목록 조회 (페이징)
     * User 정보를 fetch join으로 함께 조회하여 N+1 방지
     * 
     * @deprecated 핀 기능을 고려하지 않은 구버전. findByRoomIdWithUserOrderByPin 사용 권장
     */
    @Deprecated
    @Query("SELECT g FROM RoomGuestbook g " +
           "JOIN FETCH g.user " +
           "WHERE g.room.id = :roomId " +
           "ORDER BY g.createdAt DESC")
    Page<RoomGuestbook> findByRoomIdWithUser(@Param("roomId") Long roomId, Pageable pageable);

    /**
     * 특정 방의 방명록 목록 조회 (핀 우선 정렬, 페이징)
     * - 사용자가 핀한 방명록이 최상단에 표시됨
     * - 핀한 방명록 내에서는 최신순
     * - 핀하지 않은 방명록도 최신순
     */
    @Query("SELECT g FROM RoomGuestbook g " +
           "LEFT JOIN RoomGuestbookPin p ON p.guestbook.id = g.id AND p.user.id = :userId " +
           "JOIN FETCH g.user " +
           "WHERE g.room.id = :roomId " +
           "ORDER BY " +
           "CASE WHEN p.id IS NOT NULL THEN 0 ELSE 1 END, " +  // 핀한 것 우선
           "g.createdAt DESC")
    Page<RoomGuestbook> findByRoomIdWithUserOrderByPin(
            @Param("roomId") Long roomId, 
            @Param("userId") Long userId, 
            Pageable pageable);

    /**
     * 방명록 단건 조회 (User, Room 함께 조회)
     */
    @Query("SELECT g FROM RoomGuestbook g " +
           "JOIN FETCH g.user " +
           "JOIN FETCH g.room " +
           "WHERE g.id = :guestbookId")
    Optional<RoomGuestbook> findByIdWithUserAndRoom(@Param("guestbookId") Long guestbookId);

    /**
     * 특정 방의 방명록 개수 조회
     */
    long countByRoomId(Long roomId);

    /**
     * 사용자가 특정 방에 작성한 방명록 존재 여부
     */
    boolean existsByRoomIdAndUserId(Long roomId, Long userId);
}
