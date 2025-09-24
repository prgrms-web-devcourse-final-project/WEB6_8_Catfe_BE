package com.back.domain.studyroom.entity;

import com.back.domain.user.entity.User;
import com.back.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/*
 RoomMember 엔티티 - 방과 사용자 간의 멤버십 관계를 나타냄
 연관관계 :
 - Room (1) : RoomMember (N) - 한 방에 여러 멤버가 있을 수 있음
 - User (1) : RoomMember (N) - 한 사용자가 여러 방의 멤버가 될 수 있음
 @JoinColumn vs @JoinTable 선택 이유:
 - @JoinColumn: 외래키를 이용한 직접 관계 (현재 변경)
 - @JoinTable: 별도의 연결 테이블을 만드는 관계
 RoomMember 테이블에서 그냥 room_id와 user_id 외래키로 직접 연결.
 */
@Entity
@NoArgsConstructor
@Getter
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"room_id", "user_id"}))
public class RoomMember extends BaseEntity {
    
    // 방 정보 - 어떤 방의 멤버인지
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id")  // room_member 테이블의 room_id 컬럼이 room 테이블의 id를 참조
    private Room room;

    // 사용자 정보 - 누가 이 방의 멤버인지
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")  // room_member 테이블의 user_id 컬럼이 users 테이블의 id를 참조
    private User user;

    // 방 내에서의 역할 (방장, 부방장, 멤버, 방문객)
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RoomRole role = RoomRole.VISITOR;

    // 멤버십 기본 정보
    @Column(nullable = false)
    private LocalDateTime joinedAt;  // 방에 처음 입장한 시간
    private LocalDateTime lastActiveAt;  // 마지막으로 활동한 시간

    // 실시간 상태 관리 필드들
    @Column(nullable = false)
    private boolean isOnline = false;  // 현재 방에 온라인 상태인지

    private String connectionId; // WebSocket 연결 ID (실시간 통신용)

    private LocalDateTime lastHeartbeat;  // 마지막 heartbeat 시간 (연결 상태 확인용)

    // 💡 권한 확인 메서드들 (RoomRole enum의 메서드를 위임)
    
    /**
     * 방 관리 권한이 있는지 확인 (방장, 부방장)
     방 설정 변경, 공지사항 작성 등의 권한이 필요할 때
     */
    public boolean canManageRoom() {
        return role.canManageRoom();
    }

    /**
     * 멤버 추방 권한이 있는지 확인 (방장, 부방장)
     다른 멤버를 추방하려고 할 때
     */
    public boolean canKickMember() {
        return role.canKickMember();
    }

    /**
     * 공지사항 관리 권한이 있는지 확인 (방장, 부방장)
     공지사항을 작성하거나 삭제할 때
     */
    public boolean canManageNotices() {
        return role.canManageNotices();
    }

    /**
     * 방장인지 확인
     방 소유자만 가능한 작업 (방 삭제, 호스트 권한 이양 등)
     */
    public boolean isHost() {
        return role.isHost();
    }

    /**
     * 정식 멤버인지 확인 (방문객이 아닌 멤버, 부방장, 방장)
     멤버만 접근 가능한 기능 (파일 업로드, 학습 기록 등)
     */
    public boolean isMember() {
        return role.isMember();
    }

    /**
     * 현재 활성 상태인지 확인
     온라인 멤버 목록 표시, 비활성 사용자 정리 등
     온라인 상태이고 최근 5분 이내에 heartbeat가 있었던 경우
     */
    public boolean isActive() {
        return isOnline && lastHeartbeat != null && 
               lastHeartbeat.isAfter(LocalDateTime.now().minusMinutes(5));
    }

    
    /**
     기본 멤버 생성 메서드, 처음 입장 시 사용
     */
    public static RoomMember create(Room room, User user, RoomRole role) {
        RoomMember member = new RoomMember();
        member.room = room;
        member.user = user;
        member.role = role;
        member.joinedAt = LocalDateTime.now();
        member.lastActiveAt = LocalDateTime.now();
        member.isOnline = true;  // 생성 시 온라인 상태
        member.lastHeartbeat = LocalDateTime.now();
        
        return member;
    }

    // 방장 멤버 생성 -> 새로운 방을 생성할 때 방 생성자를 방장으로 등록
    public static RoomMember createHost(Room room, User user) {
        return create(room, user, RoomRole.HOST);
    }

    /**
     * 일반 멤버 생성, 권한 자동 변경
     - 비공개 방에서 초대받은 사용자를 정식 멤버로 등록할 때 (로직 검토 중)
     */
    public static RoomMember createMember(Room room, User user) {
        return create(room, user, RoomRole.MEMBER);
    }

    /**
     * 방문객 생성
     * 사용 상황: 공개 방에 처음 입장하는 사용자를 임시 방문객으로 등록
     */
    public static RoomMember createVisitor(Room room, User user) {
        return create(room, user, RoomRole.VISITOR);
    }
    
    /**
     * 멤버의 역할 변경
     방장이 멤버를 부방장으로 승격시키거나 강등시킬 때
     */
    public void updateRole(RoomRole newRole) {
        this.role = newRole;
    }

    /**
     * 온라인 상태 변경
     * 사용 상황: 멤버가 방에 입장하거나 퇴장할 때
     활동 시간도 함께 업데이트, 온라인이 되면 heartbeat도 갱신
     */
    public void updateOnlineStatus(boolean online) {
        this.isOnline = online;
        this.lastActiveAt = LocalDateTime.now();
        if (online) {
            this.lastHeartbeat = LocalDateTime.now();
        }
    }

    /**
     * WebSocket 연결 ID 업데이트
     * 사용 상황: 멤버가 웹소켓으로 방에 연결될 때
     + heartbeat도 함께 갱신
     */
    public void updateConnectionId(String connectionId) {
        this.connectionId = connectionId;
        this.lastHeartbeat = LocalDateTime.now();
    }

    /**
     * 사용 : 클라이언트에서 주기적으로 서버에 연결 상태를 알릴 때
     * 목적: 연결이 끊어진 멤버를 자동으로 감지하기 위해 사용, 별도의 다른 것으로 변경 가능
     */
    public void heartbeat() {
        this.lastHeartbeat = LocalDateTime.now();
        this.lastActiveAt = LocalDateTime.now();
        this.isOnline = true;
    }

    /**
     * 방 퇴장 처리 (명시적 퇴장과 연결 끊김 상태 로직 분할 예정임.. 일단은 임시로 통합 상태)
     멤버가 방을 나가거나 연결이 끊어졌을 때, 오프라인 상태로 변경하고 연결 ID 제거
     */
    public void leave() {
        this.isOnline = false;
        this.connectionId = null;
    }
}
