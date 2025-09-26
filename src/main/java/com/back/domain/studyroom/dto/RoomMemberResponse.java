package com.back.domain.studyroom.dto;

import com.back.domain.studyroom.entity.RoomMember;
import com.back.domain.studyroom.entity.RoomRole;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class RoomMemberResponse {
    private Long userId;
    private String nickname;
    private RoomRole role;
    private boolean isOnline;
    private LocalDateTime joinedAt;
    private LocalDateTime lastActiveAt;
    
    public static RoomMemberResponse from(RoomMember member) {
        return RoomMemberResponse.builder()
                .userId(member.getUser().getId())
                .nickname(member.getUser().getNickname())
                .role(member.getRole())
                .isOnline(member.isOnline())
                .joinedAt(member.getJoinedAt())
                .lastActiveAt(member.getLastActiveAt() != null ? member.getLastActiveAt() : member.getJoinedAt())
                .build();
    }
}
