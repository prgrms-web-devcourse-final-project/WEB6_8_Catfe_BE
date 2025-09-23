package com.back.domain.studyroom.repository;

import com.back.domain.studyroom.entity.Room;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RoomRepository extends JpaRepository<Room, Long> {

    // 제목으로 방 검색 (부분 일치)
    List<Room> findByTitleContaining(String title);

    // 활성화된 방 목록 조회
    @Query("SELECT r FROM Room r WHERE r.isActive = true")
    List<Room> findActiveRooms();

    // 사용자가 생성한 방 목록 조회
    @Query("SELECT r FROM Room r WHERE r.createdBy.id = :createdById")
    List<Room> findByCreatedById(@Param("createdById") Long createdById);

    // 방 존재 여부 확인
    boolean existsById(Long roomId);
}