package com.back.domain.studyroom.dto;

import com.back.domain.studyroom.entity.RoomMember;
import com.back.domain.studyroom.entity.RoomRole;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 방 멤버 정보 응답 DTO
 isOnline 필드 제거: 이 DTO로 변환된 멤버는 이미 온라인 상태임을 의미
 RoomService.getOnlineMembersWithWebSocket()에서 Redis 기반 필터링 후 변환
 // Redis에서 온라인 사용자만 필터링
 (Set)Long onlineUserIds = sessionManager.getOnlineUsersInRoom(roomId);

 // 해당 사용자들의 멤버 정보 조회
 (List)RoomMember onlineMembers = repository.findByRoomIdAndUserIdIn(roomId, onlineUserIds);

 // DTO 변환 (이미 온라인인 멤버들만 변환됨)
  (List)RoomMemberResponse response = onlineMembers.stream()
      .map(RoomMemberResponse::from)
      .collect(Collectors.toList());
 */
@Getter
@Builder
public class RoomMemberResponse {
    private Long userId;
    private String nickname;
    private RoomRole role;
    private LocalDateTime joinedAt;
    private LocalDateTime lastActiveAt;
    
    /**
     * RoomMember 엔티티를 응답 DTO로 변환
     주의..) 이 메서드로 변환된 멤버는 온라인 상태로 인식되기 때문에 getOnlineMembersWithWebSocket()에서만 사용 해야함!!!!
     */
    public static RoomMemberResponse from(RoomMember member) {
        return RoomMemberResponse.builder()
                .userId(member.getUser().getId())
                .nickname(member.getUser().getNickname())
                .role(member.getRole())
                .joinedAt(member.getJoinedAt())
                .lastActiveAt(member.getLastActiveAt() != null ? member.getLastActiveAt() : member.getJoinedAt())
                .build();
    }
}
