package com.back.domain.studyroom.controller;

import com.back.domain.studyroom.dto.*;
import com.back.domain.studyroom.service.RoomGuestbookService;
import com.back.global.common.dto.RsData;
import com.back.global.security.user.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 방명록 관리 Controller
 * - 방명록 CRUD
 * - 이모지 반응 추가/제거
 * - 개인별 핀 기능
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/room/{roomId}/guestbook")
@Tag(name = "Room Guestbook", description = "방명록 관리 API - 방명록 작성, 조회, 수정, 삭제 및 이모지 반응, 개인 핀 기능")
public class RoomGuestbookController {

    private final RoomGuestbookService guestbookService;
    private final CurrentUser currentUser;

    /**
     * 방명록 목록 조회 (페이징)
     * 
     * @param roomId 방 ID
     * @param page 페이지 번호 (0부터 시작)
     * @param size 페이지 크기
     * @return 방명록 목록
     */
    @GetMapping
    @Operation(
        summary = "방명록 목록 조회",
        description = "특정 방의 방명록 목록을 조회합니다. 로그인한 사용자가 핀한 방명록이 최상단에 표시됩니다. 페이징을 지원하며, 각 방명록의 이모지 반응과 핀 상태가 포함됩니다."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "방명록 목록 조회 성공"),
        @ApiResponse(responseCode = "404", description = "존재하지 않는 방")
    })
    public ResponseEntity<RsData<Map<String, Object>>> getGuestbooks(
            @PathVariable Long roomId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Long currentUserId = currentUser.getUserIdOrNull();
        Pageable pageable = PageRequest.of(page, size);

        Page<GuestbookResponse> guestbooks = guestbookService.getGuestbooks(roomId, currentUserId, pageable);

        Map<String, Object> response = new HashMap<>();
        response.put("guestbooks", guestbooks.getContent());
        response.put("totalPages", guestbooks.getTotalPages());
        response.put("totalElements", guestbooks.getTotalElements());
        response.put("currentPage", guestbooks.getNumber());
        response.put("pageSize", guestbooks.getSize());

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(RsData.success("방명록 목록 조회 성공", response));
    }

    /**
     * 방명록 단건 조회
     * 
     * @param roomId 방 ID
     * @param guestbookId 방명록 ID
     * @return 방명록 상세 정보
     */
    @GetMapping("/{guestbookId}")
    @Operation(
        summary = "방명록 단건 조회",
        description = "특정 방명록의 상세 정보를 조회합니다. 작성자 정보, 내용, 이모지 반응, 핀 상태 등이 포함됩니다."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "방명록 조회 성공"),
        @ApiResponse(responseCode = "404", description = "존재하지 않는 방명록")
    })
    public ResponseEntity<RsData<GuestbookResponse>> getGuestbook(
            @PathVariable Long roomId,
            @PathVariable Long guestbookId) {

        Long currentUserId = currentUser.getUserIdOrNull();
        GuestbookResponse guestbook = guestbookService.getGuestbook(guestbookId, currentUserId);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(RsData.success("방명록 조회 성공", guestbook));
    }

    /**
     * 방명록 작성
     * 
     * @param roomId 방 ID
     * @param request 방명록 내용
     * @return 생성된 방명록
     */
    @PostMapping
    @Operation(
        summary = "방명록 작성",
        description = "특정 방에 방명록을 작성합니다. 방을 방문한 사용자가 메시지를 남길 수 있으며, 최대 500자까지 작성 가능합니다."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "방명록 작성 성공"),
        @ApiResponse(responseCode = "400", description = "잘못된 요청 (내용 누락 또는 500자 초과)"),
        @ApiResponse(responseCode = "404", description = "존재하지 않는 방"),
        @ApiResponse(responseCode = "401", description = "인증 실패")
    })
    public ResponseEntity<RsData<GuestbookResponse>> createGuestbook(
            @PathVariable Long roomId,
            @RequestBody @Valid CreateGuestbookRequest request) {

        Long userId = currentUser.getUserId();
        GuestbookResponse guestbook = guestbookService.createGuestbook(roomId, request.getContent(), userId);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(RsData.success("방명록 작성 성공", guestbook));
    }

    /**
     * 방명록 수정 (작성자만 가능)
     * 
     * @param roomId 방 ID
     * @param guestbookId 방명록 ID
     * @param request 수정할 내용
     * @return 수정된 방명록
     */
    @PutMapping("/{guestbookId}")
    @Operation(
        summary = "방명록 수정",
        description = "작성한 방명록의 내용을 수정합니다. 작성자 본인만 수정할 수 있습니다."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "방명록 수정 성공"),
        @ApiResponse(responseCode = "400", description = "잘못된 요청 (내용 누락 또는 500자 초과)"),
        @ApiResponse(responseCode = "403", description = "권한 없음 (작성자가 아님)"),
        @ApiResponse(responseCode = "404", description = "존재하지 않는 방명록"),
        @ApiResponse(responseCode = "401", description = "인증 실패")
    })
    public ResponseEntity<RsData<GuestbookResponse>> updateGuestbook(
            @PathVariable Long roomId,
            @PathVariable Long guestbookId,
            @RequestBody @Valid UpdateGuestbookRequest request) {

        Long userId = currentUser.getUserId();
        GuestbookResponse guestbook = guestbookService.updateGuestbook(guestbookId, request.getContent(), userId);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(RsData.success("방명록 수정 성공", guestbook));
    }

    /**
     * 방명록 삭제 (작성자만 가능)
     * 
     * @param roomId 방 ID
     * @param guestbookId 방명록 ID
     * @return 성공 메시지
     */
    @DeleteMapping("/{guestbookId}")
    @Operation(
        summary = "방명록 삭제",
        description = "작성한 방명록을 삭제합니다. 작성자 본인만 삭제할 수 있으며, 삭제 시 관련된 이모지 반응과 핀도 함께 삭제됩니다."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "방명록 삭제 성공"),
        @ApiResponse(responseCode = "403", description = "권한 없음 (작성자가 아님)"),
        @ApiResponse(responseCode = "404", description = "존재하지 않는 방명록"),
        @ApiResponse(responseCode = "401", description = "인증 실패")
    })
    public ResponseEntity<RsData<Void>> deleteGuestbook(
            @PathVariable Long roomId,
            @PathVariable Long guestbookId) {

        Long userId = currentUser.getUserId();
        guestbookService.deleteGuestbook(guestbookId, userId);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(RsData.success("방명록 삭제 성공"));
    }

    /**
     * 방명록 이모지 반응 추가/제거 (토글)
     * - 이미 반응한 이모지면 제거
     * - 반응하지 않은 이모지면 추가
     * 
     * @param roomId 방 ID
     * @param guestbookId 방명록 ID
     * @param request 이모지
     * @return 업데이트된 방명록 (반응 포함)
     */
    @PostMapping("/{guestbookId}/reaction")
    @Operation(
        summary = "이모지 반응 토글",
        description = "방명록에 이모지 반응을 추가하거나 제거합니다. 이미 해당 이모지로 반응한 경우 제거되고, 반응하지 않은 경우 추가됩니다. 한 사용자는 같은 이모지로 중복 반응할 수 없지만, 여러 종류의 이모지로 반응할 수 있습니다."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "이모지 반응 토글 성공"),
        @ApiResponse(responseCode = "400", description = "잘못된 요청 (이모지 형식 오류)"),
        @ApiResponse(responseCode = "404", description = "존재하지 않는 방명록"),
        @ApiResponse(responseCode = "401", description = "인증 실패")
    })
    public ResponseEntity<RsData<GuestbookResponse>> toggleReaction(
            @PathVariable Long roomId,
            @PathVariable Long guestbookId,
            @RequestBody @Valid AddGuestbookReactionRequest request) {

        Long userId = currentUser.getUserId();
        GuestbookResponse guestbook = guestbookService.toggleReaction(guestbookId, request.getEmoji(), userId);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(RsData.success("이모지 반응 토글 성공", guestbook));
    }

    /**
     * 방명록 핀 추가/제거 (토글)
     * - 이미 핀한 방명록이면 제거
     * - 핀하지 않은 방명록이면 추가
     * 
     * @param roomId 방 ID
     * @param guestbookId 방명록 ID
     * @return 업데이트된 방명록 (핀 상태 포함)
     */
    @PostMapping("/{guestbookId}/pin")
    @Operation(
        summary = "방명록 개인 핀 토글",
        description = "방명록을 개인 핀에 추가하거나 제거합니다. 핀한 방명록은 목록 조회 시 최상단에 표시됩니다. 각 사용자는 자신만의 핀 목록을 가지며, 다른 사용자에게는 영향을 주지 않습니다. (공지사항 핀과 다름)"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "방명록 핀 토글 성공"),
        @ApiResponse(responseCode = "404", description = "존재하지 않는 방명록"),
        @ApiResponse(responseCode = "401", description = "인증 실패")
    })
    public ResponseEntity<RsData<GuestbookResponse>> togglePin(
            @PathVariable Long roomId,
            @PathVariable Long guestbookId) {

        Long userId = currentUser.getUserId();
        GuestbookResponse guestbook = guestbookService.togglePin(guestbookId, userId);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(RsData.success("방명록 핀 토글 성공", guestbook));
    }
}
