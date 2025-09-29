package com.back.domain.studyroom.dto;

import com.back.domain.studyroom.entity.RoomMember;
import com.back.domain.studyroom.entity.RoomRole;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Getter
@Builder
public class JoinRoomResponse {
    private Long roomId;
    private Long userId;
    private RoomRole role;
    private LocalDateTime joinedAt;
    
    // 🆕 WebSocket 관련 정보
    private int currentOnlineCount;
    private List<RoomMemberResponse> onlineMembers;
    private WebSocketChannelInfo websocketInfo;
    
    public static JoinRoomResponse from(RoomMember member) {
        return JoinRoomResponse.builder()
                .roomId(member.getRoom().getId())
                .userId(member.getUser().getId())
                .role(member.getRole())
                .joinedAt(member.getJoinedAt())
                .build();
    }
    
    /**
     * 🆕 WebSocket 정보를 포함한 응답 생성
     */
    public static JoinRoomResponse withWebSocketInfo(RoomMember member, 
                                                     List<RoomMemberResponse> onlineMembers,
                                                     int onlineCount) {
        return JoinRoomResponse.builder()
                .roomId(member.getRoom().getId())
                .userId(member.getUser().getId())
                .role(member.getRole())
                .joinedAt(member.getJoinedAt())
                .currentOnlineCount(onlineCount)
                .onlineMembers(onlineMembers)
                .websocketInfo(WebSocketChannelInfo.forRoom(member.getRoom().getId()))
                .build();
    }
    
    /**
     * WebSocket 채널 정보
     */
    @Getter
    @Builder
    public static class WebSocketChannelInfo {
        private String roomUpdatesChannel;
        private String roomChatChannel;
        private String privateMessageChannel;
        private Map<String, String> subscribeTopics;
        
        public static WebSocketChannelInfo forRoom(Long roomId) {
            return WebSocketChannelInfo.builder()
                    .roomUpdatesChannel("/topic/rooms/" + roomId + "/updates")
                    .roomChatChannel("/topic/rooms/" + roomId + "/chat")
                    .privateMessageChannel("/user/queue/messages")
                    .subscribeTopics(Map.of(
                        "roomUpdates", "/topic/rooms/" + roomId + "/updates",
                        "roomChat", "/topic/rooms/" + roomId + "/chat",
                        "privateMessages", "/user/queue/messages",
                        "notifications", "/user/queue/notifications"
                    ))
                    .build();
        }
    }
}
