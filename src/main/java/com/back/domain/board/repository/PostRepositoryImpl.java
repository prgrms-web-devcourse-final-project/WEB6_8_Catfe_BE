package com.back.domain.board.repository;

import com.back.domain.board.dto.PostListResponse;
import com.back.domain.board.dto.QPostListResponse;
import com.back.domain.board.dto.QPostListResponse_AuthorResponse;
import com.back.domain.board.dto.QPostListResponse_CategoryResponse;
import com.back.domain.board.entity.*;
import com.back.domain.user.entity.QUser;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.Order;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.core.types.dsl.PathBuilder;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.*;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class PostRepositoryImpl implements PostRepositoryCustom {
    private final JPAQueryFactory queryFactory;

    /**
     * 게시글 다건 검색
     *
     * @param keyword    검색 키워드
     * @param searchType 검색 타입(title/content/author/전체)
     * @param categoryId 카테고리 ID 필터 (nullable)
     * @param pageable   페이징 + 정렬 조건
     */
    @Override
    public Page<PostListResponse> searchPosts(String keyword, String searchType, Long categoryId, Pageable pageable) {
        // 검색 조건 생성
        BooleanBuilder where = buildWhere(keyword, searchType, categoryId);

        // 정렬 조건 생성
        List<OrderSpecifier<?>> orders = buildOrderSpecifiers(pageable);

        // 메인 게시글 쿼리 실행
        List<PostListResponse> results = fetchPosts(where, orders, pageable);

        // 카테고리 조회 후 DTO에 주입
        injectCategories(results);

        // 전체 카운트 조회
        long total = countPosts(where, categoryId);

        // 결과를 Page로 감싸서 반환
        return new PageImpl<>(results, pageable, total);
    }

    /**
     * 검색 조건 생성
     * - keyword + searchType(title/content/author)에 따라 동적 조건 추가
     * - categoryId가 주어지면 카테고리 필터 조건 추가
     */
    private BooleanBuilder buildWhere(String keyword, String searchType, Long categoryId) {
        QPost post = QPost.post;
        QPostCategoryMapping categoryMapping = QPostCategoryMapping.postCategoryMapping;

        BooleanBuilder where = new BooleanBuilder();

        // 검색 조건 추가
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
     * 정렬 처리 빌더
     * - Pageable의 Sort 정보 기반으로 OrderSpecifier 생성
     * - likeCount/bookmarkCount/commentCount -> countDistinct() 기준 정렬
     * - 그 외 속성 -> Post 엔티티 필드 기준 정렬
     */
    private List<OrderSpecifier<?>> buildOrderSpecifiers(Pageable pageable) {
        QPost post = QPost.post;
        QPostLike postLike = QPostLike.postLike;
        QPostBookmark postBookmark = QPostBookmark.postBookmark;
        QComment comment = QComment.comment;

        List<OrderSpecifier<?>> orders = new ArrayList<>();
        PathBuilder<Post> entityPath = new PathBuilder<>(Post.class, post.getMetadata());

        // 정렬 조건 추가
        for (Sort.Order order : pageable.getSort()) {
            Order direction = order.isAscending() ? Order.ASC : Order.DESC;
            String prop = order.getProperty();
            switch (prop) {
                case "likeCount" -> orders.add(new OrderSpecifier<>(direction, postLike.id.countDistinct()));
                case "bookmarkCount" -> orders.add(new OrderSpecifier<>(direction, postBookmark.id.countDistinct()));
                case "commentCount" -> orders.add(new OrderSpecifier<>(direction, comment.id.countDistinct()));
                default ->
                        orders.add(new OrderSpecifier<>(direction, entityPath.getComparable(prop, Comparable.class)));
            }
        }

        return orders;
    }

    /**
     * 게시글 조회 (메인 쿼리)
     * - Post + User join
     * - 좋아요, 북마크, 댓글 countDistinct() 집계
     * - groupBy(post.id, user.id, userProfie.nickname)
     * - Pageable offset/limit 적용
     */
    private List<PostListResponse> fetchPosts(BooleanBuilder where, List<OrderSpecifier<?>> orders, Pageable pageable) {
        QPost post = QPost.post;
        QUser user = QUser.user;
        QPostLike postLike = QPostLike.postLike;
        QPostBookmark postBookmark = QPostBookmark.postBookmark;
        QComment comment = QComment.comment;

        return queryFactory
                .select(new QPostListResponse(
                        post.id,
                        new QPostListResponse_AuthorResponse(user.id, user.userProfile.nickname),
                        post.title,
                        Expressions.constant(Collections.emptyList()),   // 카테고리는 나중에 주입
                        postLike.id.countDistinct(),
                        postBookmark.id.countDistinct(),
                        comment.id.countDistinct(),
                        post.createdAt,
                        post.updatedAt
                ))
                .from(post)
                .leftJoin(post.user, user)
                .leftJoin(post.postLikes, postLike)
                .leftJoin(post.postBookmarks, postBookmark)
                .leftJoin(post.comments, comment)
                .where(where)
                .groupBy(post.id, user.id, user.userProfile.nickname)
                .orderBy(orders.toArray(new OrderSpecifier[0]))
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();
    }

    /**
     * 카테고리 일괄 조회 & 매핑
     * - postId 목록을 모아 IN 쿼리 실행
     * - 결과를 Map<postId, List<CategoryResponse>>로 변환
     * - 각 PostListResponse DTO에 categories 주입
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
                        new QPostListResponse_CategoryResponse(categoryMapping.category.id, categoryMapping.category.name)
                )
                .from(categoryMapping)
                .where(categoryMapping.post.id.in(postIds))
                .fetch();

        // Map<postId, List<CategoryResponse>>로 변환
        Map<Long, List<PostListResponse.CategoryResponse>> categoryMap = categoryTuples.stream()
                .collect(Collectors.groupingBy(
                        tuple -> Objects.requireNonNull(tuple.get(categoryMapping.post.id)),
                        Collectors.mapping(t -> t.get(1, PostListResponse.CategoryResponse.class), Collectors.toList())
                ));

        // categories 주입
        results.forEach(r -> r.setCategories(categoryMap.getOrDefault(r.getPostId(), List.of())));
    }

    /**
     * 전체 게시글 개수 조회
     * - 조건에 맞는 게시글 총 개수를 가져옴
     * - categoryId 필터가 있으면 postCategoryMapping join 포함
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
