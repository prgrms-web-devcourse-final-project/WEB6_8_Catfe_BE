package com.back.domain.studyroom.entity;

import com.back.domain.user.common.entity.User;
import com.back.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

/**
 * 방 공지사항 엔티티
 * - 방장만 생성/수정/삭제 가능
 * - 핀 고정 기능 (방장이 중요 공지를 상단에 고정)
 */
@Entity
@Getter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Table(
    name = "room_announcement",
    indexes = {
        @Index(name = "idx_announcement_room", columnList = "room_id"),
        @Index(name = "idx_announcement_pinned", columnList = "is_pinned, created_at")
    }
)
public class RoomAnnouncement extends BaseEntity {
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id", nullable = false)
    private Room room;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;
    
    @Column(nullable = false, length = 100)
    private String title;
    
    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;
    
    @Column(name = "is_pinned", nullable = false)
    @Builder.Default
    private boolean isPinned = false;
    
    @Column(name = "pinned_at")
    private LocalDateTime pinnedAt;
    
    /**
     * 공지사항 생성 정적 팩토리 메서드
     */
    public static RoomAnnouncement create(Room room, User createdBy, String title, String content) {
        return RoomAnnouncement.builder()
                .room(room)
                .createdBy(createdBy)
                .title(title)
                .content(content)
                .isPinned(false)
                .build();
    }
    
    /**
     * 공지사항 수정
     */
    public void update(String title, String content) {
        this.title = title;
        this.content = content;
    }
    
    /**
     * 핀 고정 토글
     */
    public void togglePin() {
        this.isPinned = !this.isPinned;
        this.pinnedAt = this.isPinned ? LocalDateTime.now() : null;
    }
    
    /**
     * 핀 고정 설정
     */
    public void pin() {
        if (!this.isPinned) {
            this.isPinned = true;
            this.pinnedAt = LocalDateTime.now();
        }
    }
    
    /**
     * 핀 고정 해제
     */
    public void unpin() {
        if (this.isPinned) {
            this.isPinned = false;
            this.pinnedAt = null;
        }
    }
}
