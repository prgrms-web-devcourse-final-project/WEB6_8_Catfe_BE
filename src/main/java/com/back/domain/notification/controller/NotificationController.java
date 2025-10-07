package com.back.domain.notification.controller;

import com.back.domain.notification.dto.NotificationCreateRequest;
import com.back.domain.notification.dto.NotificationResponse;
import com.back.domain.notification.dto.NotificationListResponse;
import com.back.domain.notification.entity.Notification;
import com.back.domain.notification.service.NotificationService;
import com.back.domain.studyroom.entity.Room;
import com.back.domain.studyroom.repository.RoomRepository;
import com.back.domain.user.entity.User;
import com.back.domain.user.repository.UserRepository;
import com.back.global.common.dto.RsData;
import com.back.global.exception.CustomException;
import com.back.global.exception.ErrorCode;
import com.back.global.security.user.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/notifications")
@Tag(name = "Notification API", description = "알림 관련 API")
public class NotificationController {

    private final NotificationService notificationService;
    private final UserRepository userRepository;
    private final RoomRepository roomRepository;

    @Operation(
            summary = "알림 전송",
            description = "USER/ROOM/COMMUNITY/SYSTEM 타입별 알림 생성 및 전송\n\n" +
                    "- USER: 개인 알림 (actorId, targetId 필수)\n" +
                    "- ROOM: 스터디룸 알림 (actorId, targetId(roomId) 필수)\n" +
                    "- COMMUNITY: 커뮤니티 알림 (actorId, targetId 필수)\n" +
                    "- SYSTEM: 시스템 전체 알림 (actorId, targetId 불필요)"
    )
    @PostMapping
    public ResponseEntity<RsData<NotificationResponse>> createNotification(
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody NotificationCreateRequest request) {

        log.info("알림 전송 요청 - 타입: {}, 제목: {}", request.targetType(), request.title());

        // targetType 검증
        if (!isValidTargetType(request.targetType())) {
            throw new CustomException(ErrorCode.NOTIFICATION_INVALID_TARGET_TYPE);
        }

        // SYSTEM이 아닌 경우 필수 필드 검증
        if (!"SYSTEM".equals(request.targetType())) {
            if (request.actorId() == null) {
                throw new CustomException(ErrorCode.NOTIFICATION_MISSING_ACTOR);
            }
            if (request.targetId() == null) {
                throw new CustomException(ErrorCode.NOTIFICATION_MISSING_TARGET);
            }
        }

        Notification notification = switch (request.targetType()) {
            case "USER" -> {
                // 수신자 조회
                User receiver = userRepository.findById(request.targetId())
                        .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

                // 발신자 조회
                User actor = userRepository.findById(request.actorId())
                        .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

                yield notificationService.createPersonalNotification(
                        receiver,
                        actor,
                        request.title(),
                        request.message(),
                        request.redirectUrl()
                );
            }
            case "ROOM" -> {
                // 스터디룸 조회
                Room room = roomRepository.findById(request.targetId())
                        .orElseThrow(() -> new CustomException(ErrorCode.ROOM_NOT_FOUND));

                // 발신자 조회
                User actor = userRepository.findById(request.actorId())
                        .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

                yield notificationService.createRoomNotification(
                        room,
                        actor,
                        request.title(),
                        request.message(),
                        request.redirectUrl()
                );
            }
            case "COMMUNITY" -> {
                // 수신자 조회 (리뷰/게시글 작성자)
                User receiver = userRepository.findById(request.targetId())
                        .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

                // 발신자 조회 (댓글/좋아요 작성자)
                User actor = userRepository.findById(request.actorId())
                        .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

                yield notificationService.createCommunityNotification(
                        receiver,
                        actor,
                        request.title(),
                        request.message(),
                        request.redirectUrl()
                );
            }
            case "SYSTEM" -> {
                // 시스템 알림은 발신자/수신자 없음
                yield notificationService.createSystemNotification(
                        request.title(),
                        request.message(),
                        request.redirectUrl()
                );
            }
            default -> throw new CustomException(ErrorCode.NOTIFICATION_INVALID_TARGET_TYPE);
        };

        NotificationResponse response = NotificationResponse.from(notification);

        return ResponseEntity.ok(RsData.success("알림 전송 성공", response));
    }

    @Operation(
            summary = "알림 목록 조회",
            description = "사용자의 알림 목록 조회 (페이징)\n\n" +
                    "- unreadOnly=true: 읽지 않은 알림만\n" +
                    "- unreadOnly=false: 모든 알림"
    )
    @GetMapping
    public ResponseEntity<RsData<NotificationListResponse>> getNotifications(
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails userDetails,
            @Parameter(description = "페이지 번호 (0부터 시작)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지 크기") @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "읽지 않은 알림만 조회") @RequestParam(defaultValue = "false") boolean unreadOnly) {

        log.info("알림 목록 조회 - 유저 ID: {}, 읽지 않은 것만: {}", userDetails.getUserId(), unreadOnly);

        Pageable pageable = PageRequest.of(page, size);
        Page<Notification> notifications;

        if (unreadOnly) {
            notifications = notificationService.getUnreadNotifications(userDetails.getUserId(), pageable);
        } else {
            notifications = notificationService.getUserNotifications(userDetails.getUserId(), pageable);
        }

        long unreadCount = notificationService.getUnreadCount(userDetails.getUserId());

        NotificationListResponse response = NotificationListResponse.from(
                notifications,
                userDetails.getUserId(),
                unreadCount,
                notificationService
        );

        return ResponseEntity.ok(RsData.success("알림 목록 조회 성공", response));
    }

    @Operation(
            summary = "알림 읽음 처리",
            description = "특정 알림을 읽음 상태로 변경\n\n" +
                    "이미 읽은 알림일 경우 NOTIFICATION_ALREADY_READ 에러 반환"
    )
    @PutMapping("/{notificationId}/read")
    public ResponseEntity<RsData<NotificationResponse>> markAsRead(
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails userDetails,
            @Parameter(description = "알림 ID") @PathVariable Long notificationId) {

        log.info("알림 읽음 처리 - 알림 ID: {}, 유저 ID: {}", notificationId, userDetails.getUserId());

        User user = userRepository.findById(userDetails.getUserId())
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        notificationService.markAsRead(notificationId, user);

        Notification notification = notificationService.getNotification(notificationId);
        boolean isRead = notificationService.isNotificationRead(notificationId, user.getId());

        // readAt 조회 (NotificationRead에서)
        NotificationResponse response = NotificationResponse.from(notification, isRead,
                isRead ? java.time.LocalDateTime.now() : null);

        return ResponseEntity.ok(RsData.success("알림 읽음 처리 성공", response));
    }

    @Operation(
            summary = "모든 알림 읽음 처리",
            description = "사용자의 읽지 않은 모든 알림을 읽음 상태로 변경"
    )
    @PutMapping("/read-all")
    public ResponseEntity<RsData<Void>> markAllAsRead(
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails userDetails) {

        log.info("전체 알림 읽음 처리 - 유저 ID: {}", userDetails.getUserId());

        User user = userRepository.findById(userDetails.getUserId())
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        notificationService.markMultipleAsRead(userDetails.getUserId(), user);

        return ResponseEntity.ok(RsData.success("전체 알림 읽음 처리 성공"));
    }

    // ==================== 헬퍼 메서드 ====================

    private boolean isValidTargetType(String targetType) {
        return targetType != null &&
                (targetType.equals("USER") ||
                        targetType.equals("ROOM") ||
                        targetType.equals("COMMUNITY") ||
                        targetType.equals("SYSTEM"));
    }
}