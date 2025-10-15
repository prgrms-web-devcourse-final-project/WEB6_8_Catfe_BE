package com.back.domain.studyroom.dto;

import com.back.domain.studyroom.entity.RoomMember;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 방 입장 결과 + 아바타 ID
 * 내부 전달용 DTO
 */
@Getter
@AllArgsConstructor
public class JoinRoomWithAvatarResult {
    private RoomMember member;
    private Long avatarId;
}
