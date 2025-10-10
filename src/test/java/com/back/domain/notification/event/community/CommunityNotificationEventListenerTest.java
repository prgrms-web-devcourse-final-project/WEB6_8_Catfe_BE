package com.back.domain.notification.event.community;

import com.back.domain.notification.entity.NotificationSettingType;
import com.back.domain.notification.service.NotificationService;
import com.back.domain.user.entity.User;
import com.back.domain.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CommunityNotificationEventListener 테스트")
class CommunityNotificationEventListenerTest {

    @Mock
    private NotificationService notificationService;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CommunityNotificationEventListener listener;

    private User actor;
    private User receiver;

    @BeforeEach
    void setUp() {
        actor = User.builder()
                .username("actor")
                .email("actor@test.com")
                .build();

        receiver = User.builder()
                .username("receiver")
                .email("receiver@test.com")
                .build();
    }

    // ====================== 댓글 작성 이벤트 ======================

    @Test
    @DisplayName("댓글 작성 이벤트 수신 - 알림 생성 성공")
    void t1() {
        // given
        CommentCreatedEvent event = new CommentCreatedEvent(
                1L,  // actorId (댓글 작성자)
                2L,  // receiverId (게시글 작성자)
                100L, // postId
                200L, // commentId
                "댓글 내용"
        );

        given(userRepository.findById(1L)).willReturn(Optional.of(actor));
        given(userRepository.findById(2L)).willReturn(Optional.of(receiver));

        // when
        listener.handleCommentCreated(event);

        // then
        verify(notificationService).createCommunityNotification(
                eq(receiver),
                eq(actor),
                anyString(), // title
                anyString(), // content
                eq("/posts/100"),
                eq(NotificationSettingType.POST_COMMENT)
        );
    }

    @Test
    @DisplayName("댓글 작성 이벤트 - 작성자(actor) 없음")
    void t2() {
        // given
        CommentCreatedEvent event = new CommentCreatedEvent(
                999L, // 존재하지 않는 actorId
                2L,
                100L,
                200L,
                "댓글 내용"
        );

        given(userRepository.findById(999L)).willReturn(Optional.empty());

        // when
        listener.handleCommentCreated(event);

        // then
        verify(notificationService, never()).createCommunityNotification(
                any(), any(), anyString(), anyString(), anyString(), any(NotificationSettingType.class)
        );
    }

    @Test
    @DisplayName("댓글 작성 이벤트 - 수신자(receiver) 없음")
    void t3() {
        // given
        CommentCreatedEvent event = new CommentCreatedEvent(
                1L,
                999L, // 존재하지 않는 receiverId
                100L,
                200L,
                "댓글 내용"
        );

        given(userRepository.findById(1L)).willReturn(Optional.of(actor));
        given(userRepository.findById(999L)).willReturn(Optional.empty());

        // when
        listener.handleCommentCreated(event);

        // then
        verify(notificationService, never()).createCommunityNotification(
                any(), any(), anyString(), anyString(), anyString(), any(NotificationSettingType.class)
        );
    }

    // ====================== 대댓글 작성 이벤트 ======================

    @Test
    @DisplayName("대댓글 작성 이벤트 수신 - 알림 생성 성공")
    void t4() {
        // given
        ReplyCreatedEvent event = new ReplyCreatedEvent(
                1L,  // actorId (대댓글 작성자)
                2L,  // receiverId (댓글 작성자)
                100L, // postId
                200L, // parentCommentId
                300L, // replyId
                "대댓글 내용"
        );

        given(userRepository.findById(1L)).willReturn(Optional.of(actor));
        given(userRepository.findById(2L)).willReturn(Optional.of(receiver));

        // when
        listener.handleReplyCreated(event);

        // then
        verify(notificationService).createCommunityNotification(
                eq(receiver),
                eq(actor),
                anyString(),
                anyString(),
                eq("/posts/100#comment-200"),
                eq(NotificationSettingType.POST_COMMENT)
        );
    }

    @Test
    @DisplayName("대댓글 작성 이벤트 - 작성자(actor) 없음")
    void t5() {
        // given
        ReplyCreatedEvent event = new ReplyCreatedEvent(
                999L, // 존재하지 않는 actorId
                2L,
                100L,
                200L,
                300L,
                "대댓글 내용"
        );

        given(userRepository.findById(999L)).willReturn(Optional.empty());

        // when
        listener.handleReplyCreated(event);

        // then
        verify(notificationService, never()).createCommunityNotification(
                any(), any(), anyString(), anyString(), anyString(), any(NotificationSettingType.class)
        );
    }

    // ====================== 게시글 좋아요 이벤트 ======================

    @Test
    @DisplayName("게시글 좋아요 이벤트 수신 - 알림 생성 성공")
    void t6() {
        // given
        PostLikedEvent event = new PostLikedEvent(
                1L,  // actorId (좋아요 누른 사람)
                2L,  // receiverId (게시글 작성자)
                100L, // postId
                "게시글 제목"
        );

        given(userRepository.findById(1L)).willReturn(Optional.of(actor));
        given(userRepository.findById(2L)).willReturn(Optional.of(receiver));

        // when
        listener.handlePostLiked(event);

        // then
        verify(notificationService).createCommunityNotification(
                eq(receiver),
                eq(actor),
                anyString(),
                anyString(),
                eq("/posts/100"),
                eq(NotificationSettingType.POST_LIKE)
        );
    }

    @Test
    @DisplayName("게시글 좋아요 이벤트 - 수신자 없음")
    void t7() {
        // given
        PostLikedEvent event = new PostLikedEvent(
                1L,
                999L, // 존재하지 않는 receiverId
                100L,
                "게시글 제목"
        );

        given(userRepository.findById(1L)).willReturn(Optional.of(actor));
        given(userRepository.findById(999L)).willReturn(Optional.empty());

        // when
        listener.handlePostLiked(event);

        // then
        verify(notificationService, never()).createCommunityNotification(
                any(), any(), anyString(), anyString(), anyString(), any(NotificationSettingType.class)
        );
    }

    // ====================== 댓글 좋아요 이벤트 ======================

    @Test
    @DisplayName("댓글 좋아요 이벤트 수신 - 알림 생성 성공")
    void t8() {
        // given
        CommentLikedEvent event = new CommentLikedEvent(
                1L,  // actorId
                2L,  // receiverId
                100L, // postId
                200L, // commentId,
                "댓글 내용"
        );

        given(userRepository.findById(1L)).willReturn(Optional.of(actor));
        given(userRepository.findById(2L)).willReturn(Optional.of(receiver));

        // when
        listener.handleCommentLiked(event);

        // then
        verify(notificationService).createCommunityNotification(
                eq(receiver),
                eq(actor),
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

        given(userRepository.findById(1L)).willReturn(Optional.of(actor));

        // when
        listener.handleCommentLiked(event);

        // then
        // NotificationService에서 자기 자신 알림 필터링 처리
        verify(notificationService).createCommunityNotification(
                eq(actor), // receiver
                eq(actor), // actor
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
        CommentCreatedEvent event = new CommentCreatedEvent(
                1L, 2L, 100L, 200L, "댓글 내용"
        );

        given(userRepository.findById(1L)).willReturn(Optional.of(actor));
        given(userRepository.findById(2L)).willReturn(Optional.of(receiver));

        willThrow(new RuntimeException("알림 생성 실패"))
                .given(notificationService).createCommunityNotification(
                        any(), any(), anyString(), anyString(), anyString(), any(NotificationSettingType.class)
                );

        // when & then - 예외가 전파되지 않아야 함
        assertThatCode(() -> listener.handleCommentCreated(event))
                .doesNotThrowAnyException();

        verify(notificationService).createCommunityNotification(
                any(), any(), anyString(), anyString(), anyString(), any(NotificationSettingType.class)
        );
    }
}