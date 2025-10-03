package com.back.domain.studyroom.dto;

import com.back.domain.studyroom.entity.RoomRole;
import lombok.Builder;
import lombok.Getter;

/**
 * 역할 변경 응답 DTO
 */
@Getter
@Builder
public class ChangeRoleResponse {
    
    private Long userId;
    private String nickname;
    private RoomRole oldRole;
    private RoomRole newRole;
    private String message;
    
    public static ChangeRoleResponse of(Long userId, String nickname, 
                                       RoomRole oldRole, RoomRole newRole) {
        String message = buildMessage(oldRole, newRole);
        
        return ChangeRoleResponse.builder()
                .userId(userId)
                .nickname(nickname)
                .oldRole(oldRole)
                .newRole(newRole)
                .message(message)
                .build();
    }
    
    private static String buildMessage(RoomRole oldRole, RoomRole newRole) {
        if (newRole == RoomRole.HOST) {
            return "방장으로 임명되었습니다.";
        } else if (oldRole == RoomRole.HOST) {
            return "방장 권한이 해제되었습니다.";
        } else if (newRole == RoomRole.SUB_HOST) {
            return "부방장으로 승격되었습니다.";
        } else if (newRole == RoomRole.MEMBER && oldRole == RoomRole.VISITOR) {
            return "정식 멤버로 승격되었습니다.";
        } else if (newRole == RoomRole.MEMBER) {
            return "일반 멤버로 강등되었습니다.";
        }
        return "역할이 변경되었습니다.";
    }
}
