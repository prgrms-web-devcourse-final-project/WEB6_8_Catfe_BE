package com.back.domain.studyroom.repository;

import com.back.domain.studyroom.entity.Room;
import com.back.domain.studyroom.entity.RoomStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/*
 - 방 검색 및 필터링 (제목, 상태, 공개/비공개)
 - 참여 가능한 방 목록 조회
 - 사용자별 방 관리 (생성한 방, 참여 중인 방)
 - 방 통계 및 관리 (인기 방, 참가자 수 동기화)
 */
@Repository
public interface RoomRepository extends JpaRepository<Room, Long> {
    /*
     제목으로 방 검색
     사용 상황: 사용자가 검색창에서 방 이름을 검색할 때
     */
    List<Room> findByTitleContaining(String title);

    /*
     활성화된 방 목록 조회
     현재 사용 가능한 모든 방을 조회할 때(수정 예정, isActive에 만석 조건 추가 예정)
     */
    @Query("SELECT r FROM Room r WHERE r.isActive = true")
    List<Room> findActiveRooms();

    /*
     특정 상태의 방 목록 조회
     상태별로 방을 관리하거나 통계를 낼 때
     */
    @Query("SELECT r FROM Room r WHERE r.status = :status")
    List<Room> findByStatus(@Param("status") RoomStatus status);
    
    /*
     공개 방 중 입장 가능한 방들 조회 (페이징)
     - 메인 페이지에서 사용자에게 입장 가능한 방 목록을 보여줄 때
     비공개가 아니고, 활성화되어 있고, 입장 가능한 상태이며, 정원이 가득 차지 않은 방
     JOIN FETCH로 N+1 문제 방지 (createdBy 즉시 로딩)
     */
    @Query("SELECT r FROM Room r " +
           "JOIN FETCH r.createdBy " +
           "WHERE r.isPrivate = false AND r.isActive = true " +
           "AND r.status IN ('WAITING', 'ACTIVE') AND r.currentParticipants < r.maxParticipants " +
           "ORDER BY r.createdAt DESC")
    Page<Room> findJoinablePublicRooms(Pageable pageable);

    // 사용자가 생성한 방 목록 조회
    @Query("SELECT r FROM Room r WHERE r.createdBy.id = :createdById ORDER BY r.createdAt DESC")
    List<Room> findByCreatedById(@Param("createdById") Long createdById);

    /*
     사용자가 참여 중인 방 조회
     해당 사용자가 멤버로 등록되어 있고 현재 온라인 상태인 방
     JOIN FETCH로 N+1 문제 방지
     */
    @Query("SELECT r FROM Room r " +
           "JOIN FETCH r.createdBy " +
           "JOIN r.roomMembers rm " +
           "WHERE rm.user.id = :userId AND rm.isOnline = true")
    List<Room> findRoomsByUserId(@Param("userId") Long userId);

    // 방 존재 및 활성 상태 확인
    @Query("SELECT CASE WHEN COUNT(r) > 0 THEN true ELSE false END FROM Room r " +
           "WHERE r.id = :roomId AND r.isActive = true")
    boolean existsByIdAndActive(@Param("roomId") Long roomId);

    // 비밀번호 검증용 (비공개 방)
    @Query("SELECT r FROM Room r WHERE r.id = :roomId AND r.isPrivate = true AND r.password = :password")
    Optional<Room> findByIdAndPassword(@Param("roomId") Long roomId, @Param("password") String password);

    // 제목과 상태로 검색
    @Query("SELECT r FROM Room r WHERE " +
           "(:title IS NULL OR r.title LIKE %:title%) AND " +
           "(:status IS NULL OR r.status = :status) AND " +
           "(:isPrivate IS NULL OR r.isPrivate = :isPrivate)")
    Page<Room> findRoomsWithFilters(@Param("title") String title, 
                                   @Param("status") RoomStatus status,
                                   @Param("isPrivate") Boolean isPrivate,
                                   Pageable pageable);

    // 참가자 수 업데이트
    @Modifying
    @Query("UPDATE Room r SET r.currentParticipants = " +
           "(SELECT COUNT(rm) FROM RoomMember rm WHERE rm.room.id = r.id AND rm.isOnline = true) " +
           "WHERE r.id = :roomId")
    void updateCurrentParticipants(@Param("roomId") Long roomId);

    // 비활성 방 정리 (배치용)
    @Modifying
    @Query("UPDATE Room r SET r.status = 'TERMINATED', r.isActive = false " +
           "WHERE r.currentParticipants = 0 AND r.status = 'ACTIVE' " +
           "AND r.updatedAt < :cutoffTime")
    int terminateInactiveRooms(@Param("cutoffTime") java.time.LocalDateTime cutoffTime);

    // 인기 방 조회 (참가자 수 기준, 로직에 따라 수정 가능)
    // JOIN FETCH로 N+1 문제 방지
    @Query("SELECT r FROM Room r " +
           "JOIN FETCH r.createdBy " +
           "WHERE r.isPrivate = false AND r.isActive = true " +
           "ORDER BY r.currentParticipants DESC, r.createdAt DESC")
    Page<Room> findPopularRooms(Pageable pageable);

    // 비관적 락으로 방 조회 (동시성 제어용)
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM Room r WHERE r.id = :roomId")
    Optional<Room> findByIdWithLock(@Param("roomId") Long roomId);
}
