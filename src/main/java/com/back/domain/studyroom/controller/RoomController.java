package com.back.domain.studyroom.controller;

import com.back.domain.studyroom.entity.Room;
import com.back.domain.studyroom.entity.RoomMember;
import com.back.domain.studyroom.service.RoomService;
import com.back.global.common.dto.RsData;
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
 * 현재 잡아 놓은 API 목록
 - 방 CRUD
 - 방 입장/퇴장 처리
 - 멤버 관리 (목록 조회, 권한 변경, 추방)
 - 방 목록 조회 (공개방, 인기방, 내 참여방)

 인증:
 - 모든 API는 Authorization 헤더 필요 (JWT 토큰)
 - 현재는 임시로 하드코딩된 사용자 ID 사용, 예원님이 잡아준 임시 jwt 토큰과 연결 예정
 */


@RestController
@RequestMapping("/api/rooms")
@RequiredArgsConstructor
public class RoomController {
    private final RoomService roomService;
    // 방 생성
    @PostMapping
    public ResponseEntity<RsData<Map<String, Object>>> createRoom(
            @RequestBody Map<String, Object> request,
            @RequestHeader("Authorization") String authorization) {

        Long currentUserId = 1L; // 임시 하드코딩 데이터

        Room room = roomService.createRoom(
                (String) request.get("title"),
                (String) request.get("description"),
                (Boolean) request.getOrDefault("isPrivate", false),
                (String) request.get("password"),
                (Integer) request.getOrDefault("maxParticipants", 10),
                currentUserId
        );
        Map<String, Object> response = Map.of(
                "roomId", room.getId(),
                "title", room.getTitle(),
                "description", room.getDescription(),
                "isPrivate", room.isPrivate(),
                "maxParticipants", room.getMaxParticipants(),
                "currentParticipants", room.getCurrentParticipants(),
                "status", room.getStatus(),
                "createdAt", room.getCreatedAt()
        );
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(RsData.success("방 생성 완료", response));
    }

    /**
     방 입장
     입장 과정:
     - 공개 방: 바로 입장 가능
     - 비공개 방: password 필드에 비밀번호 전송 필요
     -- password: 비공개 방의 비밀번호
     - 멤버십 정보 (방 ID, 사용자 ID, 역할, 입장 시간)
     */
    @PostMapping("/api/rooms/{roomId}/{id}/join")
    public ResponseEntity<RsData<Map<String, Object>>> joinRoom(
            @PathVariable Long roomId,
            @RequestBody(required = false) Map<String, Object> request,
            @RequestHeader("Authorization") String authorization) {

        Long currentUserId = 1L; // 임시 하드코딩 데이터

        String password = null;
        if (request != null) {
            password = (String) request.get("password");
        }

        RoomMember member = roomService.joinRoom(roomId, password, currentUserId);

        Map<String, Object> response = Map.of(
                "roomId", member.getRoom().getId(),
                "userId", member.getUser().getId(),
                "role", member.getRole(),
                "joinedAt", member.getJoinedAt()
        );
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(RsData.success("방 입장 완료", response));
    }

    // 방 나가기 API
    @PostMapping("/api/rooms/{roomId}/{id}/leave")
    public ResponseEntity<RsData<Void>> leaveRoom(
            @PathVariable Long roomId,
            @RequestHeader("Authorization") String authorization) {

        Long currentUserId = 1L; // 임시 하드코딩 데이터

        roomService.leaveRoom(roomId, currentUserId);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(RsData.success("방 퇴장 완료", null));
    }

    /**
     * 공개 방 목록 조회 API
     - 공개 방만 조회 (isPrivate = false)
     - 입장 가능한 방만 조회 (활성화 + 정원 미초과)
     - 최신 생성 순으로 정렬

     * 현재 쿼리 파라미터:
     - page: 페이지 번호 (기본값: 0)
     - size: 페이지 크기 (기본값: 20)
     - search: 검색어 (향후 구현 예정)
     */
    @GetMapping
    public ResponseEntity<RsData<Map<String, Object>>> getRooms(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size);
        Page<Room> rooms = roomService.getJoinableRooms(pageable);

        List<Map<String, Object>> roomList = rooms.getContent().stream()
                .map(room -> {
                    Map<String, Object> roomMap = new HashMap<>();
                    roomMap.put("roomId", room.getId());
                    roomMap.put("title", room.getTitle());
                    roomMap.put("description", room.getDescription() != null ? room.getDescription() : "");
                    roomMap.put("currentParticipants", room.getCurrentParticipants());
                    roomMap.put("maxParticipants", room.getMaxParticipants());
                    roomMap.put("status", room.getStatus());
                    roomMap.put("createdBy", room.getCreatedBy().getNickname());
                    roomMap.put("createdAt", room.getCreatedAt());
                    return roomMap;
                })
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

     * 조회 정보:
     - 방 기본 정보 (제목, 설명, 설정 등)
     - 현재 온라인 멤버 목록 (닉네임, 역할, 상태)
     - 방 설정 (카메라, 오디오, 화면공유 허용 여부)

     * 접근 제한:
     - 공개 방: 누구나 조회 가능
     - 비공개 방: 해당 방 멤버만 조회 가능
     */
    @GetMapping("/api/rooms/{roomId}")
    public ResponseEntity<RsData<Map<String, Object>>> getRoomDetail(
            @PathVariable Long roomId,
            @RequestHeader("Authorization") String authorization) {

        Long currentUserId = 1L; // 임시 하드코딩

        Room room = roomService.getRoomDetail(roomId, currentUserId);
        List<RoomMember> members = roomService.getRoomMembers(roomId, currentUserId);

        List<Map<String, Object>> memberList = members.stream()
                .map(member -> {
                    Map<String, Object> memberMap = new HashMap<>();
                    memberMap.put("userId", member.getUser().getId());
                    memberMap.put("nickname", member.getUser().getNickname());
                    memberMap.put("role", member.getRole());
                    memberMap.put("isOnline", member.isOnline());
                    memberMap.put("joinedAt", member.getJoinedAt());
                    memberMap.put("lastActiveAt", member.getLastActiveAt() != null ? member.getLastActiveAt() : member.getJoinedAt());
                    return memberMap;
                })
                .collect(Collectors.toList());

        Map<String, Object> response = new HashMap<>();
        response.put("roomId", room.getId());
        response.put("title", room.getTitle());
        response.put("description", room.getDescription() != null ? room.getDescription() : "");
        response.put("isPrivate", room.isPrivate());
        response.put("maxParticipants", room.getMaxParticipants());
        response.put("currentParticipants", room.getCurrentParticipants());
        response.put("status", room.getStatus());
        response.put("allowCamera", room.isAllowCamera());
        response.put("allowAudio", room.isAllowAudio());
        response.put("allowScreenShare", room.isAllowScreenShare());
        response.put("createdBy", room.getCreatedBy().getNickname());
        response.put("createdAt", room.getCreatedAt());
        response.put("members", memberList);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(RsData.success("방 상세 정보 조회 완료", response));
    }

    //사용자 참여 방 목록 조회 API
    @GetMapping("/api/rooms/{roomId}/{id}/participants")
    public ResponseEntity<RsData<List<Map<String, Object>>>> getMyRooms(
            @RequestHeader("Authorization") String authorization) {

        Long currentUserId = 1L; // 임시 하드코딩

        List<Room> rooms = roomService.getUserRooms(currentUserId);

        List<Map<String, Object>> roomList = rooms.stream()
                .map(room -> {
                    Map<String, Object> roomMap = new HashMap<>();
                    roomMap.put("roomId", room.getId());
                    roomMap.put("title", room.getTitle());
                    roomMap.put("description", room.getDescription() != null ? room.getDescription() : "");
                    roomMap.put("currentParticipants", room.getCurrentParticipants());
                    roomMap.put("maxParticipants", room.getMaxParticipants());
                    roomMap.put("status", room.getStatus());
                    roomMap.put("myRole", roomService.getUserRoomRole(room.getId(), currentUserId));
                    roomMap.put("createdAt", room.getCreatedAt());
                    return roomMap;
                })
                .collect(Collectors.toList());

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(RsData.success("내 방 목록 조회 완료", roomList));
    }

    /**
     * 방 설정 수정 API
    권한: 방장만 수정 가능

     * 제약 사항:
     - 최대 참가자 수는 현재 참가자 수보다 작게 설정할 수 없음
     */
    @PutMapping("/api/rooms/{roomId}")
    public ResponseEntity<RsData<Void>> updateRoom(
            @PathVariable Long roomId,
            @RequestBody Map<String, Object> request,
            @RequestHeader("Authorization") String authorization) {

        Long currentUserId = 1L; // 임시 하드코딩

        roomService.updateRoomSettings(
                roomId,
                (String) request.get("title"),
                (String) request.get("description"),
                (Integer) request.get("maxParticipants"),
                (Boolean) request.getOrDefault("allowCamera", true),
                (Boolean) request.getOrDefault("allowAudio", true),
                (Boolean) request.getOrDefault("allowScreenShare", true),
                currentUserId
        );

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(RsData.success("방 설정 변경 완료", null));
    }

    /**
     * 방 종료 API
     권한: 방장만 종료 가능

     * 종료 처리:
     - 방 상태를 TERMINATED로 변경
     - 모든 멤버를 강제 오프라인 처리 (강퇴처리 식으로 진행 해야 할 지, 로직 처리 필요)
     - 더 이상 입장 불가능한 상태로 변경
     */
    @DeleteMapping("/api/rooms/{roomId}")
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
     - 현재 온라인 상태인 인원만 조회 (룸 내에서든 외에서든)
     - 역할별로 정렬 (방장 > 부방장 > 멤버 > 방문객)

     * 접근 제한:
     - 공개 방: 누구나 조회 가능
     - 비공개 방: 해당 방 멤버만 조회 가능
     */
    @GetMapping("/api/rooms/{roomId}/participants")
    public ResponseEntity<RsData<List<Map<String, Object>>>> getRoomMembers(
            @PathVariable Long roomId,
            @RequestHeader("Authorization") String authorization) {

        Long currentUserId = 1L; // 임시 하드코딩

        List<RoomMember> members = roomService.getRoomMembers(roomId, currentUserId);
        
        List<Map<String, Object>> memberList = members.stream()
                .map(member -> {
                    Map<String, Object> memberMap = new HashMap<>();
                    memberMap.put("userId", member.getUser().getId());
                    memberMap.put("nickname", member.getUser().getNickname());
                    memberMap.put("role", member.getRole());
                    memberMap.put("isOnline", member.isOnline());
                    memberMap.put("joinedAt", member.getJoinedAt());
                    memberMap.put("lastActiveAt", member.getLastActiveAt() != null ? member.getLastActiveAt() : member.getJoinedAt());
                    return memberMap;
                })
                .collect(Collectors.toList());

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(RsData.success("방 멤버 목록 조회 완료", memberList));
    }

    /**
     * 인기 방 목록 조회 API
     * 정렬 기준:
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

        List<Map<String, Object>> roomList = rooms.getContent().stream()
                .map(room -> {
                    Map<String, Object> roomMap = new HashMap<>();
                    roomMap.put("roomId", room.getId());
                    roomMap.put("title", room.getTitle());
                    roomMap.put("description", room.getDescription() != null ? room.getDescription() : "");
                    roomMap.put("currentParticipants", room.getCurrentParticipants());
                    roomMap.put("maxParticipants", room.getMaxParticipants());
                    roomMap.put("status", room.getStatus());
                    roomMap.put("createdBy", room.getCreatedBy().getNickname());
                    roomMap.put("createdAt", room.getCreatedAt());
                    return roomMap;
                })
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
