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
    private LocalDateTime joinedAt;
    private LocalDateTime promotedAt;
    
    // TODO: isOnline은 Redis에서 조회하여 추가 예정
    
    public static RoomMemberResponse from(RoomMember member) {
        return RoomMemberResponse.builder()
                .userId(member.getUser().getId())
                .nickname(member.getUser().getNickname())
                .role(member.getRole())
                .joinedAt(member.getJoinedAt())
                .promotedAt(member.getPromotedAt())
                .build();
    }
}
