package com.back.domain.studyroom.dto;

import com.back.domain.studyroom.entity.RoomRole;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 멤버 역할 변경 요청 DTO
 * - VISITOR → MEMBER/SUB_HOST/HOST 모두 가능
 * - HOST로 변경 시 기존 방장은 자동으로 MEMBER로 강등
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ChangeRoleRequest {
    
    @NotNull(message = "역할은 필수입니다")
    private RoomRole newRole;
}
