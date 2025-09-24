package com.back.domain.studyroom.entity;

/**
 * 방 내에서 사용자의 역할을 정의하는 Enum
 * 
 * 역할:
 * - HOST: 방장 (최고 권한, 방 소유자)
 * - SUB_HOST: 부방장 (방장의 권한 일부 위임받음)  
 * - MEMBER: 정식 멤버 (기본 권한)
 * - VISITOR: 방문객 (제한된 권한, 임시 사용자)
 * 
 * 권한 : HOST > SUB_HOST > MEMBER > VISITOR
 */
public enum RoomRole {
    HOST("방장"),
    SUB_HOST("부방장"),
    MEMBER("멤버"),
    VISITOR("방문객");
    
    private final String description;
    
    RoomRole(String description) {
        this.description = description;
    }
    
    public String getDescription() {
        return description;
    }

    /*
     방 관리 권한 확인 (방 설정 변경, 공지사항 관리)
     허용 역할: HOST, SUB_HOST
     */
    public boolean canManageRoom() {
        return this == HOST || this == SUB_HOST;
    }
    
    /*
     멤버 추방 권한 확인
     허용 역할: HOST, SUB_HOST
     */
    public boolean canKickMember() {
        return this == HOST || this == SUB_HOST;
    }
    
    /*
     공지사항 관리 권한 확인
     허용 역할: HOST, SUB_HOST
     */
    public boolean canManageNotices() {
        return this == HOST || this == SUB_HOST;
    }
    
    /*
     방장 여부 확인
     */
    public boolean isHost() {
        return this == HOST;
    }
    
    /*
     정식 멤버 여부 확인 (방문객 제외)
     멤버만 가능한 기능(아직 미정..)에서 사용
     */
    public boolean isMember() {
        return this == MEMBER || this == SUB_HOST || this == HOST;
    }
}
