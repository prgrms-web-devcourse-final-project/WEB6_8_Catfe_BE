package com.back.domain.studyroom.repository;

import com.back.domain.studyroom.entity.RoomChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface RoomChatMessageRepository extends JpaRepository<RoomChatMessage, Long>, RoomChatMessageRepositoryCustom {

    // 특정 방의 채팅 메시지 수 조회
    @Query("SELECT COUNT(m) FROM RoomChatMessage m WHERE m.room.id = :roomId")
    int countByRoomId(@Param("roomId") Long roomId);

    // 특정 방의 모든 채팅 메시지 삭제
    @Modifying
    @Query("DELETE FROM RoomChatMessage m WHERE m.room.id = :roomId")
    int deleteAllByRoomId(@Param("roomId") Long roomId);

}
