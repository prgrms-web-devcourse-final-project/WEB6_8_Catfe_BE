package com.back.domain.studyroom.repository;

import com.back.domain.studyroom.entity.RoomMemberAvatar;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Repository
public interface RoomMemberAvatarRepository extends JpaRepository<RoomMemberAvatar, Long> {
    
    /**
     * 특정 방에서 특정 사용자의 아바타 설정 조회
     */
    Optional<RoomMemberAvatar> findByRoomIdAndUserId(Long roomId, Long userId);
    
    /**
     * 특정 방의 모든 아바타 설정 조회 (일괄 조회용)
     */
    @Query("SELECT rma FROM RoomMemberAvatar rma " +
           "JOIN FETCH rma.selectedAvatar " +
           "WHERE rma.room.id = :roomId AND rma.user.id IN :userIds")
    List<RoomMemberAvatar> findByRoomIdAndUserIdIn(@Param("roomId") Long roomId, 
                                                     @Param("userIds") Set<Long> userIds);
}
