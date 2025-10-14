package com.back.domain.board.post.service;

import com.back.domain.board.post.dto.PostLikeResponse;
import com.back.domain.board.post.entity.Post;
import com.back.domain.board.post.entity.PostLike;
import com.back.domain.board.post.repository.PostLikeRepository;
import com.back.domain.board.post.repository.PostRepository;
import com.back.domain.notification.event.community.PostLikedEvent;
import com.back.domain.user.common.entity.User;
import com.back.domain.user.common.repository.UserRepository;
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
     * 게시글 좋아요 생성 서비스
     *
     * @param postId 게시글 ID
     * @param userId 사용자 ID
     * @return 게시글 좋아요 응답 DTO
     */
    public PostLikeResponse likePost(Long postId, Long userId) {
        // User 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        // Post 조회
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new CustomException(ErrorCode.POST_NOT_FOUND));

        // 이미 좋아요한 경우 예외
        if (postLikeRepository.existsByUserIdAndPostId(userId, postId)) {
            throw new CustomException(ErrorCode.POST_ALREADY_LIKED);
        }

        // PostLike 저장 및 좋아요 수 증가 처리
        post.increaseLikeCount();
        postLikeRepository.save(new PostLike(post, user));

        // 게시글 좋아요 이벤트 발행 (자기 자신의 글이 아닐 때만)
        if (!post.getUser().getId().equals(userId)) {
            eventPublisher.publishEvent(
                    new PostLikedEvent(
                            userId,                    // 좋아요한 사용자
                            post.getUser().getId(),    // 게시글 작성자
                            post.getId(),
                            post.getTitle()
                    )
            );
        }

        return PostLikeResponse.from(post);
    }

    /**
     * 게시글 좋아요 삭제 서비스
     *
     * @param postId 게시글 ID
     * @param userId 사용자 ID
     * @return 게시글 좋아요 응답 DTO
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

        // PostLike 삭제 및 좋아요 수 감소 처리
        postLike.remove();
        postLikeRepository.delete(postLike);
        post.decreaseLikeCount();

        return PostLikeResponse.from(post);
    }
}
