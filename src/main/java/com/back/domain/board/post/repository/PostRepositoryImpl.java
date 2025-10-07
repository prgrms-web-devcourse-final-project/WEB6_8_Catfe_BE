package com.back.domain.board.post.repository;

import com.back.domain.board.comment.entity.QComment;
import com.back.domain.board.common.dto.QAuthorResponse;
import com.back.domain.board.post.dto.CategoryResponse;
import com.back.domain.board.post.dto.PostListResponse;
import com.back.domain.board.post.dto.QCategoryResponse;
import com.back.domain.board.post.dto.QPostListResponse;
import com.back.domain.board.post.entity.*;
import com.back.domain.user.entity.QUser;
import com.back.domain.user.entity.QUserProfile;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.Expression;
import com.querydsl.core.types.ExpressionUtils;
import com.querydsl.core.types.Order;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;

import java.util.*;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class PostRepositoryImpl implements PostRepositoryCustom {
    private final JPAQueryFactory queryFactory;

    /**
     * 게시글 다건 검색
     * - 총 쿼리 수 : 3회 (Post + Category + count)
     *
     * @param keyword    검색 키워드
     * @param searchType 검색 타입(title/content/author/전체)
     * @param categoryId 카테고리 ID 필터 (nullable)
     * @param pageable   페이징 + 정렬 조건
     */
    @Override
    public Page<PostListResponse> searchPosts(String keyword, String searchType, Long categoryId, Pageable pageable) {
        // 1. 검색 조건 생성
        BooleanBuilder where = buildWhere(keyword, searchType, categoryId);

        // 2. 정렬 조건 생성 (엔티티 필드 기반)
        List<OrderSpecifier<?>> orders = buildOrderSpecifiers(pageable);

        // 3. 게시글 메인 조회
        List<PostListResponse> results = fetchPosts(where, orders, pageable);

        // 4. 카테고리 조회 후 결과 DTO에 매핑
        injectCategories(results);

        // 5. 정렬 후처리 (통계 필드 기반)
        results = sortInMemoryIfNeeded(results, pageable);

        // 6. 전체 게시글 개수 카운트 쿼리
        long total = countPosts(where, categoryId);

        // 7. Page 객체로 반환
        return new PageImpl<>(results, pageable, total);
    }

    // -------------------- 내부 메서드 --------------------

    /**
     * 검색 조건 생성
     * - title/content/author 기반 동적 필터
     * - categoryId가 존재하면 categoryMapping join 기반 추가 조건
     */
    private BooleanBuilder buildWhere(String keyword, String searchType, Long categoryId) {
        QPost post = QPost.post;
        QPostCategoryMapping categoryMapping = QPostCategoryMapping.postCategoryMapping;
        BooleanBuilder where = new BooleanBuilder();

        // 키워드 필터링
        if (keyword != null && !keyword.isBlank()) {
            switch (searchType) {
                case "title" -> where.and(post.title.containsIgnoreCase(keyword));
                case "content" -> where.and(post.content.containsIgnoreCase(keyword));
                case "author" -> where.and(post.user.username.containsIgnoreCase(keyword));
                default -> where.and(
                        post.title.containsIgnoreCase(keyword)
                                .or(post.content.containsIgnoreCase(keyword))
                                .or(post.user.username.containsIgnoreCase(keyword))
                );
            }
        }

        // 카테고리 필터링
        if (categoryId != null) {
            where.and(categoryMapping.category.id.eq(categoryId));
        }

        return where;
    }

    /**
     * 정렬 처리 (DB 정렬)
     * - title, createdAt, updatedAt 등 엔티티 필드
     */
    private List<OrderSpecifier<?>> buildOrderSpecifiers(Pageable pageable) {
        QPost post = QPost.post;
        List<OrderSpecifier<?>> orders = new ArrayList<>();
        var entityPath = new com.querydsl.core.types.dsl.PathBuilder<>(Post.class, post.getMetadata());

        for (Sort.Order order : pageable.getSort()) {
            String prop = order.getProperty();

            // 통계 필드는 메모리 정렬에서 처리
            if (prop.equals("likeCount") || prop.equals("bookmarkCount") || prop.equals("commentCount")) {
                continue;
            }

            Order direction = order.isAscending() ? Order.ASC : Order.DESC;
            orders.add(new OrderSpecifier<>(direction, entityPath.getComparable(prop, Comparable.class)));
        }
        return orders;
    }

    /**
     * 게시글 조회
     * - Post + User + UserProfile join (N+1 방지)
     * - like/bookmark/comment count는 각각 서브쿼리로 계산
     *   → 한 번의 SQL 안에서 처리 (쿼리 1회)
     */
    private List<PostListResponse> fetchPosts(BooleanBuilder where, List<OrderSpecifier<?>> orders, Pageable pageable) {
        QPost post = QPost.post;
        QUser user = QUser.user;
        QUserProfile profile = QUserProfile.userProfile;
        QPostLike like = QPostLike.postLike;
        QPostBookmark bookmark = QPostBookmark.postBookmark;
        QComment comment = QComment.comment;

        // 서브쿼리로 통계 계산
        Expression<Long> likeCount = ExpressionUtils.as(
                JPAExpressions.select(like.count())
                        .from(like)
                        .where(like.post.eq(post)),
                "likeCount"
        );

        Expression<Long> bookmarkCount = ExpressionUtils.as(
                JPAExpressions.select(bookmark.count())
                        .from(bookmark)
                        .where(bookmark.post.eq(post)),
                "bookmarkCount"
        );

        Expression<Long> commentCount = ExpressionUtils.as(
                JPAExpressions.select(comment.count())
                        .from(comment)
                        .where(comment.post.eq(post)),
                "commentCount"
        );

        // 메인 쿼리
        return queryFactory
                .select(new QPostListResponse(
                        post.id,
                        new QAuthorResponse(user.id, profile.nickname), // 작성자 정보 (N+1 방지 join)
                        post.title,
                        Expressions.constant(Collections.emptyList()), // categories는 별도 주입
                        likeCount,
                        bookmarkCount,
                        commentCount,
                        post.createdAt,
                        post.updatedAt
                ))
                .from(post)
                .leftJoin(post.user, user)
                .leftJoin(user.userProfile, profile) // UserProfile join으로 N+1 방지
                .where(where)
                .orderBy(orders.toArray(new OrderSpecifier[0]))
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch(); // 쿼리 1회 (서브쿼리 포함)
    }

    /**
     * 카테고리 일괄 조회
     * - Post ID 목록 기반으로 categoryMapping 테이블 IN 쿼리 1회 실행
     * - Map<postId, List<CategoryResponse>>로 매핑 후 DTO에 주입
     * - N+1 방지 (게시글별 조회 X)
     */
    private void injectCategories(List<PostListResponse> results) {
        if (results.isEmpty()) return;

        QPostCategoryMapping categoryMapping = QPostCategoryMapping.postCategoryMapping;

        // postId 목록 생성
        List<Long> postIds = results.stream()
                .map(PostListResponse::getPostId)
                .toList();

        // 해당하는 카테고리 정보 조회
        List<Tuple> categoryTuples = queryFactory
                .select(
                        categoryMapping.post.id,
                        new QCategoryResponse(
                                categoryMapping.category.id,
                                categoryMapping.category.name,
                                categoryMapping.category.type
                        )
                )
                .from(categoryMapping)
                .where(categoryMapping.post.id.in(postIds))
                .fetch();

        // 매핑 편의를 위해 변환
        Map<Long, List<CategoryResponse>> categoryMap = categoryTuples.stream()
                .collect(Collectors.groupingBy(
                        tuple -> Objects.requireNonNull(tuple.get(categoryMapping.post.id)),
                        Collectors.mapping(t -> t.get(1, CategoryResponse.class), Collectors.toList())
                ));

        // categories 주입
        results.forEach(r ->
                r.setCategories(categoryMap.getOrDefault(r.getPostId(), List.of()))
        );
    }

    /**
     * 통계 기반 정렬 처리 (메모리)
     * - likeCount / bookmarkCount / commentCount 등 통계 필드
     * - DB에서는 서브쿼리 필드 정렬 불가 → Java 단에서 정렬
     * - 데이터량이 페이지 단위(20~50건)라면 CPU 부하는 무시 가능
     */
    private List<PostListResponse> sortInMemoryIfNeeded(List<PostListResponse> results, Pageable pageable) {
        if (results.isEmpty() || !pageable.getSort().isSorted()) return results;

        for (Sort.Order order : pageable.getSort()) {
            Comparator<PostListResponse> comparator = null;
            switch (order.getProperty()) {
                case "likeCount" -> comparator = Comparator.comparing(PostListResponse::getLikeCount);
                case "bookmarkCount" -> comparator = Comparator.comparing(PostListResponse::getBookmarkCount);
                case "commentCount" -> comparator = Comparator.comparing(PostListResponse::getCommentCount);
            }
            if (comparator != null) {
                if (order.isDescending()) comparator = comparator.reversed();
                results.sort(comparator);
            }
        }
        return results;
    }

    /**
     * 전체 게시글 개수 카운트
     * - 페이지네이션 total 계산용
     * - categoryId가 있으면 mapping join 포함
     * - 단순 count 쿼리 1회 실행
     */
    private long countPosts(BooleanBuilder where, Long categoryId) {
        QPost post = QPost.post;
        QPostCategoryMapping categoryMapping = QPostCategoryMapping.postCategoryMapping;

        // 카운트 쿼리
        JPAQuery<Long> countQuery = queryFactory
                .select(post.countDistinct())
                .from(post);

        // 카테고리 필터링
        if (categoryId != null) {
            countQuery.leftJoin(post.postCategoryMappings, categoryMapping);
        }

        Long total = countQuery.where(where).fetchOne();
        return total != null ? total : 0L;
    }
}