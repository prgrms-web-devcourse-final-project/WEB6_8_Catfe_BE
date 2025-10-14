package com.back.domain.studyroom.controller;

import com.back.domain.studyroom.dto.JoinRoomResponse;
import com.back.domain.studyroom.entity.Room;
import com.back.domain.studyroom.entity.RoomMember;
import com.back.domain.studyroom.service.RoomInviteService;
import com.back.domain.studyroom.service.RoomService;
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
 * 초대 코드 입장 API
 * - 비로그인 시 401 반환 (프론트에서 로그인 페이지로 리다이렉트)
 */
@RestController
@RequestMapping("/api/invite")
@RequiredArgsConstructor
@Tag(name = "Room Invite API", description = "초대 코드로 방 입장 API")
public class RoomInvitePublicController {

    private final RoomInviteService inviteService;
    private final RoomService roomService;
    private final CurrentUser currentUser;

    @PostMapping("/{inviteCode}")
    @SecurityRequirement(name = "Bearer Authentication")
    @Operation(
        summary = "초대 코드로 방 입장", 
        description = "초대 코드를 사용하여 방 입장 권한을 획득합니다. " +
                      "비밀번호가 걸린 방도 초대 코드로 입장 가능합니다. " +
                      "실제 온라인 등록은 WebSocket 연결 시 자동으로 처리됩니다. " +
                      "비로그인 사용자는 401 응답을 받습니다 (프론트에서 로그인 페이지로 이동)."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "입장 권한 획득 성공 (WebSocket 연결 필요)"),
        @ApiResponse(responseCode = "400", description = "만료되었거나 유효하지 않은 코드"),
        @ApiResponse(responseCode = "404", description = "존재하지 않는 초대 코드"),
        @ApiResponse(responseCode = "401", description = "인증 필요 (비로그인)")
    })
    public ResponseEntity<RsData<JoinRoomResponse>> joinByInviteCode(
            @Parameter(description = "초대 코드", required = true, example = "A3B9C2D1") 
            @PathVariable String inviteCode) {

        // 로그인 체크는 Spring Security에서 자동 처리 (비로그인 시 401)
        Long userId = currentUser.getUserId();

        // 초대 코드 검증 및 방 조회
        Room room = inviteService.getRoomByInviteCode(inviteCode);

        // 방 입장 권한 체크 (비밀번호 무시, Redis 등록 건너뜀)
        RoomMember member = roomService.joinRoom(room.getId(), null, userId, false);
        JoinRoomResponse response = JoinRoomResponse.from(member);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(RsData.success("초대 코드로 입장 권한 획득 완료. WebSocket 연결 후 실제 입장됩니다.", response));
    }
}
