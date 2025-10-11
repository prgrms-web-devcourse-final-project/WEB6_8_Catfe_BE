package com.back.domain.studyroom.entity;

import com.back.domain.user.entity.User;
import com.back.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

/**
 * 방별 아바타 설정 테이블
 * - MEMBER 등급 이상만 저장됨 (VISITOR는 저장 안함)
 * - 사용자가 아바타를 변경하면 이 테이블에 기록
 * - 재입장 시 저장된 아바타 자동 로드
 */
@Entity
@Getter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "room_member_avatars",
       uniqueConstraints = @UniqueConstraint(columnNames = {"room_id", "user_id"}))
public class RoomMemberAvatar extends BaseEntity {
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id", nullable = false)
    private Room room;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "avatar_id", nullable = false)
    private Avatar selectedAvatar;
    
    private LocalDateTime updatedAt;
    
    /**
     * 선택한 아바타 변경
     */
    public void setSelectedAvatar(Avatar newAvatar) {
        this.selectedAvatar = newAvatar;
        this.updatedAt = LocalDateTime.now();
    }
}
