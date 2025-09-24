package com.back.domain.user.entity;

import com.back.domain.board.entity.*;
import com.back.domain.file.entity.FileAttachment;
import com.back.domain.study.plan.entity.StudyPlan;
import com.back.domain.study.todo.entity.Todo;
import com.back.domain.studyroom.entity.RoomChatMessage;
import com.back.domain.studyroom.entity.RoomMember;
import com.back.domain.studyroom.entity.RoomParticipantHistory;
import com.back.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Table(name = "USERS")
@NoArgsConstructor
public class User extends BaseEntity {
    private String username;

    private String email;

    private String password;

    @Enumerated(EnumType.STRING)
    private Role role;

    private String provider;

    private String providerId;

    @Enumerated(EnumType.STRING)
    private UserStatus userStatus;

    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private UserProfile userProfile;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<UserToken> userTokens = new ArrayList<>();

    @OneToMany(mappedBy = "fromUser", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PrivateChatMessage> sentMessages = new ArrayList<>();

    @OneToMany(mappedBy = "toUser", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PrivateChatMessage> receivedMessages = new ArrayList<>();

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<RoomMember> roomMembers = new ArrayList<>();

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<RoomChatMessage> roomChatMessages = new ArrayList<>();

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<RoomParticipantHistory> roomParticipantHistories = new ArrayList<>();

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<StudyPlan> studyPlans = new ArrayList<>();

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Todo> todos = new ArrayList<>();

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Post> posts = new ArrayList<>();

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PostLike> postLikes = new ArrayList<>();

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PostBookmark> postBookmarks = new ArrayList<>();

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Comment> comments = new ArrayList<>();

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CommentLike> commentLikes = new ArrayList<>();

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<FileAttachment> fileAttachments = new ArrayList<>();

    // -------------------- 생성자 --------------------
    public User(String username, String email, String password, Role role, UserStatus userStatus) {
        this.username = username;
        this.email = email;
        this.password = password;
        this.role = role;
        this.userStatus = userStatus;
    }

    // -------------------- 정적 팩토리 메서드 --------------------
    // 일반 사용자 생성
    public static User createUser(String username, String email, String password) {
        return new User(username, email, password, Role.USER, UserStatus.PENDING);
    }

    // 관리자 사용자 생성
    public static User createAdmin(String username, String email, String password) {
        return new User(username, email, password, Role.ADMIN, UserStatus.ACTIVE);
    }

    // -------------------- 연관관계 메서드 --------------------
    public void setUserProfile(UserProfile profile) {
        this.userProfile = profile;
        profile.setUser(this);
    }

    // -------------------- 헬퍼 메서드 --------------------
    // 현재 사용자의 닉네임 조회
    public String getNickname() {
        return userProfile != null && userProfile.getNickname() != null && !userProfile.getNickname().trim().isEmpty()
                ? userProfile.getNickname()
                : this.username;
    }

    // 현재 사용자의 프로필 이미지 URL 조회
    public String getProfileImageUrl() {
        return userProfile != null ? userProfile.getProfileImageUrl() : null;
    }
}
