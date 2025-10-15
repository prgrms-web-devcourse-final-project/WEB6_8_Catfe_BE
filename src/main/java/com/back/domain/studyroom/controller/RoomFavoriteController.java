package com.back.domain.studyroom.controller;

import com.back.domain.studyroom.dto.RoomFavoriteResponse;
import com.back.domain.studyroom.service.RoomFavoriteService;
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

import java.util.List;

/**
 * 방 즐겨찾기 API 컨트롤러
 */
@RestController
@RequestMapping("/api/rooms")
@RequiredArgsConstructor
@Tag(name = "Room Favorite API", description = "방 즐겨찾기 관련 API")
@SecurityRequirement(name = "Bearer Authentication")
public class RoomFavoriteController {
    
    private final RoomFavoriteService favoriteService;
    private final CurrentUser currentUser;
    
    @PostMapping("/{roomId}/favorite")
    @Operation(
        summary = "즐겨찾기 추가",
        description = "특정 방을 즐겨찾기에 추가합니다. 이미 추가된 경우 무시됩니다."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "즐겨찾기 추가 성공"),
        @ApiResponse(responseCode = "404", description = "존재하지 않는 방"),
        @ApiResponse(responseCode = "401", description = "인증 실패")
    })
    public ResponseEntity<RsData<Void>> addFavorite(
            @Parameter(description = "방 ID", required = true) @PathVariable Long roomId) {
        
        Long userId = currentUser.getUserId();
        favoriteService.addFavorite(roomId, userId);
        
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(RsData.success("즐겨찾기 추가 완료", null));
    }
    
    @DeleteMapping("/{roomId}/favorite")
    @Operation(
        summary = "즐겨찾기 제거",
        description = "특정 방을 즐겨찾기에서 제거합니다."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "즐겨찾기 제거 성공"),
        @ApiResponse(responseCode = "404", description = "즐겨찾기되지 않은 방"),
        @ApiResponse(responseCode = "401", description = "인증 실패")
    })
    public ResponseEntity<RsData<Void>> removeFavorite(
            @Parameter(description = "방 ID", required = true) @PathVariable Long roomId) {
        
        Long userId = currentUser.getUserId();
        favoriteService.removeFavorite(roomId, userId);
        
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(RsData.success("즐겨찾기 제거 완료", null));
    }
    
    @GetMapping("/favorites")
    @Operation(
        summary = "내 즐겨찾기 목록 조회",
        description = "내가 즐겨찾기한 모든 방 목록을 최신 즐겨찾기 순으로 조회합니다."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "조회 성공"),
        @ApiResponse(responseCode = "401", description = "인증 실패")
    })
    public ResponseEntity<RsData<List<RoomFavoriteResponse>>> getMyFavorites() {
        
        Long userId = currentUser.getUserId();
        List<RoomFavoriteResponse> favorites = favoriteService.getMyFavorites(userId);
        
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(RsData.success("즐겨찾기 목록 조회 완료", favorites));
    }
    
    @GetMapping("/{roomId}/favorite")
    @Operation(
        summary = "즐겨찾기 여부 확인",
        description = "특정 방이 즐겨찾기되어 있는지 확인합니다."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "조회 성공"),
        @ApiResponse(responseCode = "401", description = "인증 실패")
    })
    public ResponseEntity<RsData<Boolean>> isFavorite(
            @Parameter(description = "방 ID", required = true) @PathVariable Long roomId) {
        
        Long userId = currentUser.getUserId();
        boolean isFavorite = favoriteService.isFavorite(roomId, userId);
        
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(RsData.success("즐겨찾기 여부 조회 완료", isFavorite));
    }
}
