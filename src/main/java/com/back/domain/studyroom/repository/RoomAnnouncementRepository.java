package com.back.domain.studyroom.repository;

import com.back.domain.studyroom.entity.RoomAnnouncement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface RoomAnnouncementRepository extends JpaRepository<RoomAnnouncement, Long> {
    
    /**
     * 특정 방의 공지사항 조회 (작성자 정보 포함, 핀 고정 우선 + 최신순)
     */
    @Query("SELECT a FROM RoomAnnouncement a " +
           "JOIN FETCH a.createdBy " +
           "WHERE a.room.id = :roomId " +
           "ORDER BY a.isPinned DESC, a.createdAt DESC")
    List<RoomAnnouncement> findByRoomIdWithCreator(@Param("roomId") Long roomId);
    
    /**
     * 공지사항 단건 조회 (작성자 정보 포함)
     */
    @Query("SELECT a FROM RoomAnnouncement a " +
           "JOIN FETCH a.createdBy " +
           "WHERE a.id = :announcementId")
    Optional<RoomAnnouncement> findByIdWithCreator(@Param("announcementId") Long announcementId);
}
