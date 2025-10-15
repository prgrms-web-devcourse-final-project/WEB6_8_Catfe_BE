package com.back.domain.studyroom.entity;

import com.back.domain.user.common.entity.User;
import com.back.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 방명록 개인 핀 엔티티
 * 각 사용자가 자신만의 방명록 핀을 설정할 수 있음
 * 핀한 방명록은 목록 조회 시 최상단에 표시됨
 */
@Entity
@Getter
@NoArgsConstructor
@Table(
    name = "room_guestbook_pin",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_user_guestbook_pin",
            columnNames = {"user_id", "guestbook_id"}
        )
    }
)
public class RoomGuestbookPin extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "guestbook_id", nullable = false)
    private RoomGuestbook guestbook;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    private RoomGuestbookPin(RoomGuestbook guestbook, User user) {
        this.guestbook = guestbook;
        this.user = user;
    }

    public static RoomGuestbookPin create(RoomGuestbook guestbook, User user) {
        return new RoomGuestbookPin(guestbook, user);
    }

    public boolean isPinnedBy(Long userId) {
        return this.user.getId().equals(userId);
    }
}
