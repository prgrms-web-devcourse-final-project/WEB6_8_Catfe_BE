package com.back.domain.studyroom.controller;

import com.back.domain.studyroom.dto.InviteCodeResponse;
import com.back.domain.studyroom.entity.RoomInviteCode;
import com.back.domain.studyroom.service.RoomInviteService;
import com.back.global.common.dto.RsData;
import com.back.global.security.user.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 방 초대 코드 API 컨트롤러
 * - VISITOR 포함 모든 사용자가 초대 코드 발급 가능
 * - 사용자당 1개의 고유 코드 보유
 * - 3시간 유효, 만료 후 재생성 가능
 */
@RestController
@RequestMapping("/api/rooms/{roomId}/invite")
@RequiredArgsConstructor
@Tag(name = "Room Invite API", description = "방 초대 코드 관련 API")
@SecurityRequirement(name = "Bearer Authentication")
public class RoomInviteController {

    private final RoomInviteService inviteService;
    private final CurrentUser currentUser;

    @GetMapping("/me")
    @Operation(
        summary = "내 초대 코드 조회/생성", 
        description = "내 초대 코드를 조회합니다. 없으면 자동으로 생성됩니다. " +
                      "유효기간은 3시간이며, 만료 전까지는 같은 코드가 유지됩니다. " +
                      "만료된 경우 새로 생성할 수 있습니다."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "조회/생성 성공"),
        @ApiResponse(responseCode = "404", description = "존재하지 않는 방"),
        @ApiResponse(responseCode = "401", description = "인증 실패")
    })
    public ResponseEntity<RsData<InviteCodeResponse>> getMyInviteCode(
            @Parameter(description = "방 ID", required = true) @PathVariable Long roomId) {

        Long userId = currentUser.getUserId();

        RoomInviteCode code = inviteService.getOrCreateMyInviteCode(roomId, userId);
        InviteCodeResponse response = InviteCodeResponse.from(code);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(RsData.success("초대 코드 조회 완료", response));
    }
}
