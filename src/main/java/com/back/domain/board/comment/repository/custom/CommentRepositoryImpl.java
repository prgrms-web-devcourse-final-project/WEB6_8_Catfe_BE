package com.back.domain.board.comment.repository.custom;

import com.back.domain.board.comment.dto.CommentListResponse;
import com.back.domain.board.comment.dto.MyCommentResponse;
import com.back.domain.board.comment.dto.QCommentListResponse;
import com.back.domain.board.comment.dto.QMyCommentResponse;
import com.back.domain.board.comment.entity.Comment;
import com.back.domain.board.comment.entity.QComment;
import com.back.domain.board.common.dto.QAuthorResponse;
import com.back.domain.board.post.entity.QPost;
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
import org.springframework.stereotype.Repository;

import java.util.*;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class CommentRepositoryImpl implements CommentRepositoryCustom {
    private final JPAQueryFactory queryFactory;

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of("createdAt", "updatedAt", "likeCount");

    /**
     * 특정 게시글의 댓글 목록 조회
     * - 총 쿼리 수: 3회
     *  1.부모 댓글 목록 조회 (User, UserProfile join)
     *  2.자식 댓글 목록 조회 (User, UserProfile join)
     *  3.부모 전체 count 조회
     *
     * @param postId   게시글 ID
     * @param pageable 페이징 + 정렬 조건
     */
    @Override
    public Page<CommentListResponse> findCommentsByPostId(Long postId, Pageable pageable) {
        QComment comment = QComment.comment;

        // 1. 검색 조건 생성
        BooleanExpression condition = comment.post.id.eq(postId).and(comment.parent.isNull());

        // 2. 정렬 조건 생성
        List<OrderSpecifier<?>> orders = buildOrderSpecifiers(pageable);

        // 3. 부모 댓글 조회 (페이징 적용)
        List<CommentListResponse> parents = fetchComments(
                condition,
                orders,
                pageable.getOffset(),
                pageable.getPageSize()
        );

        // 부모가 비어 있으면 즉시 빈 페이지 반환
        if (parents.isEmpty()) {
            return new PageImpl<>(parents, pageable, 0);
        }

        // 4. 부모 ID 수집
        List<Long> parentIds = parents.stream()
                .map(CommentListResponse::getCommentId)
                .toList();

        // 5. 자식 댓글 조회 (부모 집합에 대한 전체 조회)
        List<CommentListResponse> children = fetchComments(
                comment.parent.id.in(parentIds),
                List.of(comment.createdAt.asc()),   // 시간순 정렬
                null,
                null
        );

        // 6. 부모-자식 매핑
        mapChildrenToParents(parents, children);

        // 7. 전체 부모 댓글 수 조회
        long total = countComments(condition);

        return new PageImpl<>(parents, pageable, total);
    }

    /**
     * 특정 사용자의 댓글 목록 조회
     * - 총 쿼리 수: 2회
     *  1. 댓글 목록 조회 (Comment, Post join)
     *  2. 전체 count 조회
     *
     * @param userId   사용자 ID
     * @param pageable 페이징 + 정렬 조건
     */
    @Override
    public Page<MyCommentResponse> findCommentsByUserId(Long userId, Pageable pageable) {
        QComment comment = QComment.comment;
        QComment parent = new QComment("parent");
        QPost post = QPost.post;

        // 1. 검색 조건 생성
        BooleanExpression condition = comment.user.id.eq(userId);

        // 2. 정렬 조건 생성
        List<OrderSpecifier<?>> orders = buildOrderSpecifiers(pageable);

        // 3. 댓글 목록 조회
        List<MyCommentResponse> comments = queryFactory
                .select(new QMyCommentResponse(
                        comment.id,
                        post.id,
                        post.title,
                        parent.id,
                        parent.content.substring(0, 50),
                        comment.content,
                        comment.likeCount,
                        comment.createdAt,
                        comment.updatedAt
                ))
                .from(comment)
                .leftJoin(comment.parent, parent)
                .leftJoin(comment.post, post)
                .where(condition)
                .orderBy(orders.toArray(new OrderSpecifier[0]))
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        // 결과가 없으면 즉시 빈 페이지 반환
        if (comments.isEmpty()) {
            return new PageImpl<>(comments, pageable, 0);
        }

        // 4. 전체 댓글 수 조회
        long total = countComments(condition);

        return new PageImpl<>(comments, pageable, total);
    }

    // -------------------- 내부 메서드 --------------------

    /**
     * 정렬 조건 생성
     * - Pageable의 Sort 정보를 QueryDSL OrderSpecifier 목록으로 변환
     */
    private List<OrderSpecifier<?>> buildOrderSpecifiers(Pageable pageable) {
        QComment comment = QComment.comment;
        PathBuilder<Comment> entityPath = new PathBuilder<>(Comment.class, comment.getMetadata());
        List<OrderSpecifier<?>> orders = new ArrayList<>();

        for (Sort.Order order : pageable.getSort()) {
            String property = order.getProperty();

            // 화이트리스트에 포함된 필드만 허용
            if (!ALLOWED_SORT_FIELDS.contains(property)) {
                // 허용되지 않은 정렬 키는 무시 (런타임 예외 대신 안전하게 스킵)
                continue;
            }

            Order direction = order.isAscending() ? Order.ASC : Order.DESC;
            orders.add(new OrderSpecifier<>(direction, entityPath.getComparable(property, Comparable.class)));
        }

        // 명시된 정렬이 없으면 기본 정렬(createdAt DESC) 적용
        if (orders.isEmpty()) {
            orders.add(new OrderSpecifier<>(Order.DESC, comment.createdAt));
        }

        return orders;
    }

    /**
     * 댓글 조회
     * - User / UserProfile join (N+1 방지)
     */
    private List<CommentListResponse> fetchComments(
            BooleanExpression condition,
            List<OrderSpecifier<?>> orders,
            Long offset,
            Integer limit
    ) {
        QComment comment = QComment.comment;
        QUser user = QUser.user;
        QUserProfile profile = QUserProfile .userProfile;

        var query = queryFactory
                .select(new QCommentListResponse(
                        comment.id,
                        comment.post.id,
                        comment.parent.id,
                        new QAuthorResponse(user.id, profile.nickname, profile.profileImageUrl),
                        comment.content,
                        comment.likeCount,
                        Expressions.constant(false), // likedByMe는 별도 주입
                        comment.createdAt,
                        comment.updatedAt,
                        Expressions.constant(Collections.emptyList()) // children은 별도 주입
                ))
                .from(comment)
                .leftJoin(comment.user, user)
                .leftJoin(user.userProfile, profile)
                .where(condition)
                .orderBy(orders.toArray(new OrderSpecifier[0]));

        // 페이징 적용
        if (offset != null && limit != null) {
            query.offset(offset).limit(limit);
        }

        return query.fetch();
    }

    /**
     * 부모/자식 관계 매핑
     * - 자식 목록을 parentId 기준으로 그룹화 후, 각 부모 DTO의 children에 설정
     */
    private void mapChildrenToParents(List<CommentListResponse> parents, List<CommentListResponse> children) {
        if (children.isEmpty()) return;

        Map<Long, List<CommentListResponse>> childMap = children.stream()
                .collect(Collectors.groupingBy(CommentListResponse::getParentId));

        parents.forEach(parent ->
                parent.setChildren(childMap.getOrDefault(parent.getCommentId(), Collections.emptyList()))
        );
    }

    /**
     * 전체 댓글 개수 조회
     * - 단순 count 쿼리 1회
     */
    private long countComments(BooleanExpression condition) {
        QComment comment = QComment.comment;
        Long total = queryFactory
                .select(comment.count())
                .from(comment)
                .where(condition)
                .fetchOne();
        return total != null ? total : 0L;
    }
}
