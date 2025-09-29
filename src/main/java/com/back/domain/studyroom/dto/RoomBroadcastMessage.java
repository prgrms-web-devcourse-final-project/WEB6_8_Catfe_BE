package com.back.domain.studyroom.dto;

import com.back.domain.studyroom.entity.RoomMember;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 방 관련 실시간 브로드캐스트 메시지 DTO
 * - WebSocket을 통해 방 내 모든 멤버에게 전송되는 메시지의 표준 형식 정의
 - 다양한 방 이벤트를 실시간으로 알림

 * 사용 패턴:
 1. 서버에서 방 이벤트 발생 시 해당하는 정적 메서드 호출
 2. 생성된 메시지를 WebSocketSessionManager.broadcastToRoom()으로 전송
 3. 클라이언트에서 /topic/rooms/{roomId}/updates 채널을 구독하여 수신
 */
@Getter
public class RoomBroadcastMessage {

    // 브로드캐스트 메시지의 종류 (어떤 이벤트인지 구분)
    private final BroadcastType type;

    // 메시지가 발생한 방의 ID (클라이언트에서 어느 방의 이벤트인지 확인용)
    private final Long roomId;

    // 메시지 생성 시각 (클라이언트에서 시간순 정렬이나 만료 체크용)
    private final LocalDateTime timestamp;

    // 이벤트와 관련된 실제 데이터 (멤버 정보, 온라인 목록 등)
    // Object 타입으로 다양한 데이터 구조를 담을 수 있도록 설계
    private final Object data;

    // 사용자에게 표시할 사람이 읽기 쉬운 메시지 (UI에서 사용될 수 있는 부분)
    private final String message;

    /**
     * 브로드캐스트 메시지 생성자 (private)
     - 외부에서 직접 생성하지 않고 정적 팩토리 메서드를 통해서만 생성
     - 이렇게 하면 메시지 타입별로 적절한 데이터와 메시지가 확실히 설정됨
     */
    private RoomBroadcastMessage(BroadcastType type, Long roomId, Object data, String message) {
        this.type = type;
        this.roomId = roomId;
        this.timestamp = LocalDateTime.now(); // 메시지 생성 시점 자동 기록
        this.data = data;
        this.message = message;
    }

    /**
     * 멤버 입장 알림 메시지 생성
     RoomService.joinRoom() 메서드에서 멤버가 성공적으로 입장했을 때
     * @param roomId 입장한 방의 ID
     * @param member 입장한 멤버 정보 (RoomMember 엔티티)
     * @return 입장 알림 브로드캐스트 메시지
     */
    public static RoomBroadcastMessage memberJoined(Long roomId, RoomMember member) {
        // 멤버 정보를 클라이언트용 DTO로 변환 (민감한 정보 제외하고 필요한 정보만)
        RoomMemberResponse memberData = RoomMemberResponse.from(member);
        
        // 사용자 친화적인 알림 메시지 생성 (닉네임 사용)
        String message = String.format("%s님이 방에 입장했습니다.", member.getUser().getNickname());
        
        return new RoomBroadcastMessage(BroadcastType.MEMBER_JOINED, roomId, memberData, message);
    }

    /**
     * 멤버 퇴장 알림 메시지 생성
     RoomService.leaveRoom() 메서드에서 멤버가 퇴장했을 때 (명시적 퇴장 또는 강제 퇴장)
     * @param roomId 퇴장한 방의 ID
     * @param member 퇴장한 멤버 정보 (퇴장 처리 전에 미리 정보 백업 필요)
     * @return 퇴장 알림 브로드캐스트 메시지
     */
    public static RoomBroadcastMessage memberLeft(Long roomId, RoomMember member) {
        // 퇴장한 멤버의 정보를 포함 (클라이언트에서 UI 업데이트용)
        RoomMemberResponse memberData = RoomMemberResponse.from(member);
        
        // 퇴장 알림 메시지 생성
        String message = String.format("%s님이 방에서 나갔습니다.", member.getUser().getNickname());
        
        return new RoomBroadcastMessage(BroadcastType.MEMBER_LEFT, roomId, memberData, message);
    }

    /**
     * 온라인 멤버 목록 업데이트 알림
     - 멤버 입장/퇴장 후 온라인 목록이 변경되었을 때
     - 관리자가 강제로 온라인 목록 새로고침을 요청했을 때
     * @param roomId 업데이트된 방의 ID
     * @param onlineUserIds 현재 온라인 상태인 사용자 ID 목록
     * @return 온라인 멤버 목록 업데이트 알림 메시지
     */
    public static RoomBroadcastMessage onlineMembersUpdated(Long roomId, List<Long> onlineUserIds) {
        // 현재 온라인 멤버 수 표시
        String message = String.format("현재 온라인 멤버: %d명", onlineUserIds.size());
        
        // 온라인 사용자 ID 목록을 데이터로 전송 (클라이언트에서 상세 정보 요청 가능)
        return new RoomBroadcastMessage(BroadcastType.ONLINE_MEMBERS_UPDATED, roomId, onlineUserIds, message);
    }

    /**
     * 방 설정 변경 알림 메시지
     RoomService.updateRoomSettings() 메서드에서 방 설정이 변경되었을 때
     * @param roomId 설정이 변경된 방의 ID
     * @param updateMessage 변경 내용을 설명하는 메시지
     * @return 방 설정 변경 알림 메시지
     */
    public static RoomBroadcastMessage roomUpdated(Long roomId, String updateMessage) {
        // 설정 변경의 경우 별도 데이터 없이 메시지만 전송
        return new RoomBroadcastMessage(BroadcastType.ROOM_UPDATED, roomId, null, updateMessage);
    }

    /**
     * 방장 변경 알림 메시지
     - 기존 방장이 퇴장하여 새 방장이 자동 선정되었을 때 (로직 확인 예정)
     - 방장이 다른 멤버에게 방장 권한을 이양했을 때
     * @param roomId 방장이 변경된 방의 ID
     * @param newHost 새로운 방장이 된 멤버 정보
     * @return 방장 변경 알림 메시지
     */
    public static RoomBroadcastMessage hostChanged(Long roomId, RoomMember newHost) {
        // 새 방장의 정보를 포함 (클라이언트에서 UI 권한 업데이트용)
        RoomMemberResponse hostData = RoomMemberResponse.from(newHost);
        
        // 새 방장 알림 메시지 생성
        String message = String.format("%s님이 새로운 방장이 되었습니다.", newHost.getUser().getNickname());
        
        return new RoomBroadcastMessage(BroadcastType.HOST_CHANGED, roomId, hostData, message);
    }

    /**
     * 멤버 역할 변경 알림 메시지
     RoomService.changeUserRole() 메서드에서 멤버의 역할이 변경되었을 때
     * @param roomId 역할이 변경된 방의 ID
     * @param member 역할이 변경된 멤버 정보 (변경 후 정보)
     * @return 멤버 역할 변경 알림 메시지
     */
    public static RoomBroadcastMessage memberRoleChanged(Long roomId, RoomMember member) {
        // 변경된 멤버의 새로운 역할 정보 포함
        RoomMemberResponse memberData = RoomMemberResponse.from(member);
        
        // 역할 변경 알림 메시지 (역할의 한글명 표시)
        String message = String.format("%s님의 역할이 %s로 변경되었습니다.", 
            member.getUser().getNickname(), member.getRole().getDisplayName());
        
        return new RoomBroadcastMessage(BroadcastType.MEMBER_ROLE_CHANGED, roomId, memberData, message);
    }

    /**
     * 멤버 추방 알림 메시지
     RoomService.kickMember() 메서드에서 멤버가 추방되었을 때
     * @param roomId 추방이 발생한 방의 ID
     * @param memberName 추방된 멤버의 닉네임 (추방 후에는 멤버 정보 조회 불가능하므로 미리 백업)
     * @return 멤버 추방 알림 메시지
     */
    public static RoomBroadcastMessage memberKicked(Long roomId, String memberName) {
        // 추방 알림 메시지 생성
        String message = String.format("%s님이 방에서 추방되었습니다.", memberName);
        
        // 추방의 경우 이미 멤버 정보가 제거되므로 데이터는 null
        return new RoomBroadcastMessage(BroadcastType.MEMBER_KICKED, roomId, null, message);
    }

    /**
     * 방 종료 알림 메시지
     - 방장이 수동으로 방을 종료했을 때
     - 모든 멤버가 퇴장하여 방이 자동 종료되었을 때
     * @param roomId 종료된 방의 ID
     * @return 방 종료 알림 메시지
     */
    public static RoomBroadcastMessage roomTerminated(Long roomId) {
        // 방 종료 메시지 (간단명료)
        String message = "방이 종료되었습니다.";
        
        // 방 종료의 경우 별도 데이터 없이 메시지만 전송
        return new RoomBroadcastMessage(BroadcastType.ROOM_TERMINATED, roomId, null, message);
    }

    // 브로드캐스트 메시지 타입
    public enum BroadcastType {
        // 멤버 관련 이벤트
        MEMBER_JOINED("멤버 입장"),           // 새 멤버가 방에 입장했을 때
        MEMBER_LEFT("멤버 퇴장"),             // 멤버가 방에서 퇴장했을 때
        MEMBER_ROLE_CHANGED("멤버 역할 변경"), // 멤버의 역할(권한)이 변경되었을 때
        MEMBER_KICKED("멤버 추방"),           // 멤버가 강제로 추방되었을 때
        
        // 방 상태 관련 이벤트
        ONLINE_MEMBERS_UPDATED("온라인 멤버 목록 업데이트"), // 온라인 멤버 목록이 변경되었을 때
        ROOM_UPDATED("방 설정 변경"),         // 방 제목, 설명, 정원 등이 변경되었을 때
        HOST_CHANGED("방장 변경"),            // 방장이 바뀌었을 때
        ROOM_TERMINATED("방 종료");           // 방이 종료되었을 때

        private final String description; // 한글 설명 (디버깅이나 로그용)

        BroadcastType(String description) {
            this.description = description;
        }

        /**
         * 브로드캐스트 타입의 한글 설명 반환
         * 주로 로그 출력이나 디버깅 시 사용
         */
        public String getDescription() {
            return description;
        }
    }
}
