package com.back.domain.studyroom.repository;

import com.back.domain.studyroom.entity.RoomFavorite;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface RoomFavoriteRepository extends JpaRepository<RoomFavorite, Long> {
    
    /**
     * 특정 사용자의 특정 방 즐겨찾기 조회
     */
    Optional<RoomFavorite> findByUserIdAndRoomId(Long userId, Long roomId);
    
    /**
     * 특정 사용자의 모든 즐겨찾기 조회 (최신순)
     * N+1 방지를 위해 Room과 JOIN FETCH
     */
    @Query("SELECT rf FROM RoomFavorite rf " +
           "JOIN FETCH rf.room r " +
           "LEFT JOIN FETCH r.createdBy " +
           "WHERE rf.user.id = :userId " +
           "ORDER BY rf.createdAt DESC")
    List<RoomFavorite> findByUserIdWithRoom(@Param("userId") Long userId);
    
    /**
     * 즐겨찾기 존재 여부 확인
     */
    boolean existsByUserIdAndRoomId(Long userId, Long roomId);
    
    /**
     * 여러 방에 대한 즐겨찾기 여부 일괄 조회 (N+1 방지용)
     */
    @Query("SELECT rf.room.id FROM RoomFavorite rf " +
           "WHERE rf.user.id = :userId AND rf.room.id IN :roomIds")
    Set<Long> findFavoriteRoomIds(@Param("userId") Long userId, @Param("roomIds") List<Long> roomIds);
    
    /**
     * 특정 방의 즐겨찾기 개수 (추후 통계용)
     */
    long countByRoomId(Long roomId);
}
