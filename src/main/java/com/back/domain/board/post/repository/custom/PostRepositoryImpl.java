package com.back.domain.board.post.repository.custom;

import com.back.domain.board.common.dto.QAuthorResponse;
import com.back.domain.board.post.dto.CategoryResponse;
import com.back.domain.board.post.dto.PostListResponse;
import com.back.domain.board.post.dto.QCategoryResponse;
import com.back.domain.board.post.dto.QPostListResponse;
import com.back.domain.board.post.entity.*;
import com.back.domain.board.post.enums.CategoryType;
import com.back.domain.user.entity.QUser;
import com.back.domain.user.entity.QUserProfile;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.Order;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.core.types.dsl.PathBuilder;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Repository;

import java.util.*;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class PostRepositoryImpl implements PostRepositoryCustom {
    private final JPAQueryFactory queryFactory;

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
            "createdAt", "updatedAt", "title", "likeCount", "bookmarkCount", "commentCount"
    );

    /**
     * 게시글 다건 검색
     * - 총 쿼리 수: 3회
     *  1. 게시글 목록 조회 (User, UserProfile join)
     *  2. 카테고리 목록 조회 (IN 쿼리)
     *  3. 전체 count 조회
     * - categoryIds 포함 시, 총 쿼리 수: 4회
     *  4. CategoryType 매핑 조회 추가 (buildWhere 내부)
     *
     * @param keyword     검색 키워드
     * @param searchType  검색 유형(title/content/author/전체)
     * @param categoryIds 카테고리 ID 리스트 (같은 타입은 OR, 다른 타입은 AND)
     * @param pageable    페이징 + 정렬 조건
     */
    @Override
    public Page<PostListResponse> searchPosts(String keyword, String searchType, List<Long> categoryIds, Pageable pageable) {
        // 1. 검색 조건 생성
        BooleanBuilder where = buildWhere(keyword, searchType, categoryIds);

        // 2. 정렬 조건 생성 (화이트리스트 기반)
        List<OrderSpecifier<?>> orders = buildOrderSpecifiers(pageable);

        // 3. 게시글 목록 조회 (User, UserProfile join으로 N+1 방지)
        List<PostListResponse> posts = fetchPosts(where, orders, pageable);

        // 결과가 없으면 즉시 빈 페이지 반환
        if (posts.isEmpty()) {
            return new PageImpl<>(posts, pageable, 0);
        }

        // 4. 카테고리 목록 주입 (postIds 기반 IN 쿼리 1회)
        injectCategories(posts);

        // 5. 전체 게시글 수 조회
        long total = countPosts(where);

        return new PageImpl<>(posts, pageable, total);
    }

    /**
     * 내 게시글 목록 조회
     * - 총 쿼리 수: 3회
     *  1. 게시글 목록 조회 (User, UserProfile join)
     *  2. 카테고리 목록 조회 (IN 쿼리)
     *  3. 전체 count 조회
     *
     * @param userId   사용자 ID
     * @param pageable 페이징 + 정렬 조건
     */
    @Override
    public Page<PostListResponse> findPostsByUserId(Long userId, Pageable pageable) {
        QPost post = QPost.post;

        // 1. 검색 조건 생성
        BooleanBuilder where = new BooleanBuilder(post.user.id.eq(userId));

        // 2. 정렬 조건 생성 (화이트리스트 기반)
        List<OrderSpecifier<?>> orders = buildOrderSpecifiers(pageable);

        // 3. 게시글 목록 조회 (User, UserProfile join으로 N+1 방지)
        List<PostListResponse> posts = fetchPosts(where, orders, pageable);

        // 결과가 없으면 즉시 빈 페이지 반환
        if (posts.isEmpty()) {
            return new PageImpl<>(posts, pageable, 0);
        }

        // 4. 카테고리 목록 주입 (postIds 기반 IN 쿼리 1회)
        injectCategories(posts);

        // 5. 전체 게시글 수 조회
        long total = countPosts(where);

        return new PageImpl<>(posts, pageable, total);
    }

    // -------------------- 내부 메서드 --------------------

    /**
     * 검색 조건 생성
     * - keyword, searchType, categoryIds를 기반으로 BooleanBuilder 구성
     * - 카테고리 조건 로직:
     * 1. categoryIds → CategoryType 매핑 조회 (1회 쿼리)
     * 2. 같은 CategoryType끼리는 OR (in 조건)
     * 3. 서로 다른 CategoryType끼리는 AND 결합
     * - 결과적으로 `(SUBJECT in (...)) AND (DEMOGRAPHIC in (...))` 형태로 조합됨
     */
    private BooleanBuilder buildWhere(String keyword, String searchType, List<Long> categoryIds) {
        QPost post = QPost.post;
        QPostCategory category = QPostCategory.postCategory;
        QPostCategoryMapping categoryMapping = QPostCategoryMapping.postCategoryMapping;
        BooleanBuilder where = new BooleanBuilder();

        // 키워드 필터
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

        // 카테고리 필터
        if (categoryIds != null && !categoryIds.isEmpty()) {
            // categoryId -> CategoryType 매핑 조회 (1회 쿼리)
            List<Tuple> categoryTypeTuples = queryFactory
                    .select(category.id, category.type)
                    .from(category)
                    .where(category.id.in(categoryIds))
                    .fetch();

            // 타입별 그룹핑
            Map<CategoryType, List<Long>> groupedIds = categoryTypeTuples.stream()
                    .filter(t -> t.get(category.id) != null && t.get(category.type) != null)
                    .collect(Collectors.groupingBy(
                            t -> t.get(category.type),
                            Collectors.mapping(t -> t.get(category.id), Collectors.toList())
                    ));

            // 같은 타입은 OR(in), 다른 타입은 AND로 결합
            BooleanBuilder typeConditions = new BooleanBuilder();
            groupedIds.forEach((type, ids) -> {
                // 각 타입별로 (category_id in ids)
                BooleanBuilder subCondition = new BooleanBuilder(categoryMapping.category.id.in(ids));

                // post.id in (select mapping.post.id ...)
                typeConditions.and(post.id.in(
                        JPAExpressions.select(categoryMapping.post.id)
                                .from(categoryMapping)
                                .where(subCondition)
                ));
            });

            where.and(typeConditions);
        }

        return where;
    }

    /**
     * 정렬 조건 생성
     * - ALLOWED_SORT_FIELDS에 포함된 필드만 변환
     * - 정렬 지정이 없으면 createdAt DESC 기본 적용
     */
    private List<OrderSpecifier<?>> buildOrderSpecifiers(Pageable pageable) {
        QPost post = QPost.post;
        PathBuilder<Post> entityPath = new PathBuilder<>(Post.class, post.getMetadata());
        List<OrderSpecifier<?>> orders = new ArrayList<>();

        for (Sort.Order order : pageable.getSort()) {
            String property = order.getProperty();
            if (!ALLOWED_SORT_FIELDS.contains(property)) continue;

            Order direction = order.isAscending() ? Order.ASC : Order.DESC;
            orders.add(new OrderSpecifier<>(direction, entityPath.getComparable(property, Comparable.class)));
        }

        // 기본 정렬(createdAt DESC)
        if (orders.isEmpty()) {
            orders.add(new OrderSpecifier<>(Order.DESC, post.createdAt));
        }

        return orders;
    }

    /**
     * 게시글 조회
     * - Post + User + UserProfile join (N+1 방지)
     * - 카테고리 정보는 별도 injectCategories()에서 주입
     */
    private List<PostListResponse> fetchPosts(BooleanBuilder where, List<OrderSpecifier<?>> orders, Pageable pageable) {
        QPost post = QPost.post;
        QUser user = QUser.user;
        QUserProfile profile = QUserProfile.userProfile;

        return queryFactory
                .select(new QPostListResponse(
                        post.id,
                        new QAuthorResponse(user.id, profile.nickname, profile.profileImageUrl),
                        post.title,
                        post.thumbnailUrl,
                        Expressions.constant(Collections.emptyList()), // categories는 별도 주입
                        post.likeCount,
                        post.bookmarkCount,
                        post.commentCount,
                        post.createdAt,
                        post.updatedAt
                ))
                .from(post)
                .leftJoin(post.user, user)
                .leftJoin(user.userProfile, profile)
                .where(where)
                .orderBy(orders.toArray(new OrderSpecifier[0]))
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();
    }

    /**
     * 카테고리 일괄 조회
     * - postIds 기반 IN 쿼리 (1회)
     * - N+1 방지
     * - Map<postId, List<CategoryResponse>>로 그룹핑 후 DTO에 매핑
     */
    private void injectCategories(List<PostListResponse> results) {
        QPostCategoryMapping mapping = QPostCategoryMapping.postCategoryMapping;

        List<Long> postIds = results.stream()
                .map(PostListResponse::getPostId)
                .toList();

        List<Tuple> tuples = queryFactory
                .select(
                        mapping.post.id,
                        new QCategoryResponse(
                                mapping.category.id,
                                mapping.category.name,
                                mapping.category.type
                        )
                )
                .from(mapping)
                .where(mapping.post.id.in(postIds))
                .fetch();

        Map<Long, List<CategoryResponse>> categoryMap = tuples.stream()
                .collect(Collectors.groupingBy(
                        t -> Objects.requireNonNull(t.get(mapping.post.id)),
                        Collectors.mapping(t -> t.get(1, CategoryResponse.class), Collectors.toList())
                ));

        results.forEach(post ->
                post.setCategories(categoryMap.getOrDefault(post.getPostId(), List.of()))
        );
    }

    /**
     * 전체 게시글 개수 조회
     * - 단순 count 쿼리 1회
     * - category 조합 조건 포함 시 중복 방지를 위해 countDistinct() 사용
     */
    private long countPosts(BooleanBuilder where) {
        QPost post = QPost.post;
        Long total = queryFactory
                .select(post.countDistinct())
                .from(post)
                .where(where)
                .fetchOne();
        return total != null ? total : 0L;
    }
}