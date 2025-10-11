package com.back.domain.studyroom.controller;

import com.back.domain.studyroom.dto.AvatarResponse;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 방 아바타 관리 API 컨트롤러
 * - JWT 인증 필수
 * - MEMBER 등급 이상만 아바타 변경 가능
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
     * 사용 가능한 아바타 목록 조회
     */
    @GetMapping
    @Operation(
        summary = "아바타 목록 조회", 
        description = "선택 가능한 아바타 목록을 조회합니다. 고양이, 강아지 등 다양한 아바타를 제공합니다."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "조회 성공"),
        @ApiResponse(responseCode = "401", description = "인증 실패")
    })
    public ResponseEntity<RsData<List<AvatarResponse>>> getAvatars(
            @Parameter(description = "방 ID", required = true) @PathVariable Long roomId) {
        
        List<AvatarResponse> avatars = avatarService.getAvailableAvatars();
        
        return ResponseEntity.ok(
            RsData.success("아바타 목록 조회 완료", avatars)
        );
    }
    
    /**
     * 내 아바타 변경 (모든 사용자 가능)
     */
    @PutMapping("/me")
    @Operation(
        summary = "아바타 변경", 
        description = "방에서 사용할 아바타를 변경합니다.\n\n" +
                      "- VISITOR: Redis에만 저장 (퇴장 시 삭제, 재입장 시 랜덤 배정)\n" +
                      "- MEMBER 이상: DB에 저장 (재입장 시에도 유지)"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "변경 성공"),
        @ApiResponse(responseCode = "400", description = "잘못된 아바타 ID"),
        @ApiResponse(responseCode = "404", description = "존재하지 않는 방 또는 아바타"),
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
