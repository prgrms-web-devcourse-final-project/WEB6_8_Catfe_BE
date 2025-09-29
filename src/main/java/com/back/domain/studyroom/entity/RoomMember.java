package com.back.domain.studyroom.entity;

import com.back.domain.user.entity.User;
import com.back.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * RoomMember 엔티티 - 방과 사용자 간의 영구적인 멤버십 관계를 나타냄
 *   room (1) : RoomMember (N) - 한 방에 여러 멤버
 *   user (1) : RoomMember (N) - 한 사용자가 여러 방의 멤버
 */
@Entity
@NoArgsConstructor
@Getter
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"room_id", "user_id"}))
public class RoomMember extends BaseEntity {
    
    // ==================== 영구 데이터 (DB에서 관리) ====================
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id", nullable = false)
    private Room room;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RoomRole role = RoomRole.VISITOR;

    @Column(nullable = false)
    private LocalDateTime joinedAt;  // 방에 처음 가입한 시간 (불변)

    private LocalDateTime lastActiveAt;  // 마지막 활동 시간 (참고용, 정확성 낮음)

    // ==================== 권한 확인 메서드 ====================
    
    /**
     * 방 관리 권한 확인 (방장, 부방장)
     * 사용: 방 설정 변경, 공지사항 작성 등
     */
    public boolean canManageRoom() {
        return role.canManageRoom();
    }

    /**
     * 멤버 추방 권한 확인 (방장, 부방장)
     * 사용: 다른 멤버를 추방할 때
     */
    public boolean canKickMember() {
        return role.canKickMember();
    }

    /**
     * 공지사항 관리 권한 확인 (방장, 부방장)
     * 사용: 공지사항 작성/삭제
     */
    public boolean canManageNotices() {
        return role.canManageNotices();
    }

    /**
     * 방장 여부 확인
     * 사용: 방 삭제, 호스트 권한 이양 등
     */
    public boolean isHost() {
        return role.isHost();
    }

    /**
     * 정식 멤버 여부 확인 (방문객이 아닌 멤버, 부방장, 방장)
     * 사용: 멤버만 접근 가능한 기능
     */
    public boolean isMember() {
        return role.isMember();
    }

    // ==================== 정적 팩토리 메서드 ====================
    
    /**
     * 기본 멤버 생성
     */
    private static RoomMember create(Room room, User user, RoomRole role) {
        RoomMember member = new RoomMember();
        member.room = room;
        member.user = user;
        member.role = role;
        member.joinedAt = LocalDateTime.now();
        member.lastActiveAt = LocalDateTime.now();
        return member;
    }

    /**
     * 방장 멤버 생성
     * 사용: 방 생성 시 생성자를 방장으로 등록
     */
    public static RoomMember createHost(Room room, User user) {
        return create(room, user, RoomRole.HOST);
    }

    /**
     * 일반 멤버 생성
     * 사용: 비공개 방에 초대된 사용자를 정식 멤버로 등록
     */
    public static RoomMember createMember(Room room, User user) {
        return create(room, user, RoomRole.MEMBER);
    }

    /**
     * 방문객 생성
     * 사용: 공개 방에 처음 입장하는 사용자를 임시 방문객으로 등록
     */
    public static RoomMember createVisitor(Room room, User user) {
        return create(room, user, RoomRole.VISITOR);
    }
    
    // ==================== 상태 변경 메서드 ====================
    
    /**
     * 멤버 역할 변경
     * 사용: 방장이 멤버를 승격/강등시킬 때
     */
    public void updateRole(RoomRole newRole) {
        this.role = newRole;
    }

    /**
     * 마지막 활동 시간 업데이트
     * 참고용이며, 정확한 활동 추적은 Redis의 WebSocketSessionManager를 사용하세요.
     */
    public void updateLastActivity() {
        this.lastActiveAt = LocalDateTime.now();
    }
}
