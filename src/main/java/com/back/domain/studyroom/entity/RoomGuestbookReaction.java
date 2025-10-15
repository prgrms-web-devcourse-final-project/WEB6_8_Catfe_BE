package com.back.domain.studyroom.entity;

import com.back.domain.user.common.entity.User;
import com.back.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 방명록 이모지 반응 엔티티
 * 각 방명록에 사용자들이 남기는 이모지 반응
 */
@Entity
@Getter
@NoArgsConstructor
@Table(
    name = "room_guestbook_reaction",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_guestbook_user_emoji",
            columnNames = {"guestbook_id", "user_id", "emoji"}
        )
    }
)
public class RoomGuestbookReaction extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "guestbook_id", nullable = false)
    private RoomGuestbook guestbook;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 10)
    private String emoji;

    private RoomGuestbookReaction(RoomGuestbook guestbook, User user, String emoji) {
        this.guestbook = guestbook;
        this.user = user;
        this.emoji = emoji;
    }

    public static RoomGuestbookReaction create(RoomGuestbook guestbook, User user, String emoji) {
        return new RoomGuestbookReaction(guestbook, user, emoji);
    }

    public boolean isReactedBy(Long userId) {
        return this.user.getId().equals(userId);
    }
}
