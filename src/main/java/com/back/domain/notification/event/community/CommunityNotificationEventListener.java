package com.back.domain.notification.event.community;

import com.back.domain.notification.entity.NotificationSettingType;
import com.back.domain.notification.service.NotificationService;
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
public class CommunityNotificationEventListener {

    private final NotificationService notificationService;
    private final UserRepository userRepository;

    // 댓글 작성 시 - 게시글 작성자에게 알림
    @EventListener
    @Async("notificationExecutor")
    public void handleCommentCreated(CommentCreatedEvent event) {
        log.info("[알림] 댓글 작성: postId={}, commentId={}, actorId={}",
                event.getPostId(), event.getCommentId(), event.getActorId());

        try {
            User actor = userRepository.findById(event.getActorId())
                    .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

            User receiver = userRepository.findById(event.getReceiverId())
                    .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

            notificationService.createCommunityNotification(
                    receiver,
                    actor,
                    event.getTitle(),
                    event.getContent(),
                    "/posts/" + event.getPostId(),
                    NotificationSettingType.POST_COMMENT
            );

            log.info("[알림] 댓글 작성 알림 전송 완료");

        } catch (Exception e) {
            log.error("[알림] 댓글 작성 알림 전송 실패: error={}", e.getMessage(), e);
        }
    }

    // 대댓글 작성 시 - 댓글 작성자에게 알림
    @EventListener
    @Async("notificationExecutor")
    public void handleReplyCreated(ReplyCreatedEvent event) {
        log.info("[알림] 대댓글 작성: parentCommentId={}, replyId={}, actorId={}",
                event.getParentCommentId(), event.getReplyId(), event.getActorId());

        try {
            User actor = userRepository.findById(event.getActorId())
                    .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

            User receiver = userRepository.findById(event.getReceiverId())
                    .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

            notificationService.createCommunityNotification(
                    receiver,
                    actor,
                    event.getTitle(),
                    event.getContent(),
                    "/posts/" + event.getPostId() + "#comment-" + event.getParentCommentId(),
                    NotificationSettingType.POST_COMMENT
            );

            log.info("[알림] 대댓글 작성 알림 전송 완료");

        } catch (Exception e) {
            log.error("[알림] 대댓글 작성 알림 전송 실패: error={}", e.getMessage(), e);
        }
    }

    // 게시글 좋아요 시 - 게시글 작성자에게 알림
    @EventListener
    @Async("notificationExecutor")
    public void handlePostLiked(PostLikedEvent event) {
        log.info("[알림] 게시글 좋아요: postId={}, actorId={}",
                event.getPostId(), event.getActorId());

        try {
            User actor = userRepository.findById(event.getActorId())
                    .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

            User receiver = userRepository.findById(event.getReceiverId())
                    .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

            notificationService.createCommunityNotification(
                    receiver,
                    actor,
                    event.getTitle(),
                    event.getContent(),
                    "/posts/" + event.getPostId(),
                    NotificationSettingType.POST_LIKE
            );

            log.info("[알림] 게시글 좋아요 알림 전송 완료");

        } catch (Exception e) {
            log.error("[알림] 게시글 좋아요 알림 전송 실패: error={}", e.getMessage(), e);
        }
    }

    // 댓글 좋아요 시 - 댓글 작성자에게 알림
    @EventListener
    @Async("notificationExecutor")
    public void handleCommentLiked(CommentLikedEvent event) {
        log.info("[알림] 댓글 좋아요: commentId={}, actorId={}",
                event.getCommentId(), event.getActorId());

        try {
            User actor = userRepository.findById(event.getActorId())
                    .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

            User receiver = userRepository.findById(event.getReceiverId())
                    .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

            notificationService.createCommunityNotification(
                    receiver,
                    actor,
                    event.getTitle(),
                    event.getContent(),
                    "/posts/" + event.getPostId() + "#comment-" + event.getCommentId(),
                    NotificationSettingType.POST_LIKE
            );

            log.info("[알림] 댓글 좋아요 알림 전송 완료");

        } catch (Exception e) {
            log.error("[알림] 댓글 좋아요 알림 전송 실패: error={}", e.getMessage(), e);
        }
    }
}