package com.back.domain.studyroom.controller;

import com.back.domain.studyroom.dto.CreateAnnouncementRequest;
import com.back.domain.studyroom.dto.RoomAnnouncementResponse;
import com.back.domain.studyroom.dto.UpdateAnnouncementRequest;
import com.back.domain.studyroom.service.RoomAnnouncementService;
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
 * 방 공지사항 API 컨트롤러
 */
@RestController
@RequestMapping("/api/rooms/{roomId}/announcements")
@RequiredArgsConstructor
@Tag(name = "Room Announcement API", description = "방 공지사항 관련 API")
@SecurityRequirement(name = "Bearer Authentication")
public class RoomAnnouncementController {
    
    private final RoomAnnouncementService announcementService;
    private final CurrentUser currentUser;
    
    @PostMapping
    @Operation(
        summary = "공지사항 생성",
        description = "새로운 공지사항을 생성합니다. 방장만 생성 가능합니다."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "공지사항 생성 성공"),
        @ApiResponse(responseCode = "403", description = "방장 권한 없음"),
        @ApiResponse(responseCode = "404", description = "존재하지 않는 방"),
        @ApiResponse(responseCode = "401", description = "인증 실패")
    })
    public ResponseEntity<RsData<RoomAnnouncementResponse>> createAnnouncement(
            @Parameter(description = "방 ID", required = true) @PathVariable Long roomId,
            @Valid @RequestBody CreateAnnouncementRequest request) {
        
        Long userId = currentUser.getUserId();
        RoomAnnouncementResponse response = announcementService.createAnnouncement(
                roomId, request.getTitle(), request.getContent(), userId);
        
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(RsData.success("공지사항 생성 완료", response));
    }
    
    @GetMapping
    @Operation(
        summary = "공지사항 목록 조회",
        description = "방의 모든 공지사항을 조회합니다. 핀 고정된 공지가 먼저 표시되고, 그 다음 최신순으로 정렬됩니다."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "조회 성공"),
        @ApiResponse(responseCode = "404", description = "존재하지 않는 방")
    })
    public ResponseEntity<RsData<List<RoomAnnouncementResponse>>> getAnnouncements(
            @Parameter(description = "방 ID", required = true) @PathVariable Long roomId) {
        
        List<RoomAnnouncementResponse> announcements = announcementService.getAnnouncements(roomId);
        
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(RsData.success("공지사항 목록 조회 완료", announcements));
    }
    
    @GetMapping("/{announcementId}")
    @Operation(
        summary = "공지사항 단건 조회",
        description = "특정 공지사항의 상세 정보를 조회합니다."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "조회 성공"),
        @ApiResponse(responseCode = "404", description = "존재하지 않는 공지사항")
    })
    public ResponseEntity<RsData<RoomAnnouncementResponse>> getAnnouncement(
            @Parameter(description = "방 ID", required = true) @PathVariable Long roomId,
            @Parameter(description = "공지사항 ID", required = true) @PathVariable Long announcementId) {
        
        RoomAnnouncementResponse announcement = announcementService.getAnnouncement(announcementId);
        
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(RsData.success("공지사항 조회 완료", announcement));
    }
    
    @PutMapping("/{announcementId}")
    @Operation(
        summary = "공지사항 수정",
        description = "공지사항의 제목과 내용을 수정합니다. 방장만 수정 가능합니다."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "수정 성공"),
        @ApiResponse(responseCode = "403", description = "방장 권한 없음"),
        @ApiResponse(responseCode = "404", description = "존재하지 않는 공지사항"),
        @ApiResponse(responseCode = "401", description = "인증 실패")
    })
    public ResponseEntity<RsData<RoomAnnouncementResponse>> updateAnnouncement(
            @Parameter(description = "방 ID", required = true) @PathVariable Long roomId,
            @Parameter(description = "공지사항 ID", required = true) @PathVariable Long announcementId,
            @Valid @RequestBody UpdateAnnouncementRequest request) {
        
        Long userId = currentUser.getUserId();
        RoomAnnouncementResponse response = announcementService.updateAnnouncement(
                announcementId, request.getTitle(), request.getContent(), userId);
        
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(RsData.success("공지사항 수정 완료", response));
    }
    
    @DeleteMapping("/{announcementId}")
    @Operation(
        summary = "공지사항 삭제",
        description = "공지사항을 삭제합니다. 방장만 삭제 가능합니다."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "삭제 성공"),
        @ApiResponse(responseCode = "403", description = "방장 권한 없음"),
        @ApiResponse(responseCode = "404", description = "존재하지 않는 공지사항"),
        @ApiResponse(responseCode = "401", description = "인증 실패")
    })
    public ResponseEntity<RsData<Void>> deleteAnnouncement(
            @Parameter(description = "방 ID", required = true) @PathVariable Long roomId,
            @Parameter(description = "공지사항 ID", required = true) @PathVariable Long announcementId) {
        
        Long userId = currentUser.getUserId();
        announcementService.deleteAnnouncement(announcementId, userId);
        
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(RsData.success("공지사항 삭제 완료", null));
    }
    
    @PutMapping("/{announcementId}/pin")
    @Operation(
        summary = "공지사항 핀 고정/해제",
        description = "공지사항을 상단에 고정하거나 고정을 해제합니다. 방장만 실행 가능합니다."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "핀 토글 성공"),
        @ApiResponse(responseCode = "403", description = "방장 권한 없음"),
        @ApiResponse(responseCode = "404", description = "존재하지 않는 공지사항"),
        @ApiResponse(responseCode = "401", description = "인증 실패")
    })
    public ResponseEntity<RsData<RoomAnnouncementResponse>> togglePin(
            @Parameter(description = "방 ID", required = true) @PathVariable Long roomId,
            @Parameter(description = "공지사항 ID", required = true) @PathVariable Long announcementId) {
        
        Long userId = currentUser.getUserId();
        RoomAnnouncementResponse response = announcementService.togglePin(announcementId, userId);
        
        String message = response.getIsPinned() ? "공지사항 핀 고정 완료" : "공지사항 핀 고정 해제 완료";
        
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(RsData.success(message, response));
    }
}
