package com.back.domain.studyroom.dto;

import com.back.domain.studyroom.entity.RoomInviteCode;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
@Schema(description = "초대 코드 응답")
public class InviteCodeResponse {

    @Schema(description = "초대 코드", example = "A3B9C2D1")
    private String inviteCode;

    @Schema(description = "초대 링크", example = "https://catfe.com/invite/A3B9C2D1")
    private String inviteLink;

    @Schema(description = "방 ID", example = "1")
    private Long roomId;

    @Schema(description = "방 제목", example = "스터디 모임")
    private String roomTitle;

    @Schema(description = "생성자 닉네임", example = "홍길동")
    private String createdByNickname;

    @Schema(description = "만료 시간")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime expiresAt;

    @Schema(description = "활성 여부", example = "true")
    private boolean isActive;

    @Schema(description = "유효 여부 (만료되지 않았는지)", example = "true")
    private boolean isValid;

    public static InviteCodeResponse from(RoomInviteCode code) {
        return InviteCodeResponse.builder()
                .inviteCode(code.getInviteCode())
                .inviteLink("https://catfe.com/invite/" + code.getInviteCode())
                .roomId(code.getRoom().getId())
                .roomTitle(code.getRoom().getTitle())
                .createdByNickname(code.getCreatedBy().getNickname())
                .expiresAt(code.getExpiresAt())
                .isActive(code.isActive())
                .isValid(code.isValid())  // 만료 여부 체크
                .build();
    }
}
