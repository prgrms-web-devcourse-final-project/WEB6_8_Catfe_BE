package com.back.domain.board.comment.repository.custom;

import com.back.domain.board.comment.entity.QCommentLike;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;


@Repository
@RequiredArgsConstructor
public class CommentLikeRepositoryImpl implements CommentLikeRepositoryCustom {

    private final JPAQueryFactory queryFactory;

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