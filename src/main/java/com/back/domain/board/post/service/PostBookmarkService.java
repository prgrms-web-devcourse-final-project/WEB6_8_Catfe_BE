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
     *
     * @param postId 게시글 ID
     * @param userId 사용자 ID
     * @return 게시글 북마크 응답 DTO
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

        // PostBookmark 저장 및 북마크 수 증가 처리
        post.increaseBookmarkCount();
        postBookmarkRepository.save(new PostBookmark(post, user));

        return PostBookmarkResponse.from(post);
    }

    /**
     * 게시글 북마크 취소 서비스
     *
     * @param postId 게시글 ID
     * @param userId 사용자 ID
     * @return 게시글 북마크 응답 DTO
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

        // PostBookmark 삭제 및 북마크 수 감소 처리
        postBookmark.remove();
        postBookmarkRepository.delete(postBookmark);
        post.decreaseBookmarkCount();

        return PostBookmarkResponse.from(post);
    }
}
