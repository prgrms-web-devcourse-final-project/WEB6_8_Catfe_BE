package com.back.domain.board.post.controller;

import com.back.domain.board.post.controller.docs.PostCategoryControllerDocs;
import com.back.domain.board.post.dto.CategoryRequest;
import com.back.domain.board.post.dto.CategoryResponse;
import com.back.domain.board.post.service.PostCategoryService;
import com.back.global.common.dto.RsData;
import com.back.global.security.user.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/posts/categories")
@RequiredArgsConstructor
public class PostCategoryController implements PostCategoryControllerDocs {
    private final PostCategoryService postCategoryService;

    // 카테고리 생성
    @PostMapping
    public ResponseEntity<RsData<CategoryResponse>> createCategory(
            @RequestBody @Valid CategoryRequest request,
            @AuthenticationPrincipal CustomUserDetails user
    ) {
        CategoryResponse response = postCategoryService.createCategory(request, user.getUserId());
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(RsData.success(
                        "카테고리가 생성되었습니다.",
                        response
                ));
    }

    // 카테고리 전체 조회
    @GetMapping
    public ResponseEntity<RsData<List<CategoryResponse>>> getAllCategories() {
        List<CategoryResponse> response = postCategoryService.getAllCategories();
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(RsData.success(
                        "카테고리 목록이 조회되었습니다.",
                        response
                ));
    }
}
