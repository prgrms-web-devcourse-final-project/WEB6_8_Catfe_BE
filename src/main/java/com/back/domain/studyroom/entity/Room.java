package com.back.domain.studyroom.entity;

import com.back.domain.study.record.entity.StudyRecord;
import com.back.domain.user.common.entity.User;
import com.back.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class Room extends BaseEntity {
    private String title;
    private String description;
    private boolean isPrivate;
    private String password;
    private int maxParticipants;
    private boolean isActive;
    
    // 방 썸네일 이미지 URL
    private String thumbnailUrl;
    
    // 디폴트 썸네일 URL
    private static final String DEFAULT_THUMBNAIL_URL = "/images/default-room-thumbnail.png";
    
    /**
     * 썸네일 URL 조회 (디폴트 처리 포함)
     * null인 경우 디폴트 이미지 반환
     */
    public String getThumbnailUrl() {
        return (thumbnailUrl != null && !thumbnailUrl.trim().isEmpty()) 
            ? thumbnailUrl 
            : DEFAULT_THUMBNAIL_URL;
    }
    
    /**
     * 원본 썸네일 URL 조회 (디폴트 처리 없음)
     * DB에 실제 저장된 값 그대로 반환
     */
    public String getRawThumbnailUrl() {
        return thumbnailUrl;
    }
    
    private boolean allowCamera;
    private boolean allowAudio;
    private boolean allowScreenShare;

    // 방 상태
    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RoomStatus status = RoomStatus.WAITING;

    // 방장
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    // 테마
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "theme_id")
    private RoomTheme theme;

    // 연관관계 설정
    @Builder.Default
    @OneToMany(mappedBy = "room", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<RoomMember> roomMembers = new ArrayList<>();

    // 채팅 메시지
    @Builder.Default
    @OneToMany(mappedBy = "room", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<RoomChatMessage> roomChatMessages = new ArrayList<>();

    // 참가자 기록
    @Builder.Default
    @OneToMany(mappedBy = "room", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<RoomParticipantHistory> roomParticipantHistories = new ArrayList<>();

    // 스터디 기록
    @Builder.Default
    @OneToMany(mappedBy = "room", cascade = CascadeType.ALL, orphanRemoval = true)
    public List<StudyRecord> studyRecords = new ArrayList<>();

    // 방명록
    @Builder.Default
    @OneToMany(mappedBy = "room", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<RoomGuestbook> guestbooks = new ArrayList<>();

    
    /**
     * 방에 입장할 수 있는지 확인
     * 사용 상황: 사용자가 방 입장을 시도할 때 입장 가능 여부를 미리 체크
     * 방이 활성화되어 있고, 입장 가능한 상태인 경우
     * 
     * 정원 체크는 Redis에서 실시간으로 수행 (RoomService에서 처리)
     */
    public boolean canJoin() {
        return isActive && status.isJoinable();
    }

    /**
     * 방의 정원이 가득 찼는지 확인
     * 
     * 실제 정원 체크는 Redis 기반으로 RoomService에서 수행
     * 이 메서드는 UI 표시용으로만 사용 (최대 정원 반환)
     */
    public int getMaxCapacity() {
        return maxParticipants;
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
        if (this.status == RoomStatus.TERMINATED) {
            throw new IllegalStateException("종료된 방은 활성화할 수 없습니다.");
        }
        this.status = RoomStatus.ACTIVE;
        this.isActive = true;
    }

    /**
     * 방을 일시 정지 상태로 변경
     */
    public void pause() {
        if (this.status == RoomStatus.TERMINATED) {
            throw new IllegalStateException("종료된 방은 일시정지할 수 없습니다.");
        }
        if (this.status != RoomStatus.ACTIVE) {
            throw new IllegalStateException("활성화된 방만 일시정지할 수 있습니다.");
        }
        this.status = RoomStatus.PAUSED;
    }

    /**
     * 방을 종료 상태로 변경
     방장이 스터디를 완전히 끝내거나, 빈 방을 자동 정리할 때 (종료 처리를 어떻게 뺄지에 따라 변경 될 예정)
     */
    public void terminate() {
        if (this.status == RoomStatus.TERMINATED) {
            return; // 이미 종료된 방은 무시
        }
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
     * @param useWebRTC WebRTC 사용 여부 (true: 카메라/오디오/화면공유 전체 활성화, false: 전체 비활성화)
     */
    public static Room create(String title, String description, boolean isPrivate, 
                             String password, int maxParticipants, User creator, RoomTheme theme,
                             boolean useWebRTC, String thumbnailUrl) {
        Room room = new Room();
        room.title = title;
        room.description = description;
        room.isPrivate = isPrivate;
        room.password = password;
        room.maxParticipants = maxParticipants;
        room.isActive = true;  // 생성 시 기본적으로 활성화
        room.thumbnailUrl = thumbnailUrl;  // 썸네일 URL
        room.allowCamera = useWebRTC;  // WebRTC 사용 여부에 따라 설정
        room.allowAudio = useWebRTC;   // WebRTC 사용 여부에 따라 설정
        room.allowScreenShare = useWebRTC;  // WebRTC 사용 여부에 따라 설정
        room.status = RoomStatus.WAITING;  // 생성 시 대기 상태
        room.createdBy = creator;
        room.theme = theme;
        
        // 컬렉션 필드 명시적 초기화 (null 방지)
        room.roomMembers = new ArrayList<>();
        room.roomChatMessages = new ArrayList<>();
        room.roomParticipantHistories = new ArrayList<>();
        room.studyRecords = new ArrayList<>();
        room.guestbooks = new ArrayList<>();
        
        return room;
    }

    /**
     * 방 설정 일괄 업데이트 메서드 (썸네일 포함)
     * 방장이 방 설정을 변경할 때 여러 필드를 한 번에 업데이트
     * WebRTC 설정은 제외 (확장성을 위해 제거가 아닌 주석으로 현재 놔둠..)
     */
    public void updateSettings(String title, String description, int maxParticipants, String thumbnailUrl) {
        this.title = title;
        this.description = description;
        this.maxParticipants = maxParticipants;
        this.thumbnailUrl = thumbnailUrl;
    }
    
    /**
     * WebRTC 설정 업데이트 메서드 (추후 사용 가능!)
     * 현재는 미사용 - 추후 팀원이 만약 구현 시 활성화 되도록
     */
    public void updateWebRTCSettings(boolean allowCamera, boolean allowAudio, boolean allowScreenShare) {
        this.allowCamera = allowCamera;
        this.allowAudio = allowAudio;
        this.allowScreenShare = allowScreenShare;
    }

    /**
     * 방 비밀번호 변경 메서드
     방장이 방의 비밀번호를 변경할 때
     별도 메서드로 분리한 이유: 비밀번호는 보안상 별도로 관리되어야 하기 때문
     */
    public void updatePassword(String newPassword) {
        this.password = newPassword;
        
        // 비밀번호 유무에 따라 isPrivate 자동 설정
        this.isPrivate = (newPassword != null && !newPassword.trim().isEmpty());
    }
}
