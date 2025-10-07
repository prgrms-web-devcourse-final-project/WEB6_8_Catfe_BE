package com.back.domain.board.service;

import com.back.domain.board.post.dto.CategoryRequest;
import com.back.domain.board.post.dto.CategoryResponse;
import com.back.domain.board.post.enums.CategoryType;
import com.back.domain.board.post.entity.PostCategory;
import com.back.domain.board.post.repository.PostCategoryRepository;
import com.back.domain.board.post.service.PostCategoryService;
import com.back.domain.user.entity.User;
import com.back.domain.user.entity.UserProfile;
import com.back.domain.user.entity.UserStatus;
import com.back.domain.user.repository.UserRepository;
import com.back.global.exception.CustomException;
import com.back.global.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@Transactional
@ActiveProfiles("test")
class PostCategoryServiceTest {

    @Autowired
    private PostCategoryService postCategoryService;

    @Autowired
    private PostCategoryRepository postCategoryRepository;

    @Autowired
    private UserRepository userRepository;

    // ====================== 카테고리 생성 테스트 ======================

    @Test
    @DisplayName("카테고리 생성 성공")
    void createCategory_success() {
        // given
        User user = User.createUser("writer", "writer@example.com", "encodedPwd");
        user.setUserProfile(new UserProfile(user, "작성자", null, null, null, 0));
        user.setUserStatus(UserStatus.ACTIVE);
        userRepository.save(user);

        CategoryRequest request = new CategoryRequest("백엔드", CategoryType.SUBJECT);

        // when
        CategoryResponse response = postCategoryService.createCategory(request, user.getId());

        // then
        assertThat(response.name()).isEqualTo("백엔드");
        assertThat(response.type()).isEqualTo(CategoryType.SUBJECT);
        assertThat(postCategoryRepository.findAll()).hasSize(1);
    }

    @Test
    @DisplayName("카테고리 생성 실패 - 존재하지 않는 유저")
    void createCategory_fail_userNotFound() {
        // given
        CategoryRequest request = new CategoryRequest("프론트엔드", CategoryType.SUBJECT);

        // when & then
        assertThatThrownBy(() -> postCategoryService.createCategory(request, 999L))
                .isInstanceOf(CustomException.class)
                .hasMessage(ErrorCode.USER_NOT_FOUND.getMessage());
    }

    @Test
    @DisplayName("카테고리 생성 실패 - 이미 존재하는 카테고리 이름")
    void createCategory_fail_alreadyExists() {
        // given
        User user = User.createUser("admin", "admin@example.com", "encodedPwd");
        user.setUserProfile(new UserProfile(user, "관리자", null, null, null, 0));
        user.setUserStatus(UserStatus.ACTIVE);
        userRepository.save(user);

        // 같은 이름의 카테고리 미리 저장
        postCategoryRepository.save(new PostCategory("CS", CategoryType.SUBJECT));

        CategoryRequest request = new CategoryRequest("CS", CategoryType.SUBJECT);

        // when & then
        assertThatThrownBy(() -> postCategoryService.createCategory(request, user.getId()))
                .isInstanceOf(CustomException.class)
                .hasMessage(ErrorCode.CATEGORY_ALREADY_EXISTS.getMessage());
    }

    // ====================== 카테고리 조회 테스트 ======================

    @Test
    @DisplayName("카테고리 전체 조회 성공")
    void getAllCategories_success() {
        // given
        postCategoryRepository.save(new PostCategory("백엔드", CategoryType.SUBJECT));
        postCategoryRepository.save(new PostCategory("중학생", CategoryType.DEMOGRAPHIC));
        postCategoryRepository.save(new PostCategory("2~4명", CategoryType.GROUP_SIZE));

        // when
        List<CategoryResponse> responses = postCategoryService.getAllCategories();

        // then
        assertThat(responses).hasSize(3);
        assertThat(responses)
                .extracting(CategoryResponse::name)
                .containsExactlyInAnyOrder("백엔드", "중학생", "2~4명");
    }
}
