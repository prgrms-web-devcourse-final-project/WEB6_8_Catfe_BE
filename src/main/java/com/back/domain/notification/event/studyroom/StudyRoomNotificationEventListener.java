package com.back.domain.notification.event.studyroom;

import com.back.domain.notification.entity.NotificationSettingType;
import com.back.domain.notification.service.NotificationService;
import com.back.domain.studyroom.entity.Room;
import com.back.domain.studyroom.repository.RoomMemberRepository;
import com.back.domain.studyroom.repository.RoomRepository;
import com.back.domain.user.entity.User;
import com.back.domain.user.repository.UserRepository;
import com.back.global.exception.CustomException;
import com.back.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class StudyRoomNotificationEventListener {

    private final NotificationService notificationService;

    // 스터디룸 공지사항 등록 시 - 전체 멤버에게 알림
    @EventListener
    @Async("notificationExecutor")
    public void handleNoticeCreated(StudyRoomNoticeCreatedEvent event) {
        log.info("[알림] 스터디룸 공지사항 등록: roomId={}, actorId={}",
                event.getStudyRoomId(), event.getActorId());

        try {
            notificationService.createRoomNotification(
                    event.getStudyRoomId(),
                    event.getActorId(),
                    event.getTitle(),
                    event.getNoticeTitle(),
                    "/rooms/" + event.getStudyRoomId() + "/notices",
                    NotificationSettingType.ROOM_NOTICE
            );

            log.info("[알림] 스터디룸 공지사항 알림 전송 완료");

        } catch (Exception e) {
            log.error("[알림] 스터디룸 공지사항 알림 전송 실패: roomId={}, error={}",
                    event.getStudyRoomId(), e.getMessage(), e);
        }
    }

    // 권한 변경 시 - 해당 유저에게만 알림
    @EventListener
    @Async("notificationExecutor")
    public void handleMemberRoleChanged(MemberRoleChangedEvent event) {
        log.info("[알림] 멤버 권한 변경: roomId={}, targetUserId={}, newRole={}",
                event.getStudyRoomId(), event.getTargetUserId(), event.getNewRole());

        try {
            notificationService.createPersonalNotification(
                    event.getTargetUserId(),
                    event.getActorId(),
                    event.getTitle(),
                    event.getContent(),
                    "/rooms/" + event.getStudyRoomId(),
                    NotificationSettingType.ROOM_JOIN
            );

            log.info("[알림] 권한 변경 알림 전송 완료");

        } catch (Exception e) {
            log.error("[알림] 권한 변경 알림 전송 실패: error={}", e.getMessage(), e);
        }
    }

    // 멤버 추방 시 - 해당 유저에게만 알림
    @EventListener
    @Async("notificationExecutor")
    public void handleMemberKicked(MemberKickedEvent event) {
        log.info("[알림] 멤버 추방: roomId={}, targetUserId={}",
                event.getStudyRoomId(), event.getTargetUserId());

        try {
            notificationService.createPersonalNotification(
                    event.getTargetUserId(),
                    event.getActorId(),
                    event.getTitle(),
                    event.getContent(),
                    "/rooms",
                    NotificationSettingType.ROOM_JOIN
            );

            log.info("[알림] 멤버 추방 알림 전송 완료");

        } catch (Exception e) {
            log.error("[알림] 멤버 추방 알림 전송 실패: error={}", e.getMessage(), e);
        }
    }

    // 방장 위임 시 - 새 방장에게만 알림
    @EventListener
    @Async("notificationExecutor")
    public void handleOwnerTransferred(OwnerTransferredEvent event) {
        log.info("[알림] 방장 위임: roomId={}, newOwnerId={}",
                event.getStudyRoomId(), event.getNewOwnerId());

        try {
            notificationService.createPersonalNotification(
                    event.getNewOwnerId(),
                    event.getActorId(),
                    event.getTitle(),
                    event.getContent(),
                    "/rooms/" + event.getStudyRoomId(),
                    NotificationSettingType.ROOM_JOIN
            );

            log.info("[알림] 방장 위임 알림 전송 완료");

        } catch (Exception e) {
            log.error("[알림] 방장 위임 알림 전송 실패: error={}", e.getMessage(), e);
        }
    }
}