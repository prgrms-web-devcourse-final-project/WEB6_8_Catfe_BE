package com.back.domain.board.repository;

import com.back.domain.board.dto.*;
import com.back.domain.board.entity.Comment;
import com.back.domain.board.entity.QComment;
import com.back.domain.board.entity.QCommentLike;
import com.back.domain.user.entity.QUser;
import com.back.domain.user.entity.QUserProfile;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Order;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.core.types.dsl.PathBuilder;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import java.util.*;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class CommentRepositoryImpl implements CommentRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    /**
     * 게시글 ID로 댓글 목록 조회
     * - 부모 댓글 페이징 + 자식 댓글 전체 조회
     * - likeCount는 부모/자식 댓글을 한 번에 조회 후 주입
     * - likeCount 정렬은 메모리에서 처리
     * - 총 쿼리 수: 4회 (부모조회 + 자식조회 + likeCount + count)
     *
     * @param postId 게시글 Id
     * @param pageable 페이징 + 정렬 조건
     */
    @Override
    public Page<CommentListResponse> getCommentsByPostId(Long postId, Pageable pageable) {
        QComment comment = QComment.comment;
        QCommentLike commentLike = QCommentLike.commentLike;

        // 1. 정렬 조건 생성 (엔티티 필드 기반)
        List<OrderSpecifier<?>> orders = buildOrderSpecifiers(pageable, comment);

        // 2. 부모 댓글 조회 (페이징)
        List<CommentListResponse> parents = fetchComments(
                comment.post.id.eq(postId).and(comment.parent.isNull()),
                orders,
                pageable.getOffset(),
                pageable.getPageSize()
        );

        if (parents.isEmpty()) {
            return new PageImpl<>(parents, pageable, 0);
        }

        // 3. 부모 ID 목록 수집
        List<Long> parentIds = parents.stream()
                .map(CommentListResponse::getCommentId)
                .toList();

        // 4. 자식 댓글 조회 (부모 ID 기준)
        List<CommentListResponse> children = fetchComments(
                comment.parent.id.in(parentIds),
                List.of(comment.createdAt.asc()),
                null,
                null
        );

        // 5. 부모 + 자식 댓글 ID 합쳐 likeCount 조회 (쿼리 1회)
        Map<Long, Long> likeCountMap = fetchLikeCounts(parentIds, children);

        // 6. likeCount 주입
        parents.forEach(p -> p.setLikeCount(likeCountMap.getOrDefault(p.getCommentId(), 0L)));
        children.forEach(c -> c.setLikeCount(likeCountMap.getOrDefault(c.getCommentId(), 0L)));

        // 7. 부모-자식 매핑
        mapChildrenToParents(parents, children);

        // 8. 정렬 후처리 (통계 필드 기반)
        parents = sortInMemoryIfNeeded(parents, pageable);

        // 9. 전체 부모 댓글 수 조회
        Long total = queryFactory
                .select(comment.count())
                .from(comment)
                .where(comment.post.id.eq(postId).and(comment.parent.isNull()))
                .fetchOne();

        return new PageImpl<>(parents, pageable, total != null ? total : 0L);
    }

    // -------------------- 내부 메서드 --------------------

    /**
     * 댓글 조회
     * - User / UserProfile join (N+1 방지)
     * - likeCount는 이후 주입
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
                        Expressions.constant(0L), // likeCount는 별도 주입
                        comment.createdAt,
                        comment.updatedAt,
                        Expressions.constant(Collections.emptyList()) // children은 별도 주입
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
     * likeCount 일괄 조회
     * - IN 조건 기반 groupBy 쿼리 1회
     * - 부모/자식 댓글을 한 번에 조회
     */
    private Map<Long, Long> fetchLikeCounts(List<Long> parentIds, List<CommentListResponse> children) {
        QCommentLike commentLike = QCommentLike.commentLike;

        List<Long> allIds = new ArrayList<>(parentIds);
        allIds.addAll(children.stream().map(CommentListResponse::getCommentId).toList());

        if (allIds.isEmpty()) return Map.of();

        return queryFactory
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
    }

    /**
     * 부모/자식 관계 매핑
     * - childMap을 parentId 기준으로 그룹화 후 children 필드에 set
     */
    private void mapChildrenToParents(List<CommentListResponse> parents, List<CommentListResponse> children) {
        if (children.isEmpty()) return;

        Map<Long, List<CommentListResponse>> childMap = children.stream()
                .collect(Collectors.groupingBy(CommentListResponse::getParentId));

        parents.forEach(parent ->
                parent.setChildren(childMap.getOrDefault(parent.getCommentId(), List.of()))
        );
    }

    /**
     * 정렬 처리 (DB 정렬)
     * - createdAt, updatedAt 등 엔티티 필드
     */
    private List<OrderSpecifier<?>> buildOrderSpecifiers(Pageable pageable, QComment comment) {
        PathBuilder<Comment> entityPath = new PathBuilder<>(Comment.class, comment.getMetadata());
        List<OrderSpecifier<?>> orders = new ArrayList<>();

        for (Sort.Order order : pageable.getSort()) {
            String prop = order.getProperty();

            // 통계 필드는 메모리 정렬에서 처리
            if (prop.equals("likeCount")) {
                continue;
            }
            Order direction = order.isAscending() ? Order.ASC : Order.DESC;
            orders.add(new OrderSpecifier<>(direction, entityPath.getComparable(prop, Comparable.class)));
        }

        return orders;
    }

    /**
     * 통계 기반 정렬 처리 (메모리)
     * - likeCount 등 통계 필드
     * - 페이지 단위라 성능에 영향 없음
     */
    private List<CommentListResponse> sortInMemoryIfNeeded(List<CommentListResponse> results, Pageable pageable) {
        if (results.isEmpty() || !pageable.getSort().isSorted()) return results;

        for (Sort.Order order : pageable.getSort()) {
            Comparator<CommentListResponse> comparator = null;

            if ("likeCount".equals(order.getProperty())) {
                comparator = Comparator.comparing(CommentListResponse::getLikeCount);
            }

            if (comparator != null) {
                if (order.isDescending()) comparator = comparator.reversed();
                results.sort(comparator);
            }
        }

        return results;
    }
}
