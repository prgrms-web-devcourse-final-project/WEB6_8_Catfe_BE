package com.back.domain.board.comment.repository.custom;

import com.back.domain.board.comment.entity.QCommentLike;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class CommentLikeRepositoryImpl implements CommentLikeRepositoryCustom {
    private final JPAQueryFactory queryFactory;

    /**
     * 댓글 ID 목록 중 사용자가 좋아요한 댓글 ID 조회
     * - 총 쿼리 수: 1회
     *
     * @param userId      사용자 ID
     * @param commentIds  댓글 ID 목록
     */
    @Override
    public List<Long> findLikedCommentIdsIn(Long userId, Collection<Long> commentIds) {
        QCommentLike commentLike = QCommentLike.commentLike;

        return queryFactory
                .select(commentLike.comment.id)
                .from(commentLike)
                .where(
                        commentLike.user.id.eq(userId),
                        commentLike.comment.id.in(commentIds)
                )
                .fetch();
    }
}