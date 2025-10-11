package com.back.domain.board.post.service;

import com.back.domain.board.post.dto.PostBookmarkResponse;
import com.back.domain.board.post.entity.Post;
import com.back.domain.board.post.entity.PostBookmark;
import com.back.domain.board.post.repository.PostBookmarkRepository;
import com.back.domain.board.post.repository.PostRepository;
import com.back.domain.user.entity.User;
import com.back.domain.user.repository.UserRepository;
import com.back.global.exception.CustomException;
import com.back.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class PostBookmarkService {
    private final PostRepository postRepository;
    private final PostBookmarkRepository postBookmarkRepository;
    private final UserRepository userRepository;

    /**
     * 게시글 북마크 서비스
     * 1. User 조회
     * 2. Post 조회
     * 3. 이미 존재하는 경우 예외 처리
     * 4. PostBookmark 저장 및 bookmarkCount 증가
     * 5. PostBookmarkResponse 반환
     */
    public PostBookmarkResponse bookmarkPost(Long postId, Long userId) {
        // User 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        // Post 조회
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new CustomException(ErrorCode.POST_NOT_FOUND));

        // 이미 북마크한 경우 예외
        if (postBookmarkRepository.existsByUserIdAndPostId(userId, postId)) {
            throw new CustomException(ErrorCode.BOOKMARK_ALREADY_EXISTS);
        }

        // 북마크 수 증가
        post.increaseBookmarkCount();

        // PostBookmark 저장 및 응답 반환
        postBookmarkRepository.save(new PostBookmark(post, user));
        return PostBookmarkResponse.from(post);
    }

    /**
     * 게시글 북마크 취소 서비스
     * 1. User 조회
     * 2. Post 조회
     * 3. PostBookmark 조회
     * 4. PostBookmark 삭제 및 bookmarkCount 감소
     * 5. PostBookmarkResponse 반환
     */
    public PostBookmarkResponse cancelBookmarkPost(Long postId, Long userId) {
        // User 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        // Post 조회
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new CustomException(ErrorCode.POST_NOT_FOUND));

        // PostBookmark 조회
        PostBookmark postBookmark = postBookmarkRepository.findByUserIdAndPostId(userId, postId)
                .orElseThrow(() -> new CustomException(ErrorCode.BOOKMARK_NOT_FOUND));

        // 연관관계 제거
        post.removeBookmark(postBookmark);
        user.removePostBookmark(postBookmark);

        // PostBookmark 삭제
        postBookmarkRepository.delete(postBookmark);

        // 북마크 수 감소
        post.decreaseBookmarkCount();

        // 응답 반환
        return PostBookmarkResponse.from(post);
    }
}
