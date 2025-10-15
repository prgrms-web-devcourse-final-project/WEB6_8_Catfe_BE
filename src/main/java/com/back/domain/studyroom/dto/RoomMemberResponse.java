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
    private String profileImageUrl;
    private Long avatarId;  // 아바타 ID (숫자만, 프론트에서 매핑)
    private RoomRole role;
    private LocalDateTime joinedAt;
    private LocalDateTime promotedAt;

    
    /**
     * RoomMember만으로 생성 (아바타 정보 없음)
     * 기존 호환성과 간단한 조회용
     */
    public static RoomMemberResponse from(RoomMember member) {
        return RoomMemberResponse.builder()
                .userId(member.getUser().getId())
                .nickname(member.getUser().getNickname())
                .profileImageUrl(member.getUser().getProfileImageUrl())
                .role(member.getRole())
                .joinedAt(member.getJoinedAt())
                .promotedAt(member.getPromotedAt())
                .build();
    }
    
    /**
     * 아바타 ID 포함하여 생성
     */
    public static RoomMemberResponse of(RoomMember member, Long avatarId) {
        return RoomMemberResponse.builder()
                .userId(member.getUser().getId())
                .nickname(member.getUser().getNickname())
                .profileImageUrl(member.getUser().getProfileImageUrl())
                .avatarId(avatarId)
                .role(member.getRole())
                .joinedAt(member.getJoinedAt())
                .promotedAt(member.getPromotedAt())
                .build();
    }
}
