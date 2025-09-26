package com.back.domain.studyroom.controller;

import com.back.domain.studyroom.dto.*;
import com.back.domain.studyroom.entity.Room;
import com.back.domain.studyroom.entity.RoomMember;
import com.back.domain.studyroom.service.RoomService;
import com.back.global.common.dto.RsData;
import com.back.global.security.CurrentUser;
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
import java.util.stream.Collectors;

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
        description = "특정 방의 상세 정보와 현재 온라인 멤버 목록을 조회합니다. 비공개 방은 멤버만 조회 가능합니다."
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
        List<RoomMember> members = roomService.getRoomMembers(roomId, currentUserId);

        List<RoomMemberResponse> memberResponses = members.stream()
                .map(RoomMemberResponse::from)
                .collect(Collectors.toList());

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
        description = "방의 현재 온라인 멤버 목록을 조회합니다. 역할별로 정렬됩니다(방장>부방장>멤버>방문객)."
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
        
        List<RoomMemberResponse> memberList = members.stream()
                .map(RoomMemberResponse::from)
                .collect(Collectors.toList());

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
}
