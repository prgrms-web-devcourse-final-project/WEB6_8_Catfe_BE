package com.back.domain.studyroom.entity;

import com.back.domain.user.entity.User;
import com.back.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 방 초대 코드 엔티티
 * - 모든 참여자가 생성 가능
 * - 3시간 유효
 * - 사용 횟수 무제한
 */
@Entity
@Table(
    name = "room_invite_codes",
    indexes = {
        @Index(name = "idx_invite_code", columnList = "invite_code"),
        @Index(name = "idx_room_id", columnList = "room_id"),
        @Index(name = "idx_expires_at", columnList = "expires_at")
    }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RoomInviteCode extends BaseEntity {

    @Column(name = "invite_code", nullable = false, unique = true, length = 8)
    private String inviteCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id", nullable = false)
    private Room room;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

    @Builder
    private RoomInviteCode(String inviteCode, Room room, User createdBy, LocalDateTime expiresAt) {
        this.inviteCode = inviteCode;
        this.room = room;
        this.createdBy = createdBy;
        this.expiresAt = expiresAt;
        this.isActive = true;
    }

    /**
     * 초대 코드 생성
     * @param inviteCode 8자리 랜덤 코드
     * @param room 방
     * @param createdBy 생성자
     */
    public static RoomInviteCode create(String inviteCode, Room room, User createdBy) {
        LocalDateTime expiresAt = LocalDateTime.now().plusHours(3);
        
        return RoomInviteCode.builder()
                .inviteCode(inviteCode)
                .room(room)
                .createdBy(createdBy)
                .expiresAt(expiresAt)
                .build();
    }

    /**
     * 유효성 검증
     */
    public boolean isValid() {
        if (!isActive) return false;
        if (LocalDateTime.now().isAfter(expiresAt)) return false;
        return true;
    }

    /**
     * 비활성화
     */
    public void deactivate() {
        this.isActive = false;
    }
}
