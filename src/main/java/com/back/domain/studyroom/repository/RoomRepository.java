package com.back.domain.studyroom.repository;

import com.back.domain.studyroom.entity.Room;
import com.back.domain.studyroom.entity.RoomStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RoomRepository extends JpaRepository<Room, Long>, RoomRepositoryCustom {

    // 제목으로 방 검색 (단순 쿼리)
    List<Room> findByTitleContaining(String title);

    // 활성화된 방 목록 조회
    @Query("SELECT r FROM Room r WHERE r.isActive = true")
    List<Room> findActiveRooms();

    // 특정 상태의 방 목록 조회
    @Query("SELECT r FROM Room r WHERE r.status = :status")
    List<Room> findByStatus(@Param("status") RoomStatus status);

    // 사용자가 생성한 방 목록 조회
    @Query("SELECT r FROM Room r WHERE r.createdBy.id = :createdById ORDER BY r.createdAt DESC")
    List<Room> findByCreatedById(@Param("createdById") Long createdById);

    // 방 존재 및 활성 상태 확인
    @Query("SELECT CASE WHEN COUNT(r) > 0 THEN true ELSE false END FROM Room r " +
           "WHERE r.id = :roomId AND r.isActive = true")
    boolean existsByIdAndActive(@Param("roomId") Long roomId);

    // 비밀번호 검증용 (비공개 방)
    @Query("SELECT r FROM Room r WHERE r.id = :roomId AND r.isPrivate = true AND r.password = :password")
    Optional<Room> findByIdAndPassword(@Param("roomId") Long roomId, @Param("password") String password);

    // 참가자 수 업데이트
    // TODO: Redis 기반으로 변경 예정 - 현재는 사용하지 않음
    @Deprecated
    @Modifying
    @Query("UPDATE Room r SET r.currentParticipants = " +
           "(SELECT COUNT(rm) FROM RoomMember rm WHERE rm.room.id = r.id) " +
           "WHERE r.id = :roomId")
    void updateCurrentParticipants(@Param("roomId") Long roomId);
}
