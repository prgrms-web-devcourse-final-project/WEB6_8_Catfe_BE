package com.back.domain.studyroom.entity;

/*
 방의 현재 상태를 정의하는 Enum
 (이쪽 로직은 구현에 따라서 상태 축소 가능.. 현재는 임의 상태)
 상태 전환 흐름:
 WAITING → ACTIVE → PAUSED ⟷ ACTIVE → TERMINATED

 상태별 의미:
 WAITING: 방이 생성되었지만 아직 스터디를 시작하지 않은 상태
 ACTIVE: 현재 스터디가 진행 중인 상태
 PAUSED: 스터디를 일시 정지한 상태 (휴식 시간 등)
 TERMINATED: 스터디가 완전히 종료된 상태 (더 이상 사용 불가)
 */
public enum RoomStatus {
    WAITING("대기 중"),
    ACTIVE("진행 중"), 
    PAUSED("일시 정지"),
    TERMINATED("종료됨");
    
    private final String description;
    
    RoomStatus(String description) {
        this.description = description;
    }
    
    public String getDescription() {
        return description;
    }
    
    /**
     * 방에 입장 가능한 상태인지 확인
     * 사용 상황: 사용자가 방 입장을 시도할 때
     * 허용 상태: WAITING, ACTIVE (대기 중이거나 진행 중일 때만 입장 가능)
     */
    public boolean isJoinable() {
        return this == WAITING || this == ACTIVE;
    }
    
    /**
     * 방이 활성 상태인지 확인  
     * 사용 상황: 실제 스터디가 진행되고 있는 방인지 체크
     * 활성 상태: ACTIVE (실제 스터디가 진행 중인 상태만)
     */
    public boolean isActive() {
        return this == ACTIVE;
    }
}
