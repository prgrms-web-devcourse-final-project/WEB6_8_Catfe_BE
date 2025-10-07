package com.back.domain.board.post.controller;

import com.back.domain.board.post.dto.CategoryResponse;
import com.back.domain.board.post.service.PostCategoryService;
import com.back.global.common.dto.RsData;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/posts/categories")
@RequiredArgsConstructor
public class PostCategoryController {
    private final PostCategoryService postCategoryService;

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
