package com.back.domain.studyroom.entity;

import com.back.domain.user.entity.User;
import com.back.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@NoArgsConstructor
@Getter
public class RoomMember extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id")
    private Room room;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinTable(name = "user_id")
    private User user;

    @Enumerated(EnumType.STRING)
    private RoomRole role;

    private LocalDateTime joinedAt;

    private LocalDateTime lastActiveAt;
}
