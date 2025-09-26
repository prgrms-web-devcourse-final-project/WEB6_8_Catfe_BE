package com.back.domain.studyroom.controller;

import com.back.domain.studyroom.dto.*;
import com.back.domain.studyroom.entity.Room;
import com.back.domain.studyroom.entity.RoomMember;
import com.back.domain.studyroom.service.RoomService;
import com.back.global.common.dto.RsData;
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
 * API 목록:
 - 방 CRUD (생성, 조회, 수정, 삭제)
 - 방 입장/퇴장
 - 멤버 관리 (목록 조회, 권한 변경, 추방)
 - 방 목록 조회 (공개방, 인기방, 내 참여방)
 * 
 * 인증:
 - 모든 API는 Authorization 헤더 필요 (JWT 토큰)
 - 현재는 임시로 하드코딩된 사용자 ID 사용 (수정 예정)
 - JWT 연동 시,, @CurrentUser 어노테이션으로 교체 예정
 */
@RestController
@RequestMapping("/api/rooms")
@RequiredArgsConstructor
public class RoomController {
    private final RoomService roomService;
    
    /**
     * 방 생성 API
     * POST /api/rooms
     */
    @PostMapping
    public ResponseEntity<RsData<RoomResponse>> createRoom(
            @Valid @RequestBody CreateRoomRequest request,
            @RequestHeader("Authorization") String authorization) {

        Long currentUserId = 1L; // 임시 하드코딩 - JWT 연동 시 @CurrentUser로 교체

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

    /**
     * 방 입장 API
     * POST /api/rooms/{roomId}/join
     * 공개 방: 바로 입장
     * 비공개 방: password 필드에 비밀번호 전송 필요
     */
    @PostMapping("/{roomId}/join")
    public ResponseEntity<RsData<JoinRoomResponse>> joinRoom(
            @PathVariable Long roomId,
            @RequestBody(required = false) JoinRoomRequest request,
            @RequestHeader("Authorization") String authorization) {

        Long currentUserId = 1L; // 임시 하드코딩

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

    /**
     * 방 나가기 API
     * POST /api/rooms/{roomId}/leave
     */
    @PostMapping("/{roomId}/leave")
    public ResponseEntity<RsData<Void>> leaveRoom(
            @PathVariable Long roomId,
            @RequestHeader("Authorization") String authorization) {

        Long currentUserId = 1L; // 임시 하드코딩

        roomService.leaveRoom(roomId, currentUserId);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(RsData.success("방 퇴장 완료", null));
    }

    /**
     * 공개 방 목록 조회 API
     * GET /api/rooms
     - 공개 방만 조회 (isPrivate = false)
     - 입장 가능한 방만 조회 (활성화 + 정원 미초과)
     - 최신 생성 순으로 정렬
     */
    @GetMapping
    public ResponseEntity<RsData<Map<String, Object>>> getRooms(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

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

    /**
     * 방 상세 정보 조회 API
     * GET /api/rooms/{roomId}
     * 조회 정보:
     * - 방 기본 정보 (제목, 설명, 설정 등)
     * - 현재 온라인 멤버 목록
     * - 방 설정 (카메라, 오디오, 화면공유 허용 여부)
     - 공개 방: 누구나 조회 가능
     - 비공개 방: 해당 방 멤버만 조회 가능
     */
    @GetMapping("/{roomId}")
    public ResponseEntity<RsData<RoomDetailResponse>> getRoomDetail(
            @PathVariable Long roomId,
            @RequestHeader("Authorization") String authorization) {

        Long currentUserId = 1L; // 임시 하드코딩

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

    /**
     * 사용자 참여 방 목록 조회 API
     * GET /api/rooms/my
     */
    @GetMapping("/my")
    public ResponseEntity<RsData<List<MyRoomResponse>>> getMyRooms(
            @RequestHeader("Authorization") String authorization) {

        Long currentUserId = 1L; // 임시 하드코딩

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

    /**
     * 방 설정 수정 API
     * PUT /api/rooms/{roomId}
     * 권한 : 방장만 수정 가능
     * 체크 : 최대 참가자 수는 현재 참가자 수보다 작게 설정할 수 없음
     */
    @PutMapping("/{roomId}")
    public ResponseEntity<RsData<Void>> updateRoom(
            @PathVariable Long roomId,
            @Valid @RequestBody UpdateRoomSettingsRequest request,
            @RequestHeader("Authorization") String authorization) {

        Long currentUserId = 1L; // 임시 하드코딩

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

    /**
     * 방 종료 API
     * DELETE /api/rooms/{roomId}
     * 권한: 방장만 종료 가능
     * 종료 처리:
     - 방 상태를 TERMINATED로 변경
     - 모든 멤버를 강제 오프라인 처리
     - 더 이상 입장 불가능한 상태로 변경
     */
    @DeleteMapping("/{roomId}")
    public ResponseEntity<RsData<Void>> deleteRoom(
            @PathVariable Long roomId,
            @RequestHeader("Authorization") String authorization) {

        Long currentUserId = 1L; // 임시 하드코딩

        roomService.terminateRoom(roomId, currentUserId);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(RsData.success("방 종료 완료", null));
    }

    /**
     * 방 멤버 목록 조회 API
     * GET /api/rooms/{roomId}/members
     * - 현재 온라인 상태인 인원만 조회
     * - 역할별로 정렬 (방장 > 부방장 > 멤버 > 방문객)
     * 접근 제한:
     * - 공개 방: 누구나 조회 가능
     * - 비공개 방: 해당 방 멤버만 조회 가능 (로직 변경 예정)
     */
    @GetMapping("/{roomId}/members")
    public ResponseEntity<RsData<List<RoomMemberResponse>>> getRoomMembers(
            @PathVariable Long roomId,
            @RequestHeader("Authorization") String authorization) {

        Long currentUserId = 1L; // 임시 하드코딩

        List<RoomMember> members = roomService.getRoomMembers(roomId, currentUserId);
        
        List<RoomMemberResponse> memberList = members.stream()
                .map(RoomMemberResponse::from)
                .collect(Collectors.toList());

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(RsData.success("방 멤버 목록 조회 완료", memberList));
    }

    /**
     * 인기 방 목록 조회 API
     * GET /api/rooms/popular
     - 1순위: 현재 참가자 수 (내림차순)
     - 2순위: 생성 시간 (최신순)
     * 조회 조건:
     - 공개 방만 조회
     - 활성화된 방만 조회
     */
    @GetMapping("/popular")
    public ResponseEntity<RsData<Map<String, Object>>> getPopularRooms(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

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
