package com.back.domain.studyroom.entity;

import com.back.domain.study.entity.StudyRecord;
import com.back.domain.user.entity.User;
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

    // 방 상태
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RoomStatus status = RoomStatus.WAITING;
    // 현재 참여자
    @Column(nullable = false)
    private int currentParticipants = 0;
    // 방장
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;
    // 테마
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "theme_id")
    private RoomTheme theme;

    // 연관관계 설정
    @OneToMany(mappedBy = "room", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<RoomMember> roomMembers = new ArrayList<>();
    // 채팅 메시지
    @OneToMany(mappedBy = "room", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<RoomChatMessage> roomChatMessages = new ArrayList<>();
    // 참가자 기록
    @OneToMany(mappedBy = "room", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<RoomParticipantHistory> roomParticipantHistories = new ArrayList<>();
    // 스터디 기록
    @OneToMany(mappedBy = "room", cascade = CascadeType.ALL, orphanRemoval = true)
    public List<StudyRecord> studyRecords = new ArrayList<>();

    
    /**
     * 방에 입장할 수 있는지 확인
     * 사용 상황: 사용자가 방 입장을 시도할 때 입장 가능 여부를 미리 체크
     방이 활성화되어 있고, 입장 가능한 상태이며, 정원이 초과되지 않은 경우
     */
    public boolean canJoin() {
        return isActive && status.isJoinable() && currentParticipants < maxParticipants;
    }

    /**
     * 방의 정원이 가득 찼는지 확인
     방 목록에서 "만석" 표시하거나, 입장 제한할 때
     */
    public boolean isFull() {
        return currentParticipants >= maxParticipants;
    }

    /**
     * 참가자 수 증가 (최대 정원까지만)
     누군가 방에 입장했을 때 참가자 수를 1 증가시킴
     최대 정원을 초과하지 않도록 체크
     */
    public void incrementParticipant() {
        if (currentParticipants < maxParticipants) {
            this.currentParticipants++;
        }
    }

    /**
     * 참가자 수 감소 (0 이하로는 감소하지 않음)
     누군가 방에서 나갔을 때 참가자 수를 1 감소시킴
     음수가 되지 않도록 체크
     */
    public void decrementParticipant() {
        if (this.currentParticipants > 0) {
            this.currentParticipants--;
        }
    }

    /**
     * 비밀번호 입력이 필요한 방인지 확인
     비공개 방에 입장할 때 비밀번호 입력 폼을 보여줄지 결정
     비공개 방이면서 실제로 비밀번호가 설정되어 있는 경우
     */
    public boolean needsPassword() {
        return isPrivate && password != null && !password.trim().isEmpty();
    }

    /**
     * 방을 활성 상태로 변경
     방장이 스터디를 시작할 때 또는 대기 중인 방을 활성화할 때
     */
    public void activate() {
        this.status = RoomStatus.ACTIVE;
        this.isActive = true;
    }

    /**
     * 방을 일시 정지 상태로 변경
     */
    public void pause() {
        this.status = RoomStatus.PAUSED;
    }

    /**
     * 방을 종료 상태로 변경
     방장이 스터디를 완전히 끝내거나, 빈 방을 자동 정리할 때 (종료 처리를 어떻게 뺄지에 따라 변경 될 예정)
     */
    public void terminate() {
        this.status = RoomStatus.TERMINATED;
        this.isActive = false;
    }

    /**
     * 특정 사용자가 이 방의 소유자(방장)인지 확인
     방 설정 변경, 방 종료, 멤버 추방 등의 권한이 필요한 작업 전에 체크
     */
    public boolean isOwner(Long userId) {
        return createdBy != null && createdBy.getId().equals(userId);
    }

    /**
     * 방 생성을 위한 정적 팩토리 메서드
     새로운 방을 생성할 때 모든 기본값을 설정 해주는 초기 메서드
     기본 상태에서 방장이 임의로 변형하고 싶은 부분만 변경해서 사용 가능
     */
    public static Room create(String title, String description, boolean isPrivate, 
                             String password, int maxParticipants, User creator, RoomTheme theme) {
        Room room = new Room();
        room.title = title;
        room.description = description;
        room.isPrivate = isPrivate;
        room.password = password;
        room.maxParticipants = maxParticipants;
        room.isActive = true;  // 생성 시 기본적으로 활성화
        room.allowCamera = true;  // 기본적으로 카메라 허용
        room.allowAudio = true;   // 기본적으로 오디오 허용
        room.allowScreenShare = true;  // 기본적으로 화면 공유 허용
        room.status = RoomStatus.WAITING;  // 생성 시 대기 상태
        room.currentParticipants = 0;  // 생성 시 참가자 0명
        room.createdBy = creator;
        room.theme = theme;
        
        return room;
    }

    /**
     * 방 설정 일괄 업데이트 메서드
     방장이 방 설정을 변경할 때 여러 필드를 한 번에 업데이트
     주된 생성 이유.. rtc 단체 제어를 위해 잡아놓았음. 잡아준 필드 변경 가능성 농후!!
     */
    public void updateSettings(String title, String description, int maxParticipants,
                              boolean allowCamera, boolean allowAudio, boolean allowScreenShare) {
        this.title = title;
        this.description = description;
        this.maxParticipants = maxParticipants;
        this.allowCamera = allowCamera;
        this.allowAudio = allowAudio;
        this.allowScreenShare = allowScreenShare;
    }

    /**
     * 방 비밀번호 변경 메서드
     방장이 방의 비밀번호를 변경할 때
     별도 메서드로 분리한 이유: 비밀번호는 보안상 별도로 관리되어야 하기 때문 (ai의 추천)
     */
    public void updatePassword(String newPassword) {
        this.password = newPassword;
    }
}
