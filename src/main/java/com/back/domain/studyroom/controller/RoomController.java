package com.back.domain.studyroom.controller;

import com.back.domain.studyroom.dto.*;
import com.back.domain.studyroom.entity.Room;
import com.back.domain.studyroom.entity.RoomMember;
import com.back.domain.studyroom.entity.RoomRole;
import com.back.domain.studyroom.service.RoomService;
import com.back.global.common.dto.RsData;
import com.back.global.exception.CustomException;
import com.back.global.exception.ErrorCode;
import com.back.global.security.user.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 스터디 룸 관련 API 컨트롤러
 * - JWT 인증 필수 (Spring Security + CurrentUser)
 * - Swagger에서 테스트 시 "Authorize" 버튼으로 토큰 입력
 */
@RestController
@RequestMapping("/api/rooms")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Room API", description = "스터디 룸 관련 API")
@SecurityRequirement(name = "Bearer Authentication")
public class RoomController {
    private final RoomService roomService;
    private final CurrentUser currentUser;
    
    @PostMapping
    @Operation(
        summary = "방 생성", 
        description = "새로운 스터디 룸을 생성합니다. 방 생성자는 자동으로 방장(HOST)이 됩니다."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "방 생성 성공"),
        @ApiResponse(responseCode = "400", description = "잘못된 요청 데이터"),
        @ApiResponse(responseCode = "401", description = "인증 실패")
    })
    public ResponseEntity<RsData<RoomResponse>> createRoom(
            @Valid @RequestBody CreateRoomRequest request) {

        Long currentUserId = currentUser.getUserId();

        Room room = roomService.createRoom(
                request.getTitle(),
                request.getDescription(),
                request.getIsPrivate() != null ? request.getIsPrivate() : false,
                request.getPassword(),
                request.getMaxParticipants() != null ? request.getMaxParticipants() : 10,
                currentUserId
        );
        
        RoomResponse response = RoomResponse.from(room);
        
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(RsData.success("방 생성 완료", response));
    }

    @PostMapping("/{roomId}/join")
    @Operation(
        summary = "방 입장", 
        description = "특정 스터디 룸에 입장합니다." +
                " 공개방은 바로 입장 가능하며, 비공개방은 비밀번호가 필요합니다." +
                " 입장 후 WebSocket 연결 정보와 현재 온라인 멤버 목록을 함께 제공합니다."

    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "방 입장 성공"),
        @ApiResponse(responseCode = "400", description = "방이 가득 찼거나 비밀번호가 틀림"),
        @ApiResponse(responseCode = "404", description = "존재하지 않는 방"),
        @ApiResponse(responseCode = "401", description = "인증 실패")
    })
    public ResponseEntity<RsData<JoinRoomResponse>> joinRoom(
            @Parameter(description = "방 ID", required = true) @PathVariable Long roomId,
            @RequestBody(required = false) JoinRoomRequest request) {

        Long currentUserId = currentUser.getUserId();

        String password = null;
        if (request != null) {
            password = request.getPassword();
        }

        RoomMember member = roomService.joinRoom(roomId, password, currentUserId);
        
        // 🆕 WebSocket 기반 온라인 멤버 목록과 WebSocket 연결 정보 포함하여 응답 생성
        try {
            List<RoomMemberResponse> onlineMembers = roomService.getOnlineMembersWithWebSocket(roomId, currentUserId);
            int onlineCount = onlineMembers.size();
            
            JoinRoomResponse response = JoinRoomResponse.withWebSocketInfo(member, onlineMembers, onlineCount);
            
            return ResponseEntity
                    .status(HttpStatus.OK)
                    .body(RsData.success("방 입장 완료", response));
                    
        } catch (Exception e) {
            log.warn("WebSocket 정보 포함 응답 생성 실패, 기본 응답 사용 - 방: {}, 사용자: {}", roomId, currentUserId, e);
            
            // WebSocket 연동 실패 시 기본 응답으로 폴백
            JoinRoomResponse response = JoinRoomResponse.from(member);
            
            return ResponseEntity
                    .status(HttpStatus.OK)
                    .body(RsData.success("방 입장 완료", response));
        }
    }

    @PostMapping("/{roomId}/leave")
    @Operation(
        summary = "방 나가기", 
        description = "특정 스터디 룸에서 퇴장합니다."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "방 퇴장 성공"),
        @ApiResponse(responseCode = "404", description = "존재하지 않는 방 또는 멤버가 아님"),
        @ApiResponse(responseCode = "401", description = "인증 실패")
    })
    public ResponseEntity<RsData<Void>> leaveRoom(
            @Parameter(description = "방 ID", required = true) @PathVariable Long roomId) {

        Long currentUserId = currentUser.getUserId();

        roomService.leaveRoom(roomId, currentUserId);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(RsData.success("방 퇴장 완료", null));
    }

    @GetMapping
    @Operation(
        summary = "공개 방 목록 조회", 
        description = "입장 가능한 공개 스터디 룸 목록을 페이징하여 조회합니다. 최신 생성 순으로 정렬됩니다."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "조회 성공"),
        @ApiResponse(responseCode = "401", description = "인증 실패")
    })
    public ResponseEntity<RsData<Map<String, Object>>> getRooms(
            @Parameter(description = "페이지 번호 (0부터 시작)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지 크기") @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size);
        Page<Room> rooms = roomService.getJoinableRooms(pageable);

        List<RoomResponse> roomList = rooms.getContent().stream()
                .map(RoomResponse::from)
                .collect(Collectors.toList());

        Map<String, Object> response = new HashMap<>();
        response.put("rooms", roomList);
        response.put("page", rooms.getNumber());
        response.put("size", rooms.getSize());
        response.put("totalElements", rooms.getTotalElements());
        response.put("totalPages", rooms.getTotalPages());
        response.put("hasNext", rooms.hasNext());

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(RsData.success("방 목록 조회 완료", response));
    }

    @GetMapping("/{roomId}")
    @Operation(
        summary = "방 상세 정보 조회", 
        description = "특정 방의 상세 정보와 현재 온라인 멤버 목록을 조회합니다. 비공개 방은 멤버만 조회 가능하며, WebSocket 기반 실시간 온라인 상태를 반영합니다."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "조회 성공"),
        @ApiResponse(responseCode = "403", description = "비공개 방에 대한 접근 권한 없음"),
        @ApiResponse(responseCode = "404", description = "존재하지 않는 방"),
        @ApiResponse(responseCode = "401", description = "인증 실패")
    })
    public ResponseEntity<RsData<RoomDetailResponse>> getRoomDetail(
            @Parameter(description = "방 ID", required = true) @PathVariable Long roomId) {

        Long currentUserId = currentUser.getUserId();

        Room room = roomService.getRoomDetail(roomId, currentUserId);
        
        // 🆕 WebSocket 기반 온라인 멤버 목록 조회
        List<RoomMemberResponse> memberResponses = roomService.getOnlineMembersWithWebSocket(roomId, currentUserId);

        RoomDetailResponse response = RoomDetailResponse.of(room, memberResponses);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(RsData.success("방 상세 정보 조회 완료", response));
    }

    @GetMapping("/my")
    @Operation(
        summary = "내 참여 방 목록 조회", 
        description = "현재 사용자가 참여 중인 방(멤버 이상) 목록을 조회합니다."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "조회 성공"),
        @ApiResponse(responseCode = "401", description = "인증 실패")
    })
    public ResponseEntity<RsData<List<MyRoomResponse>>> getMyRooms() {

        Long currentUserId = currentUser.getUserId();

        List<Room> rooms = roomService.getUserRooms(currentUserId);

        List<MyRoomResponse> roomList = rooms.stream()
                .map(room -> MyRoomResponse.of(
                        room, 
                        roomService.getUserRoomRole(room.getId(), currentUserId)
                ))
                .collect(Collectors.toList());

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(RsData.success("내 방 목록 조회 완료", roomList));
    }

    @PutMapping("/{roomId}")
    @Operation(
        summary = "방 설정 수정", 
        description = "방의 제목, 설명, 정원, RTC 설정 등을 수정합니다. 방장만 수정 가능합니다."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "수정 성공"),
        @ApiResponse(responseCode = "400", description = "잘못된 요청 (현재 참가자보다 작은 정원 등)"),
        @ApiResponse(responseCode = "403", description = "방장 권한 없음"),
        @ApiResponse(responseCode = "404", description = "존재하지 않는 방"),
        @ApiResponse(responseCode = "401", description = "인증 실패")
    })
    public ResponseEntity<RsData<Void>> updateRoom(
            @Parameter(description = "방 ID", required = true) @PathVariable Long roomId,
            @Valid @RequestBody UpdateRoomSettingsRequest request) {

        Long currentUserId = currentUser.getUserId();

        roomService.updateRoomSettings(
                roomId,
                request.getTitle(),
                request.getDescription(),
                request.getMaxParticipants(),
                request.getAllowCamera() != null ? request.getAllowCamera() : true,
                request.getAllowAudio() != null ? request.getAllowAudio() : true,
                request.getAllowScreenShare() != null ? request.getAllowScreenShare() : true,
                currentUserId
        );

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(RsData.success("방 설정 변경 완료", null));
    }

    @DeleteMapping("/{roomId}")
    @Operation(
        summary = "방 종료", 
        description = "방을 종료합니다. 모든 멤버가 강제 퇴장되며 더 이상 입장할 수 없습니다. 방장만 실행 가능합니다."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "종료 성공"),
        @ApiResponse(responseCode = "403", description = "방장 권한 없음"),
        @ApiResponse(responseCode = "404", description = "존재하지 않는 방"),
        @ApiResponse(responseCode = "401", description = "인증 실패")
    })
    public ResponseEntity<RsData<Void>> deleteRoom(
            @Parameter(description = "방 ID", required = true) @PathVariable Long roomId) {

        Long currentUserId = currentUser.getUserId();

        roomService.terminateRoom(roomId, currentUserId);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(RsData.success("방 종료 완료", null));
    }

    @GetMapping("/{roomId}/members")
    @Operation(
        summary = "방 멤버 목록 조회", 
        description = "방의 현재 온라인 멤버 목록을 조회합니다. 역할별로 정렬되며, WebSocket 기반 실시간 온라인 상태를 반영합니다."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "조회 성공"),
        @ApiResponse(responseCode = "403", description = "비공개 방에 대한 접근 권한 없음"),
        @ApiResponse(responseCode = "404", description = "존재하지 않는 방"),
        @ApiResponse(responseCode = "401", description = "인증 실패")
    })
    public ResponseEntity<RsData<List<RoomMemberResponse>>> getRoomMembers(
            @Parameter(description = "방 ID", required = true) @PathVariable Long roomId) {

        Long currentUserId = currentUser.getUserId();

        // 🆕 WebSocket 기반 온라인 멤버 목록 조회
        List<RoomMemberResponse> memberList = roomService.getOnlineMembersWithWebSocket(roomId, currentUserId);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(RsData.success("방 멤버 목록 조회 완료", memberList));
    }

    @GetMapping("/popular")
    @Operation(
        summary = "인기 방 목록 조회", 
        description = "참가자 수가 많은 인기 방 목록을 페이징하여 조회합니다. 참가자 수 내림차순에서 최신순으로 정렬됩니다."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "조회 성공"),
        @ApiResponse(responseCode = "401", description = "인증 실패")
    })
    public ResponseEntity<RsData<Map<String, Object>>> getPopularRooms(
            @Parameter(description = "페이지 번호 (0부터 시작)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지 크기") @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size);
        Page<Room> rooms = roomService.getPopularRooms(pageable);

        List<RoomResponse> roomList = rooms.getContent().stream()
                .map(RoomResponse::from)
                .collect(Collectors.toList());

        Map<String, Object> response = new HashMap<>();
        response.put("rooms", roomList);
        response.put("page", rooms.getNumber());
        response.put("size", rooms.getSize());
        response.put("totalElements", rooms.getTotalElements());
        response.put("totalPages", rooms.getTotalPages());
        response.put("hasNext", rooms.hasNext());

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(RsData.success("인기 방 목록 조회 완료", response));
    }


    // ======================== WebSocket 연동 API ========================
    @GetMapping("/{roomId}/websocket-status")
    @Operation(
        summary = "방 WebSocket 상태 조회", 
        description = "특정 방의 WebSocket 연결 상태와 실시간 온라인 멤버 수를 조회합니다."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "조회 성공"),
        @ApiResponse(responseCode = "403", description = "비공개 방에 대한 접근 권한 없음"),
        @ApiResponse(responseCode = "404", description = "존재하지 않는 방"),
        @ApiResponse(responseCode = "401", description = "인증 실패")
    })
    public ResponseEntity<RsData<Map<String, Object>>> getWebSocketStatus(
            @Parameter(description = "방 ID", required = true) @PathVariable Long roomId) {

        Long currentUserId = currentUser.getUserId();

        // 방 접근 권한 확인
        roomService.getRoomDetail(roomId, currentUserId);

        try {
            List<RoomMemberResponse> onlineMembers = roomService.getOnlineMembersWithWebSocket(roomId, currentUserId);

            Map<String, Object> status = new HashMap<>();
            status.put("roomId", roomId);
            status.put("onlineCount", onlineMembers.size());
            status.put("onlineMembers", onlineMembers);
            status.put("websocketChannels", Map.of(
                "roomUpdates", "/topic/rooms/" + roomId + "/updates",
                "roomChat", "/topic/rooms/" + roomId + "/chat",
                "privateMessages", "/user/queue/messages"
            ));
            status.put("lastUpdated", java.time.LocalDateTime.now());

            return ResponseEntity
                    .status(HttpStatus.OK)
                    .body(RsData.success("WebSocket 상태 조회 완료", status));

        } catch (Exception e) {
            log.error("WebSocket 상태 조회 실패 - 방: {}", roomId, e);

            Map<String, Object> errorStatus = new HashMap<>();
            errorStatus.put("roomId", roomId);
            errorStatus.put("error", "WebSocket 상태 조회 실패");
            errorStatus.put("message", e.getMessage());

            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(RsData.fail(ErrorCode.WS_REDIS_ERROR, errorStatus));
        }
    }

    @PostMapping("/{roomId}/refresh-online-members")
    @Operation(
        summary = "온라인 멤버 목록 강제 새로고침",
        description = "특정 방의 온라인 멤버 목록을 강제로 새로고침하고 모든 멤버에게 업데이트를 브로드캐스트합니다."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "새로고침 성공"),
        @ApiResponse(responseCode = "403", description = "권한 없음 (방장/부방장만 가능)"),
        @ApiResponse(responseCode = "404", description = "존재하지 않는 방"),
        @ApiResponse(responseCode = "401", description = "인증 실패")
    })
    public ResponseEntity<RsData<List<RoomMemberResponse>>> refreshOnlineMembers(
            @Parameter(description = "방 ID", required = true) @PathVariable Long roomId) {

        Long currentUserId = currentUser.getUserId();

        // 방장 또는 부방장 권한 확인
        RoomRole userRole = roomService.getUserRoomRole(roomId, currentUserId);
        if (!userRole.canManageRoom()) {
            throw new CustomException(ErrorCode.NOT_ROOM_MANAGER);
        }

        try {
            List<RoomMemberResponse> onlineMembers = roomService.getOnlineMembersWithWebSocket(roomId, currentUserId);

            // TODO: SessionManager를 통해 온라인 멤버 목록 브로드캐스트 강제 실행
            // sessionManager.broadcastOnlineMembersUpdate(roomId);

            log.info("온라인 멤버 목록 강제 새로고침 완료 - 방: {}, 요청자: {}, 온라인 멤버: {}명",
                    roomId, currentUserId, onlineMembers.size());

            return ResponseEntity
                    .status(HttpStatus.OK)
                    .body(RsData.success("온라인 멤버 목록 새로고침 완료", onlineMembers));

        } catch (Exception e) {
            log.error("온라인 멤버 목록 새로고침 실패 - 방: {}, 요청자: {}", roomId, currentUserId, e);
            throw new CustomException(ErrorCode.WS_REDIS_ERROR);
        }
    }
}
