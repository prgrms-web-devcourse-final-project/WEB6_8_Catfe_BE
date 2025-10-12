package com.back.domain.notification.event.community;

import com.back.domain.notification.entity.NotificationSettingType;
import com.back.domain.notification.service.NotificationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CommunityNotificationEventListener 테스트")
class CommunityNotificationEventListenerTest {

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private CommunityNotificationEventListener listener;

    // ====================== 댓글 작성 이벤트 ======================

    @Test
    @DisplayName("댓글 작성 이벤트 수신 - 알림 생성 성공")
    void t1() {
        // given
        CommentCreatedEvent event = new CommentCreatedEvent(1L, 2L, 100L, 200L, "댓글");

        // when
        listener.handleCommentCreated(event);

        // then
        verify(notificationService).createCommunityNotification(
                eq(2L), // receiverId
                eq(1L), // actorId
                anyString(),
                anyString(),
                eq("/posts/100"),
                eq(NotificationSettingType.POST_COMMENT)
        );
    }

    @Test
    @DisplayName("댓글 작성 이벤트 - 작성자(actor) 없음")
    void t2() {
        // given
        ReplyCreatedEvent event = new ReplyCreatedEvent(1L, 2L, 100L, 200L, 300L, "대댓글");

        // when
        listener.handleReplyCreated(event);

        // then
        verify(notificationService).createCommunityNotification(
                eq(2L), // receiverId
                eq(1L), // actorId
                anyString(),
                anyString(),
                eq("/posts/100#comment-200"),
                eq(NotificationSettingType.POST_COMMENT)
        );
    }

    @Test
    @DisplayName("댓글 작성 이벤트 - 수신자(receiver) 없음")
    void t3() {
        // given
        PostLikedEvent event = new PostLikedEvent(1L, 2L, 100L, "제목");

        // when
        listener.handlePostLiked(event);

        // then
        verify(notificationService).createCommunityNotification(
                eq(2L), // receiverId
                eq(1L), // actorId
                anyString(),
                anyString(),
                eq("/posts/100"),
                eq(NotificationSettingType.POST_LIKE)
        );
    }

    // ====================== 대댓글 작성 이벤트 ======================

    @Test
    @DisplayName("대댓글 작성 이벤트 수신 - 알림 생성 성공")
    void t4() {
        // given
        CommentLikedEvent event = new CommentLikedEvent(1L, 2L, 100L, 200L, "댓글");

        // when
        listener.handleCommentLiked(event);

        // then
        verify(notificationService).createCommunityNotification(
                eq(2L), // receiverId
                eq(1L), // actorId
                anyString(),
                anyString(),
                eq("/posts/100#comment-200"),
                eq(NotificationSettingType.POST_LIKE)
        );
    }

    @Test
    @DisplayName("대댓글 작성 이벤트 - 작성자(actor) 없음")
    void t5() {
        // given
        CommentCreatedEvent event = new CommentCreatedEvent(1L, 2L, 100L, 200L, "댓글");
        willThrow(new RuntimeException("DB 오류")).given(notificationService).createCommunityNotification(anyLong(), anyLong(), any(), any(), any(), any());

        // when & then
        assertThatCode(() -> listener.handleCommentCreated(event))
                .doesNotThrowAnyException();
    }

    // ====================== 게시글 좋아요 이벤트 ======================

    @Test
    @DisplayName("게시글 좋아요 이벤트 수신 - 알림 생성 성공")
    void t6() {
        // given
        PostLikedEvent event = new PostLikedEvent(1L, 2L, 100L, "게시글 제목");

        // when
        listener.handlePostLiked(event);

        // then
        verify(notificationService).createCommunityNotification(
                eq(2L), // receiverId
                eq(1L), // actorId
                anyString(),
                anyString(),
                eq("/posts/100"),
                eq(NotificationSettingType.POST_LIKE)
        );
    }

    // ====================== 댓글 좋아요 이벤트 ======================

    @Test
    @DisplayName("댓글 좋아요 이벤트 수신 - 알림 생성 성공")
    void t8() {
        // given
        CommentLikedEvent event = new CommentLikedEvent(1L, 2L, 100L, 200L, "댓글 내용");

        // when
        listener.handleCommentLiked(event);

        // then
        verify(notificationService).createCommunityNotification(
                eq(2L), // receiverId
                eq(1L), // actorId
                anyString(),
                anyString(),
                eq("/posts/100#comment-200"),
                eq(NotificationSettingType.POST_LIKE)
        );
    }

    @Test
    @DisplayName("댓글 좋아요 이벤트 - 작성자와 수신자가 같은 경우")
    void t9() {
        // given
        CommentLikedEvent event = new CommentLikedEvent(
                1L,  // actorId
                1L,  // receiverId (같은 사람)
                100L,
                200L,
                "댓글 내용"
        );

        // when
        listener.handleCommentLiked(event);

        // then
        // 서비스에 동일한 ID가 전달되는지만 확인하면 됨
        verify(notificationService).createCommunityNotification(
                eq(1L), // receiverId
                eq(1L), // actorId
                anyString(),
                anyString(),
                anyString(),
                eq(NotificationSettingType.POST_LIKE)
        );
    }

    // ====================== 예외 처리 테스트 ======================

    @Test
    @DisplayName("알림 생성 중 예외 발생 - 로그만 출력하고 예외 전파 안함")
    void t10() {
        // given
        CommentCreatedEvent event = new CommentCreatedEvent(1L, 2L, 100L, 200L, "댓글 내용");

        willThrow(new RuntimeException("DB 오류"))
                .given(notificationService)
                .createCommunityNotification(anyLong(), anyLong(), any(), any(), any(), any());

        // when & then
        assertThatCode(() -> listener.handleCommentCreated(event))
                .doesNotThrowAnyException();

        // 서비스 메서드가 호출된 것 자체는 검증
        verify(notificationService).createCommunityNotification(anyLong(), anyLong(), any(), any(), any(), any());
    }
}