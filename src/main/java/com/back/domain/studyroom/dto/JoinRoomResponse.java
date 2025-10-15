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
    private Long avatarId;  // 부여받은 아바타 ID (숫자만)
    private LocalDateTime joinedAt;
    
    public static JoinRoomResponse of(RoomMember member, Long avatarId) {
        return JoinRoomResponse.builder()
                .roomId(member.getRoom().getId())
                .userId(member.getUser().getId())
                .role(member.getRole())
                .avatarId(avatarId)
                .joinedAt(member.getJoinedAt())
                .build();
    }
}
