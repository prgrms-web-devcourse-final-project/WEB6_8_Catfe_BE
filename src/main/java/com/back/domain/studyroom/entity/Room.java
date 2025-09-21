package com.back.domain.studyroom.entity;

import com.back.domain.study.entity.StudyRecord;
import com.back.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Entity
@NoArgsConstructor
@Getter
public class Room extends BaseEntity {
    private String title;
    private String description;

    private boolean isPrivate;

    private String password;

    private int maxParticipants;

    private boolean isActive;

    private boolean allowCamera;

    private boolean allowAudio;

    private boolean allowScreenShare;

    @ManyToOne
    @JoinColumn(name = "theme_id")
    private RoomTheme theme;

    @OneToMany(mappedBy = "room", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<RoomMember> roomMembers = new ArrayList<>();

    @OneToMany(mappedBy = "room", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<RoomChatMessage> roomChatMessages = new ArrayList<>();

    @OneToMany(mappedBy = "room", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<RoomParticipantHistory> roomParticipantHistories = new ArrayList<>();

    @OneToMany(mappedBy = "room", cascade = CascadeType.ALL, orphanRemoval = true)
    public List<StudyRecord> studyRecords = new ArrayList<>();
}
