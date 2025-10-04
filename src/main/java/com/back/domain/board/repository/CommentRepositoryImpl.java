package com.back.domain.board.repository;

import com.back.domain.board.dto.CommentListResponse;
import com.back.domain.board.dto.QAuthorResponse;
import com.back.domain.board.dto.QCommentListResponse;
import com.back.domain.board.entity.Comment;
import com.back.domain.board.entity.QComment;
import com.back.domain.board.entity.QCommentLike;
import com.back.domain.user.entity.QUser;
import com.back.domain.user.entity.QUserProfile;
import com.querydsl.core.types.Order;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.core.types.dsl.PathBuilder;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.*;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class CommentRepositoryImpl implements CommentRepositoryCustom {
    private final JPAQueryFactory queryFactory;

    @Override
    public Page<CommentListResponse> getCommentsByPostId(Long postId, Pageable pageable) {
        QComment comment = QComment.comment;
        QCommentLike commentLike = QCommentLike.commentLike;

        // 정렬 조건
        List<OrderSpecifier<?>> orders = buildOrderSpecifiers(pageable, comment, commentLike);

        // 부모 댓글 조회
        List<CommentListResponse> parents = fetchComments(
                comment.post.id.eq(postId).and(comment.parent.isNull()),
                orders,
                pageable.getOffset(),
                pageable.getPageSize()
        );

        if (parents.isEmpty()) {
            return new PageImpl<>(parents, pageable, 0);
        }

        // 부모 id 수집
        List<Long> parentIds = parents.stream()
                .map(CommentListResponse::getCommentId)
                .toList();

        // 자식 댓글 조회
        List<CommentListResponse> children = fetchComments(
                comment.parent.id.in(parentIds),
                List.of(comment.createdAt.asc()),
                null,
                null
        );

        // 부모 + 자식 id 합쳐서 likeCount 한 번에 조회
        List<Long> allIds = new ArrayList<>(parentIds);
        allIds.addAll(children.stream().map(CommentListResponse::getCommentId).toList());

        Map<Long, Long> likeCountMap = queryFactory
                .select(commentLike.comment.id, commentLike.count())
                .from(commentLike)
                .where(commentLike.comment.id.in(allIds))
                .groupBy(commentLike.comment.id)
                .fetch()
                .stream()
                .collect(Collectors.toMap(
                        tuple -> tuple.get(commentLike.comment.id),
                        tuple -> tuple.get(commentLike.count())
                ));

        // likeCount 세팅
        parents.forEach(p -> p.setLikeCount(likeCountMap.getOrDefault(p.getCommentId(), 0L)));
        children.forEach(c -> c.setLikeCount(likeCountMap.getOrDefault(c.getCommentId(), 0L)));

        // parentId → children 매핑
        Map<Long, List<CommentListResponse>> childMap = children.stream()
                .collect(Collectors.groupingBy(CommentListResponse::getParentId));

        parents.forEach(p ->
                p.setChildren(childMap.getOrDefault(p.getCommentId(), List.of()))
        );

        // 총 개수 (부모 댓글만 카운트)
        Long total = queryFactory
                .select(comment.count())
                .from(comment)
                .where(comment.post.id.eq(postId).and(comment.parent.isNull()))
                .fetchOne();

        return new PageImpl<>(parents, pageable, total != null ? total : 0L);
    }

    /**
     * 공통 댓글 조회 메서드 (부모/자식 공통)
     */
    private List<CommentListResponse> fetchComments(
            BooleanExpression condition,
            List<OrderSpecifier<?>> orders,
            Long offset,
            Integer limit
    ) {
        QComment comment = QComment.comment;
        QUser user = QUser.user;
        QUserProfile profile = QUserProfile.userProfile;

        var query = queryFactory
                .select(new QCommentListResponse(
                        comment.id,
                        comment.post.id,
                        comment.parent.id,
                        new QAuthorResponse(user.id, profile.nickname),
                        comment.content,
                        Expressions.constant(0L), // likeCount placeholder
                        comment.createdAt,
                        comment.updatedAt,
                        Expressions.constant(Collections.emptyList())
                ))
                .from(comment)
                .leftJoin(comment.user, user)
                .leftJoin(user.userProfile, profile)
                .where(condition)
                .orderBy(orders.toArray(new OrderSpecifier[0]));

        if (offset != null && limit != null) {
            query.offset(offset).limit(limit);
        }

        return query.fetch();
    }

    /**
     * 정렬 조건 처리
     */
    private List<OrderSpecifier<?>> buildOrderSpecifiers(Pageable pageable, QComment comment, QCommentLike commentLike) {
        PathBuilder<Comment> entityPath = new PathBuilder<>(Comment.class, comment.getMetadata());
        List<OrderSpecifier<?>> orders = new ArrayList<>();

        for (Sort.Order order : pageable.getSort()) {
            Order direction = order.isAscending() ? Order.ASC : Order.DESC;
            String prop = order.getProperty();

            switch (prop) {
                case "likeCount" -> orders.add(new OrderSpecifier<>(direction, commentLike.id.countDistinct()));
                default -> orders.add(new OrderSpecifier<>(direction,
                        entityPath.getComparable(prop, Comparable.class)));
            }
        }
        return orders;
    }
}
