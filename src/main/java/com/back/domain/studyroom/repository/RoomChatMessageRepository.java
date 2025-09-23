package com.back.domain.studyroom.repository;

import com.back.domain.studyroom.entity.RoomChatMessage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface RoomChatMessageRepository extends JpaRepository<RoomChatMessage, Long> {

    // 방별 페이징된 채팅 메시지 조회 (무한 스크롤용)
    @Query("SELECT m FROM RoomChatMessage m " +
            "WHERE m.room.id = :roomId " +
            "ORDER BY m.createdAt DESC")
    Page<RoomChatMessage> findByRoomIdOrderByCreatedAtDesc(@Param("roomId") Long roomId, Pageable pageable);

    // 특정 타임스탬프 이후의 메시지 조회 (실시간 업데이트용)
    @Query("SELECT m FROM RoomChatMessage m " +
            "WHERE m.room.id = :roomId " +
            "AND m.createdAt > :timestamp " +
            "ORDER BY m.createdAt ASC")
    List<RoomChatMessage> findByRoomIdAfterTimestamp(@Param("roomId") Long roomId,
                                                     @Param("timestamp") LocalDateTime timestamp);

    // 방별 최근 20개 메시지 조회
    List<RoomChatMessage> findTop20ByRoomIdOrderByCreatedAtDesc(Long roomId);

    // 방별 전체 메시지 수 조회
    long countByRoomId(Long roomId);
}