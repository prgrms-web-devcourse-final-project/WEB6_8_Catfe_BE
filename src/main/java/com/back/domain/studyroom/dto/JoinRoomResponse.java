package com.back.domain.studyroom.dto;

import com.back.domain.studyroom.entity.RoomMember;
import com.back.domain.studyroom.entity.RoomRole;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class JoinRoomResponse {
    private Long roomId;
    private Long userId;
    private RoomRole role;
    private LocalDateTime joinedAt;
    
    public static JoinRoomResponse from(RoomMember member) {
        return JoinRoomResponse.builder()
                .roomId(member.getRoom().getId())
                .userId(member.getUser().getId())
                .role(member.getRole())
                .joinedAt(member.getJoinedAt())
                .build();
    }
}
