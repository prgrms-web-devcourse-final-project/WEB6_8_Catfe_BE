package com.back.domain.studyroom.entity;

import com.back.domain.user.common.entity.User;
import com.back.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * 스터디룸 방명록 엔티티
 * 방을 방문한 사용자들이 남기는 메시지
 */
@Entity
@Getter
@NoArgsConstructor
@Table(name = "room_guestbook")
public class RoomGuestbook extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id", nullable = false)
    private Room room;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 500)
    private String content;

    @OneToMany(mappedBy = "guestbook", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<RoomGuestbookReaction> reactions = new ArrayList<>();

    private RoomGuestbook(Room room, User user, String content) {
        this.room = room;
        this.user = user;
        this.content = content;
    }

    public static RoomGuestbook create(Room room, User user, String content) {
        return new RoomGuestbook(room, user, content);
    }

    public void updateContent(String content) {
        this.content = content;
    }

    public boolean isAuthor(Long userId) {
        return this.user.getId().equals(userId);
    }
}
