package com.back.domain.studyroom.controller;

import com.back.domain.studyroom.dto.*;
import com.back.domain.studyroom.entity.Room;
import com.back.domain.studyroom.entity.RoomMember;
import com.back.domain.studyroom.entity.RoomRole;
import com.back.domain.studyroom.service.RoomService;
import com.back.domain.user.common.entity.User;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 스터디 룸 관련 API 컨트롤러
 * - JWT 인증 필수 (Spring Security + CurrentUser)
 * - Swagger에서 테스트 시 "Authorize" 버튼으로 토큰 입력
 */
@RestController
@RequestMapping("/api/rooms")
@RequiredArgsConstructor
@Tag(name = "Room API", description = "스터디 룸 관련 API")
@SecurityRequirement(name = "Bearer Authentication")
public class RoomController {
    private final RoomService roomService;
    private final CurrentUser currentUser;
    
    @PostMapping
    @Operation(
        summary = "방 생성", 
        description = "새로운 스터디 룸을 생성합니다. 방 생성자는 자동으로 방장(HOST)이 됩니다. useWebRTC로 화상/음성/화면공유 기능을 한 번에 제어할 수 있습니다."
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
                currentUserId,
                request.getUseWebRTC() != null ? request.getUseWebRTC() : true,  // 디폴트: true
                request.getThumbnailAttachmentId()  // 썸네일 Attachment ID
        );
        
        RoomResponse response = roomService.toRoomResponse(room);
        
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(RsData.success("방 생성 완료", response));
    }

    @PostMapping("/{roomId}/join")
    @Operation(
        summary = "방 입장", 
        description = "특정 스터디 룸에 입장합니다. 공개방은 바로 입장 가능하며, 비공개방은 비밀번호가 필요합니다."
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
        JoinRoomResponse response = JoinRoomResponse.from(member);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(RsData.success("방 입장 완료", response));
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

    @GetMapping("/all")
    @Operation(
        summary = "모든 방 목록 조회", 
        description = "공개 방과 비공개 방 전체를 조회합니다. 열린 방(WAITING, ACTIVE)이 우선 표시되고, 닫힌 방(PAUSED, TERMINATED)은 뒤로 밀립니다. 비로그인 사용자도 조회 가능합니다."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "조회 성공")
    })
    public ResponseEntity<RsData<Map<String, Object>>> getAllRooms(
            @Parameter(description = "페이지 번호 (0부터 시작)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지 크기") @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size);
        Page<Room> rooms = roomService.getAllRooms(pageable);

        // 비로그인 사용자도 조회 가능 (userId = null이면 isFavorite = false)
        Long userId = currentUser.getUserIdOrNull();
        List<RoomResponse> roomList = roomService.toRoomResponseList(rooms.getContent(), userId);

        Map<String, Object> response = new HashMap<>();
        response.put("rooms", roomList);
        response.put("page", rooms.getNumber());
        response.put("size", rooms.getSize());
        response.put("totalElements", rooms.getTotalElements());
        response.put("totalPages", rooms.getTotalPages());
        response.put("hasNext", rooms.hasNext());

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(RsData.success("모든 방 목록 조회 완료", response));
    }

    @GetMapping("/public")
    @Operation(
        summary = "공개 방 목록 조회", 
        description = "공개 방 전체를 조회합니다. includeInactive=true로 설정하면 닫힌 방도 포함됩니다 (기본값: true). 열린 방이 우선 표시됩니다. 비로그인 사용자도 조회 가능합니다."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "조회 성공")
    })
    public ResponseEntity<RsData<Map<String, Object>>> getPublicRooms(
            @Parameter(description = "페이지 번호 (0부터 시작)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지 크기") @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "닫힌 방 포함 여부") @RequestParam(defaultValue = "true") boolean includeInactive) {

        Pageable pageable = PageRequest.of(page, size);
        Page<Room> rooms = roomService.getPublicRooms(includeInactive, pageable);

        // 비로그인 사용자도 조회 가능
        Long userId = currentUser.getUserIdOrNull();
        List<RoomResponse> roomList = roomService.toRoomResponseList(rooms.getContent(), userId);

        Map<String, Object> response = new HashMap<>();
        response.put("rooms", roomList);
        response.put("page", rooms.getNumber());
        response.put("size", rooms.getSize());
        response.put("totalElements", rooms.getTotalElements());
        response.put("totalPages", rooms.getTotalPages());
        response.put("hasNext", rooms.hasNext());

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(RsData.success("공개 방 목록 조회 완료", response));
    }

    @GetMapping("/private")
    @Operation(
        summary = "내 비공개 방 목록 조회", 
        description = "내가 멤버로 등록된 비공개 방을 조회합니다. includeInactive=true로 설정하면 닫힌 방도 포함됩니다 (기본값: true). 열린 방이 우선 표시됩니다."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "조회 성공"),
        @ApiResponse(responseCode = "401", description = "인증 실패")
    })
    public ResponseEntity<RsData<Map<String, Object>>> getMyPrivateRooms(
            @Parameter(description = "페이지 번호 (0부터 시작)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지 크기") @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "닫힌 방 포함 여부") @RequestParam(defaultValue = "true") boolean includeInactive) {

        Long currentUserId = currentUser.getUserId();

        Pageable pageable = PageRequest.of(page, size);
        Page<Room> rooms = roomService.getMyPrivateRooms(currentUserId, includeInactive, pageable);

        List<RoomResponse> roomList = roomService.toRoomResponseList(rooms.getContent(), currentUserId);

        Map<String, Object> response = new HashMap<>();
        response.put("rooms", roomList);
        response.put("page", rooms.getNumber());
        response.put("size", rooms.getSize());
        response.put("totalElements", rooms.getTotalElements());
        response.put("totalPages", rooms.getTotalPages());
        response.put("hasNext", rooms.hasNext());

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(RsData.success("내 비공개 방 목록 조회 완료", response));
    }

    @GetMapping("/my/hosting")
    @Operation(
        summary = "내가 호스트인 방 목록 조회", 
        description = "내가 방장으로 있는 방을 조회합니다. 열린 방이 우선 표시되고, 닫힌 방은 뒤로 밀립니다."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "조회 성공"),
        @ApiResponse(responseCode = "401", description = "인증 실패")
    })
    public ResponseEntity<RsData<Map<String, Object>>> getMyHostingRooms(
            @Parameter(description = "페이지 번호 (0부터 시작)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지 크기") @RequestParam(defaultValue = "20") int size) {

        Long currentUserId = currentUser.getUserId();

        Pageable pageable = PageRequest.of(page, size);
        Page<Room> rooms = roomService.getMyHostingRooms(currentUserId, pageable);

        List<RoomResponse> roomList = roomService.toRoomResponseList(rooms.getContent(), currentUserId);

        Map<String, Object> response = new HashMap<>();
        response.put("rooms", roomList);
        response.put("page", rooms.getNumber());
        response.put("size", rooms.getSize());
        response.put("totalElements", rooms.getTotalElements());
        response.put("totalPages", rooms.getTotalPages());
        response.put("hasNext", rooms.hasNext());

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(RsData.success("내가 호스트인 방 목록 조회 완료", response));
    }

    @GetMapping
    @Operation(
        summary = "입장 가능한 공개 방 목록 조회 (기존)", 
        description = "입장 가능한 공개 스터디 룸 목록을 페이징하여 조회합니다. 최신 생성 순으로 정렬됩니다. 비로그인 사용자도 조회 가능합니다."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "조회 성공")
    })
    public ResponseEntity<RsData<Map<String, Object>>> getRooms(
            @Parameter(description = "페이지 번호 (0부터 시작)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지 크기") @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size);
        Page<Room> rooms = roomService.getJoinableRooms(pageable);

        // 비로그인 사용자도 조회 가능
        Long userId = currentUser.getUserIdOrNull();
        List<RoomResponse> roomList = roomService.toRoomResponseList(rooms.getContent(), userId);

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
        description = "특정 방의 상세 정보와 현재 온라인 멤버 목록을 조회합니다. 비로그인 사용자도 조회 가능합니다."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "조회 성공"),
        @ApiResponse(responseCode = "404", description = "존재하지 않는 방")
    })
    public ResponseEntity<RsData<RoomDetailResponse>> getRoomDetail(
            @Parameter(description = "방 ID", required = true) @PathVariable Long roomId) {

        // 비로그인 사용자는 userId = null로 처리
        Long currentUserId = currentUser.getUserIdOrNull();

        Room room = roomService.getRoomDetail(roomId, currentUserId);
        List<RoomMember> members = roomService.getRoomMembers(roomId, currentUserId);

        RoomDetailResponse response = roomService.toRoomDetailResponse(room, members, currentUserId);

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

        List<MyRoomResponse> roomList = roomService.toMyRoomResponseList(rooms, currentUserId);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(RsData.success("내 방 목록 조회 완료", roomList));
    }

    @PutMapping("/{roomId}")
    @Operation(
        summary = "방 설정 수정", 
        description = "방의 제목, 설명, 정원, 썸네일을 수정합니다.thumbnailAttachmentId가 null이면 썸네일 변경 없이 기존 유지됩니다. 방장만 수정 가능합니다. WebRTC 설정은 현재 수정 불가합니다."
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
                request.getThumbnailAttachmentId(),  // 썸네일 Attachment ID
                currentUserId
        );

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(RsData.success("방 설정 변경 완료", null));
    }

    @PutMapping("/{roomId}/password")
    @Operation(
        summary = "방 비밀번호 변경",
        description = "비공개 방의 비밀번호를 변경합니다. 현재 비밀번호 확인 후 새 비밀번호로 변경합니다. 방장만 실행 가능합니다."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "비밀번호 변경 성공"),
        @ApiResponse(responseCode = "400", description = "현재 비밀번호 불일치"),
        @ApiResponse(responseCode = "403", description = "방장 권한 없음"),
        @ApiResponse(responseCode = "404", description = "존재하지 않는 방"),
        @ApiResponse(responseCode = "401", description = "인증 실패")
    })
    public ResponseEntity<RsData<Void>> updateRoomPassword(
            @Parameter(description = "방 ID", required = true) @PathVariable Long roomId,
            @Valid @RequestBody UpdateRoomPasswordRequest request) {

        Long currentUserId = currentUser.getUserId();

        roomService.updateRoomPassword(
                roomId,
                request.getCurrentPassword(),
                request.getNewPassword(),
                currentUserId
        );

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(RsData.success("방 비밀번호 변경 완료", null));
    }

    @DeleteMapping("/{roomId}/password")
    @Operation(
        summary = "방 비밀번호 제거",
        description = "방의 비밀번호를 제거합니다. 비밀번호가 제거되면 누구나 자유롭게 입장할 수 있습니다. 방장만 실행 가능합니다."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "비밀번호 제거 성공"),
        @ApiResponse(responseCode = "403", description = "방장 권한 없음"),
        @ApiResponse(responseCode = "404", description = "존재하지 않는 방"),
        @ApiResponse(responseCode = "401", description = "인증 실패")
    })
    public ResponseEntity<RsData<Void>> removeRoomPassword(
            @Parameter(description = "방 ID", required = true) @PathVariable Long roomId) {

        Long currentUserId = currentUser.getUserId();

        roomService.removeRoomPassword(roomId, currentUserId);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(RsData.success("방 비밀번호 제거 완료", null));
    }

    @PostMapping("/{roomId}/password")
    @Operation(
        summary = "방 비밀번호 설정",
        description = "비밀번호가 없는 방에 비밀번호를 설정합니다. 이미 비밀번호가 있는 경우 비밀번호 변경 API(PUT)를 사용. 방장만 실행 가능합니다."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "비밀번호 설정 성공"),
        @ApiResponse(responseCode = "400", description = "이미 비밀번호가 설정되어 있음"),
        @ApiResponse(responseCode = "403", description = "방장 권한 없음"),
        @ApiResponse(responseCode = "404", description = "존재하지 않는 방"),
        @ApiResponse(responseCode = "401", description = "인증 실패")
    })
    public ResponseEntity<RsData<Void>> setRoomPassword(
            @Parameter(description = "방 ID", required = true) @PathVariable Long roomId,
            @Valid @RequestBody SetRoomPasswordRequest request) {

        Long currentUserId = currentUser.getUserId();

        roomService.setRoomPassword(roomId, request.getNewPassword(), currentUserId);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(RsData.success("방 비밀번호 설정 완료", null));
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

    @PutMapping("/{roomId}/pause")
    @Operation(
        summary = "방 일시정지", 
        description = "방을 일시정지 상태로 변경합니다. 일시정지된 방은 입장할 수 없으며, 방 목록에서 뒤로 밀립니다. 방장만 실행 가능합니다."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "일시정지 성공"),
        @ApiResponse(responseCode = "400", description = "이미 종료되었거나 일시정지 불가능한 상태"),
        @ApiResponse(responseCode = "403", description = "방장 권한 없음"),
        @ApiResponse(responseCode = "404", description = "존재하지 않는 방"),
        @ApiResponse(responseCode = "401", description = "인증 실패")
    })
    public ResponseEntity<RsData<Void>> pauseRoom(
            @Parameter(description = "방 ID", required = true) @PathVariable Long roomId) {

        Long currentUserId = currentUser.getUserId();

        roomService.pauseRoom(roomId, currentUserId);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(RsData.success("방 일시정지 완료", null));
    }

    @PutMapping("/{roomId}/activate")
    @Operation(
        summary = "방 활성화/재개", 
        description = "일시정지된 방을 다시 활성화합니다. 활성화된 방은 다시 입장 가능하며, 방 목록 앞쪽에 표시됩니다. 방장만 실행 가능합니다."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "활성화 성공"),
        @ApiResponse(responseCode = "400", description = "이미 종료되었거나 활성화 불가능한 상태"),
        @ApiResponse(responseCode = "403", description = "방장 권한 없음"),
        @ApiResponse(responseCode = "404", description = "존재하지 않는 방"),
        @ApiResponse(responseCode = "401", description = "인증 실패")
    })
    public ResponseEntity<RsData<Void>> activateRoom(
            @Parameter(description = "방 ID", required = true) @PathVariable Long roomId) {

        Long currentUserId = currentUser.getUserId();

        roomService.activateRoom(roomId, currentUserId);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(RsData.success("방 활성화 완료", null));
    }

    @GetMapping("/{roomId}/members")
    @Operation(
        summary = "방 멤버 목록 조회", 
        description = "방의 현재 온라인 멤버 목록을 조회합니다. 프로필 이미지와 아바타 정보를 포함. 역할별로 정렬됩니다(방장>부방장>멤버>방문객)."
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

        List<RoomMember> members = roomService.getRoomMembers(roomId, currentUserId);
        
        // 아바타 정보 포함하여 변환 (N+1 방지)
        List<RoomMemberResponse> memberList = roomService.toRoomMemberResponseList(roomId, members);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(RsData.success("방 멤버 목록 조회 완료", memberList));
    }

    @GetMapping("/popular")
    @Operation(
        summary = "인기 방 목록 조회", 
        description = "참가자 수가 많은 인기 방 목록을 페이징하여 조회합니다. 공개방과 비공개방 모두 포함됩니다. 참가자 수 내림차순에서 최신순으로 정렬됩니다. 비로그인 사용자도 조회 가능합니다."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "조회 성공")
    })
    public ResponseEntity<RsData<Map<String, Object>>> getPopularRooms(
            @Parameter(description = "페이지 번호 (0부터 시작)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지 크기") @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size);
        Page<Room> rooms = roomService.getPopularRooms(pageable);

        // 비로그인 사용자도 조회 가능
        Long userId = currentUser.getUserIdOrNull();
        List<RoomResponse> roomList = roomService.toRoomResponseList(rooms.getContent(), userId);

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

    @PutMapping("/{roomId}/members/{userId}/role")
    @Operation(
        summary = "멤버 역할 변경",
        description = "방 멤버의 역할을 변경합니다. 방장만 실행 가능합니다. VISITOR를 포함한 모든 사용자의 역할을 변경할 수 있으며, HOST로 변경 시 기존 방장은 자동으로 MEMBER로 강등됩니다."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "역할 변경 성공"),
        @ApiResponse(responseCode = "400", description = "자신의 역할은 변경 불가"),
        @ApiResponse(responseCode = "403", description = "방장 권한 없음"),
        @ApiResponse(responseCode = "404", description = "존재하지 않는 방 또는 사용자"),
        @ApiResponse(responseCode = "401", description = "인증 실패")
    })
    public ResponseEntity<RsData<ChangeRoleResponse>> changeUserRole(
            @Parameter(description = "방 ID", required = true) @PathVariable Long roomId,
            @Parameter(description = "대상 사용자 ID", required = true) @PathVariable Long userId,
            @Valid @RequestBody ChangeRoleRequest request) {

        Long currentUserId = currentUser.getUserId();

        // 변경 전 역할 조회
        RoomRole oldRole = roomService.getUserRoomRole(roomId, userId);

        // 역할 변경
        roomService.changeUserRole(roomId, userId, request.getNewRole(), currentUserId);

        // 사용자 정보 조회
        User targetUser = roomService.getUserById(userId);

        ChangeRoleResponse response = ChangeRoleResponse.of(
                userId, 
                targetUser.getNickname(), 
                oldRole, 
                request.getNewRole()
        );

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(RsData.success("역할 변경 완료", response));
    }

    @DeleteMapping("/{roomId}/members/{userId}")
    @Operation(
        summary = "멤버 추방",
        description = "방에서 특정 멤버를 강제로 퇴장시킵니다. 방장과 부방장만 실행 가능하며, 방장은 추방할 수 없습니다. 추방된 사용자는 Redis에서 즉시 제거되고 알림을 받습니다."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "추방 성공"),
        @ApiResponse(responseCode = "400", description = "방장은 추방할 수 없음"),
        @ApiResponse(responseCode = "403", description = "추방 권한 없음 (방장 또는 부방장만 가능)"),
        @ApiResponse(responseCode = "404", description = "존재하지 않는 방 또는 멤버"),
        @ApiResponse(responseCode = "401", description = "인증 실패")
    })
    public ResponseEntity<RsData<Void>> kickMember(
            @Parameter(description = "방 ID", required = true) @PathVariable Long roomId,
            @Parameter(description = "추방할 사용자 ID", required = true) @PathVariable Long userId) {

        Long currentUserId = currentUser.getUserId();

        roomService.kickMember(roomId, userId, currentUserId);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(RsData.success("멤버 추방 완료", null));
    }
}
