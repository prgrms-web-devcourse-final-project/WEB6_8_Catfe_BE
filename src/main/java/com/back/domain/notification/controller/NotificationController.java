package com.back.domain.notification.controller;

import com.back.domain.notification.dto.NotificationResponse;
import com.back.domain.notification.dto.NotificationListResponse;
import com.back.domain.notification.entity.Notification;
import com.back.domain.notification.service.NotificationService;
import com.back.domain.studyroom.repository.RoomRepository;
import com.back.domain.user.common.entity.User;
import com.back.domain.user.common.repository.UserRepository;
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

    @Operation(summary = "알림 목록 조회")
    @GetMapping
    public ResponseEntity<RsData<NotificationListResponse>> getNotifications(
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails userDetails,
            @Parameter(description = "페이지 번호 (0부터 시작)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지 크기") @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "읽지 않은 알림만 조회") @RequestParam(defaultValue = "false") boolean unreadOnly) {

        log.info("알림 목록 조회 - 유저 ID: {}, 읽지 않은 것만: {}", userDetails.getUserId(), unreadOnly);

        Pageable pageable = PageRequest.of(page, size);

        Page<Notification> notifications = notificationService.getNotifications(
                userDetails.getUserId(), pageable, unreadOnly
        );

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
}