package com.back.domain.studyroom.entity;

import com.back.domain.user.common.entity.User;
import com.back.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * 방 즐겨찾기 엔티티
 * - 사용자가 특정 방을 즐겨찾기에 추가
 * - userId + roomId 조합으로 중복 방지
 */
@Entity
@Getter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Table(
    name = "room_favorite",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_room_favorite_user_room",
        columnNames = {"user_id", "room_id"}
    ),
    indexes = {
        @Index(name = "idx_room_favorite_user", columnList = "user_id"),
        @Index(name = "idx_room_favorite_room", columnList = "room_id")
    }
)
public class RoomFavorite extends BaseEntity {
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id", nullable = false)
    private Room room;
    
    /**
     * 즐겨찾기 생성 정적 팩토리 메서드
     */
    public static RoomFavorite create(User user, Room room) {
        return RoomFavorite.builder()
                .user(user)
                .room(room)
                .build();
    }
}
