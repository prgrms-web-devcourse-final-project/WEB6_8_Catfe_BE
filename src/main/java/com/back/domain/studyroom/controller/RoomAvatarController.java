package com.back.domain.studyroom.controller;

import com.back.domain.studyroom.dto.UpdateAvatarRequest;
import com.back.domain.studyroom.service.AvatarService;
import com.back.global.common.dto.RsData;
import com.back.global.security.user.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 방 아바타 관리 API 컨트롤러
 * - JWT 인증 필수
 * - 아바타는 숫자(ID)로만 관리
 * - 프론트엔드에서 숫자에 맞는 이미지 매핑
 */
@RestController
@RequestMapping("/api/rooms/{roomId}/avatars")
@RequiredArgsConstructor
@Tag(name = "Room Avatar API", description = "방 아바타 관련 API")
@SecurityRequirement(name = "Bearer Authentication")
public class RoomAvatarController {
    
    private final AvatarService avatarService;
    private final CurrentUser currentUser;
    
    /**
     * 내 아바타 변경 (모든 사용자 가능)
     */
    @PutMapping("/me")
    @Operation(
        summary = "아바타 변경", 
        description = "방에서 사용할 아바타를 변경합니다.\n\n" +
                      "- 프론트에서 원하는 숫자(avatarId)를 전송하면 그대로 저장됩니다.\n" +
                      "- DB 검증 없이 Redis에만 저장됩니다.\n" +
                      "- 모든 사용자(VISITOR 포함) 변경 가능합니다."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "변경 성공"),
        @ApiResponse(responseCode = "401", description = "인증 실패")
    })
    public ResponseEntity<RsData<Void>> updateMyAvatar(
            @Parameter(description = "방 ID", required = true) @PathVariable Long roomId,
            @Valid @RequestBody UpdateAvatarRequest request) {
        
        Long userId = currentUser.getUserId();
        
        avatarService.updateRoomAvatar(roomId, userId, request.getAvatarId());
        
        return ResponseEntity.ok(
            RsData.success("아바타가 변경되었습니다", null)
        );
    }
}
