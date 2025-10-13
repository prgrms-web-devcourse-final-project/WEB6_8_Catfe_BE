package com.back.domain.board.post.service;

import com.back.domain.board.post.dto.PostLikeResponse;
import com.back.domain.board.post.entity.Post;
import com.back.domain.board.post.entity.PostLike;
import com.back.domain.board.post.repository.PostLikeRepository;
import com.back.domain.board.post.repository.PostRepository;
import com.back.domain.notification.event.community.PostLikedEvent;
import com.back.domain.user.entity.User;
import com.back.domain.user.repository.UserRepository;
import com.back.global.exception.CustomException;
import com.back.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class PostLikeService {
    private final PostRepository postRepository;
    private final PostLikeRepository postLikeRepository;
    private final UserRepository userRepository;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * 게시글 좋아요 서비스
     * 1. User 조회
     * 2. Post 조회
     * 3. 이미 존재하는 경우 예외 처리
     * 4. PostLike 저장 및 likeCount 증가
     * 5. 알림 이벤트 발행 (자기 글이 아닐 경우)
     * 6. PostLikeResponse 반환
     */
    public PostLikeResponse likePost(Long postId, Long userId) {
        // User 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        // Post 조회
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new CustomException(ErrorCode.POST_NOT_FOUND));

        // 이미 좋아요를 누른 경우 예외
        if (postLikeRepository.existsByUserIdAndPostId(userId, postId)) {
            throw new CustomException(ErrorCode.POST_ALREADY_LIKED);
        }

        // 좋아요 수 증가
        post.increaseLikeCount();

        // PostLike 저장 및 응답 반환
        postLikeRepository.save(new PostLike(post, user));

        // 알림 이벤트 발행 (자기 자신의 글이 아닐 때만)
        if (!post.getUser().getId().equals(userId)) {
            eventPublisher.publishEvent(
                    new PostLikedEvent(
                            userId,                    // 좋아요 누른 사람
                            post.getUser().getId(),    // 게시글 작성자
                            post.getId(),
                            post.getTitle()
                    )
            );
        }

        return PostLikeResponse.from(post);
    }

    /**
     * 게시글 좋아요 취소 서비스
     * 1. User 조회
     * 2. Post 조회
     * 3. PostLike 조회
     * 4. PostLike 삭제 및 likeCount 감소
     * 5. PostLikeResponse 반환
     */
    public PostLikeResponse cancelLikePost(Long postId, Long userId) {
        // User 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        // Post 조회
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new CustomException(ErrorCode.POST_NOT_FOUND));

        // PostLike 조회
        PostLike postLike = postLikeRepository.findByUserIdAndPostId(userId, postId)
                .orElseThrow(() -> new CustomException(ErrorCode.POST_LIKE_NOT_FOUND));

        // 연관관계 제거
        post.removeLike(postLike);
        user.removePostLike(postLike);

        // PostLike 삭제
        postLikeRepository.delete(postLike);

        // 좋아요 수 감소
        post.decreaseLikeCount();

        // 응답 반환
        return PostLikeResponse.from(post);
    }
}
