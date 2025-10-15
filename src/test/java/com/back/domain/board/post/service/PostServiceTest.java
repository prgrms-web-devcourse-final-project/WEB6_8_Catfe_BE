package com.back.domain.board.post.service;

import com.back.domain.board.common.dto.PageResponse;
import com.back.domain.board.post.entity.Post;
import com.back.domain.board.post.entity.PostCategory;
import com.back.domain.board.post.dto.PostDetailResponse;
import com.back.domain.board.post.dto.PostListResponse;
import com.back.domain.board.post.dto.PostRequest;
import com.back.domain.board.post.dto.PostResponse;
import com.back.domain.board.post.enums.CategoryType;
import com.back.domain.board.post.repository.PostCategoryRepository;
import com.back.domain.board.post.repository.PostRepository;
import com.back.domain.file.entity.AttachmentMapping;
import com.back.domain.file.entity.EntityType;
import com.back.domain.file.entity.FileAttachment;
import com.back.domain.file.repository.AttachmentMappingRepository;
import com.back.domain.file.repository.FileAttachmentRepository;
import com.back.domain.file.service.FileService;
import com.back.domain.user.common.entity.User;
import com.back.domain.user.common.entity.UserProfile;
import com.back.domain.user.common.enums.UserStatus;
import com.back.domain.user.common.repository.UserRepository;
import com.back.global.exception.CustomException;
import com.back.global.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@Transactional
@ActiveProfiles("test")
class PostServiceTest {

    @Autowired
    private PostService postService;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PostCategoryRepository postCategoryRepository;

    @Autowired
    private FileAttachmentRepository fileAttachmentRepository;

    @Autowired
    private AttachmentMappingRepository attachmentMappingRepository;

    @MockitoBean
    private FileService fileService;

    // ====================== 게시글 생성 테스트 ======================

    @Test
    @DisplayName("게시글 생성 성공 - 카테고리 + 이미지 포함")
    void createPost_success_withCategoriesAndImages() {
        // given
        User user = User.createUser("writer", "writer@example.com", "encodedPwd");
        user.setUserProfile(new UserProfile(user, "작성자", null, null, null, 0));
        user.setUserStatus(UserStatus.ACTIVE);
        userRepository.save(user);

        PostCategory category = new PostCategory("공지", CategoryType.SUBJECT);
        postCategoryRepository.save(category);

        // MockMultipartFile로 가짜 이미지 생성
        MockMultipartFile imgFile1 = new MockMultipartFile("file", "image1.png", "image/png", "dummy".getBytes());
        MockMultipartFile imgFile2 = new MockMultipartFile("file", "image2.png", "image/png", "dummy".getBytes());

        // 파일(이미지) 생성
        FileAttachment img1 = new FileAttachment("stored_image1.png", imgFile1, user, "https://cdn.example.com/image1.png");
        FileAttachment img2 = new FileAttachment("stored_image2.png", imgFile2, user, "https://cdn.example.com/image2.png");
        fileAttachmentRepository.saveAll(List.of(img1, img2));

        PostRequest request = new PostRequest(
                "제목", "내용", null,
                List.of(category.getId()),
                List.of(img1.getId(), img2.getId())
        );

        // when
        PostResponse response = postService.createPost(request, user.getId());

        // then — DTO 검증
        assertThat(response.title()).isEqualTo("제목");
        assertThat(response.content()).isEqualTo("내용");
        assertThat(response.author().nickname()).isEqualTo("작성자");
        assertThat(response.categories()).hasSize(1);
        assertThat(response.categories().getFirst().name()).isEqualTo("공지");
        assertThat(response.images()).hasSize(2);
        assertThat(response.images().getFirst().id()).isEqualTo(img1.getId());

        // then — DB 매핑 검증
        List<AttachmentMapping> mappings = attachmentMappingRepository
                .findAllByEntityTypeAndEntityId(EntityType.POST, response.postId());

        assertThat(mappings).hasSize(2); // 이미지 2개 → 매핑 2개
        assertThat(mappings)
                .allSatisfy(mapping -> {
                    assertThat(mapping.getEntityType()).isEqualTo(EntityType.POST);
                    assertThat(mapping.getEntityId()).isEqualTo(response.postId());
                    assertThat(mapping.getFileAttachment()).isNotNull();
                    assertThat(mapping.getFileAttachment().getId())
                            .isIn(img1.getId(), img2.getId());
                });
    }

    @Test
    @DisplayName("게시글 생성 실패 - 존재하지 않는 유저")
    void createPost_fail_userNotFound() {
        // given
        PostRequest request = new PostRequest("제목", "내용", null, null, null);

        // when & then
        assertThatThrownBy(() -> postService.createPost(request, 999L))
                .isInstanceOf(CustomException.class)
                .hasMessage(ErrorCode.USER_NOT_FOUND.getMessage());
    }

    @Test
    @DisplayName("게시글 생성 실패 - 존재하지 않는 카테고리 ID 포함")
    void createPost_fail_categoryNotFound() {
        // given: 유저는 정상
        User user = User.createUser("writer2", "writer2@example.com", "encodedPwd");
        user.setUserProfile(new UserProfile(user, "작성자2", null, null, null, 0));
        user.setUserStatus(UserStatus.ACTIVE);
        userRepository.save(user);

        // 실제 저장 안 된 카테고리 ID 요청
        PostRequest request = new PostRequest("제목", "내용", null, List.of(100L, 200L), null);

        // when & then
        assertThatThrownBy(() -> postService.createPost(request, user.getId()))
                .isInstanceOf(CustomException.class)
                .hasMessage(ErrorCode.CATEGORY_NOT_FOUND.getMessage());
    }

    @Test
    @DisplayName("게시글 생성 실패 - 존재하지 않는 파일 ID 포함")
    void createPost_fail_fileNotFound() {
        // given
        User user = User.createUser("writer", "writer@example.com", "encodedPwd");
        user.setUserProfile(new UserProfile(user, "작성자", null, null, null, 0));
        user.setUserStatus(UserStatus.ACTIVE);
        userRepository.save(user);

        PostRequest request = new PostRequest(
                "제목", "내용", null,
                null, // 카테고리 없음
                List.of(999L) // 존재하지 않는 파일 ID
        );

        // when & then
        assertThatThrownBy(() -> postService.createPost(request, user.getId()))
                .isInstanceOf(CustomException.class)
                .hasMessage(ErrorCode.FILE_NOT_FOUND.getMessage());
    }

    // ====================== 게시글 조회 테스트 ======================


    @Test
    @DisplayName("게시글 다건 조회 성공 - 페이징 + 카테고리")
    void getPosts_success() {
        // given
        User user = User.createUser("writer3", "writer3@example.com", "encodedPwd");
        user.setUserProfile(new UserProfile(user, "작성자3", null, null, null, 0));
        user.setUserStatus(UserStatus.ACTIVE);
        userRepository.save(user);

        PostCategory c1 = new PostCategory("공지사항", CategoryType.SUBJECT);
        PostCategory c2 = new PostCategory("자유게시판", CategoryType.SUBJECT);
        postCategoryRepository.saveAll(List.of(c1, c2));

        Post post1 = new Post(user, "첫 번째 글", "내용1", null);
        post1.updateCategories(List.of(c1));
        postRepository.save(post1);

        Post post2 = new Post(user, "두 번째 글", "내용2", null);
        post2.updateCategories(List.of(c2));
        postRepository.save(post2);

        Pageable pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt"));

        // when
        PageResponse<PostListResponse> response = postService.getPosts(null, null, null, pageable);

        // then
        assertThat(response.items()).hasSize(2);
        assertThat(response.items().get(0).getTitle()).isEqualTo("두 번째 글");
        assertThat(response.items().get(1).getTitle()).isEqualTo("첫 번째 글");
    }

    @Test
    @DisplayName("게시글 단건 조회 성공")
    void getPost_success() {
        // given
        User user = User.createUser("reader", "reader@example.com", "encodedPwd");
        user.setUserProfile(new UserProfile(user, "독자", null, null, null, 0));
        user.setUserStatus(UserStatus.ACTIVE);
        userRepository.save(user);

        PostCategory category = new PostCategory("공지", CategoryType.SUBJECT);
        postCategoryRepository.save(category);

        // 게시글 생성
        Post post = new Post(user, "조회용 제목", "조회용 내용", null);
        post.updateCategories(List.of(category));
        postRepository.save(post);

        // 첨부 이미지 추가
        MockMultipartFile file1 = new MockMultipartFile("file", "img1.png", "image/png", "dummy".getBytes());
        FileAttachment attachment1 = new FileAttachment("stored_img1.png", file1, user, "https://cdn.example.com/img1.png");
        fileAttachmentRepository.save(attachment1);

        MockMultipartFile file2 = new MockMultipartFile("file", "img2.png", "image/png", "dummy".getBytes());
        FileAttachment attachment2 = new FileAttachment("stored_img2.png", file2, user, "https://cdn.example.com/img2.png");
        fileAttachmentRepository.save(attachment2);

        // 매핑 저장 (EntityType.POST)
        attachmentMappingRepository.save(new AttachmentMapping(attachment1, EntityType.POST, post.getId()));
        attachmentMappingRepository.save(new AttachmentMapping(attachment2, EntityType.POST, post.getId()));

        // when: 비로그인 상태에서 조회
        PostDetailResponse response = postService.getPost(post.getId(), null);

        // then
        assertThat(response.postId()).isEqualTo(post.getId());
        assertThat(response.title()).isEqualTo("조회용 제목");
        assertThat(response.content()).isEqualTo("조회용 내용");
        assertThat(response.author().nickname()).isEqualTo("독자");
        assertThat(response.categories()).extracting("name").containsExactly("공지");

        // 첨부 이미지 검증
        assertThat(response.images()).hasSize(2);
        assertThat(response.images())
                .extracting("url")
                .containsExactlyInAnyOrder("https://cdn.example.com/img1.png", "https://cdn.example.com/img2.png");

        // 기본 상호작용 상태 검증
        assertThat(response.likeCount()).isZero();
        assertThat(response.bookmarkCount()).isZero();
        assertThat(response.commentCount()).isZero();
        assertThat(response.likedByMe()).isFalse();
        assertThat(response.bookmarkedByMe()).isFalse();
    }

    @Test
    @DisplayName("게시글 단건 조회 실패 - 존재하지 않는 게시글")
    void getPost_fail_postNotFound() {
        // when & then
        assertThatThrownBy(() -> postService.getPost(999L, null))
                .isInstanceOf(CustomException.class)
                .hasMessage(ErrorCode.POST_NOT_FOUND.getMessage());
    }

    // ====================== 게시글 수정 테스트 ======================

    @Test
    @DisplayName("게시글 수정 성공 - 이미지 교체")
    void updatePost_success_withNewImages() {
        // given
        User user = User.createUser("writer", "writer@example.com", "encodedPwd");
        user.setUserProfile(new UserProfile(user, "작성자", null, null, null, 0));
        user.setUserStatus(UserStatus.ACTIVE);
        userRepository.save(user);

        // 카테고리: 기존(공지) / 신규(자유)
        PostCategory oldCategory = new PostCategory("공지", CategoryType.SUBJECT);
        PostCategory newCategory = new PostCategory("자유", CategoryType.SUBJECT);
        postCategoryRepository.saveAll(List.of(oldCategory, newCategory));

        // 기존 이미지
        MockMultipartFile oldFile = new MockMultipartFile("file", "old.png", "image/png", "dummy".getBytes());
        FileAttachment imgOld = new FileAttachment("stored_old.png", oldFile, user, "https://cdn.example.com/old.png");
        fileAttachmentRepository.save(imgOld);

        // 게시글 생성 + 기존 카테고리 세팅
        Post post = new Post(user, "원래 제목", "원래 내용", null);
        post.updateCategories(List.of(oldCategory));
        postRepository.save(post);

        // 기존 이미지 매핑(다형 매핑 방식으로 직접 저장)
        attachmentMappingRepository.save(new AttachmentMapping(imgOld, EntityType.POST, post.getId()));

        // 새 이미지
        MockMultipartFile newFile = new MockMultipartFile("file", "new.png", "image/png", "dummy".getBytes());
        FileAttachment imgNew = new FileAttachment("stored_new.png", newFile, user, "https://cdn.example.com/new.png");
        fileAttachmentRepository.save(imgNew);

        // 수정 요청: 제목/내용 변경 + 카테고리를 '자유'로 교체 + 이미지를 새 것으로 교체
        PostRequest request = new PostRequest(
                "수정된 제목", "수정된 내용", null,
                List.of(newCategory.getId()),
                List.of(imgNew.getId())
        );

        // when
        PostResponse response = postService.updatePost(post.getId(), request, user.getId());

        // then
        assertThat(response.title()).isEqualTo("수정된 제목");
        assertThat(response.content()).isEqualTo("수정된 내용");
        assertThat(response.categories()).hasSize(1);
        assertThat(response.categories().getFirst().name()).isEqualTo("자유");
        assertThat(response.images()).hasSize(1);
        assertThat(response.images().getFirst().id()).isEqualTo(imgNew.getId());
    }

    @Test
    @DisplayName("게시글 수정 실패 - 게시글 없음")
    void updatePost_fail_postNotFound() {
        // given
        User user = User.createUser("writer2", "writer2@example.com", "encodedPwd");
        user.setUserProfile(new UserProfile(user, "작성자2", null, null, null, 0));
        user.setUserStatus(UserStatus.ACTIVE);
        userRepository.save(user);

        PostRequest request = new PostRequest("제목", "내용", null, List.of(), null);

        // when & then
        assertThatThrownBy(() -> postService.updatePost(999L, request, user.getId()))
                .isInstanceOf(CustomException.class)
                .hasMessage(ErrorCode.POST_NOT_FOUND.getMessage());
    }

    @Test
    @DisplayName("게시글 수정 실패 - 작성자 아님")
    void updatePost_fail_noPermission() {
        // given: 게시글 작성자
        User writer = User.createUser("writer3", "writer3@example.com", "encodedPwd");
        writer.setUserProfile(new UserProfile(writer, "작성자3", null, null, null, 0));
        writer.setUserStatus(UserStatus.ACTIVE);
        userRepository.save(writer);

        // 다른 사용자
        User another = User.createUser("other", "other@example.com", "encodedPwd");
        another.setUserProfile(new UserProfile(another, "다른사람", null, null, null, 0));
        another.setUserStatus(UserStatus.ACTIVE);
        userRepository.save(another);

        PostCategory category = new PostCategory("공지", CategoryType.SUBJECT);
        postCategoryRepository.save(category);

        Post post = new Post(writer, "원래 제목", "원래 내용", null);
        post.updateCategories(List.of(category));
        postRepository.save(post);

        PostRequest request = new PostRequest("수정된 제목", "수정된 내용", null, List.of(category.getId()), null);

        // when & then
        assertThatThrownBy(() -> postService.updatePost(post.getId(), request, another.getId()))
                .isInstanceOf(CustomException.class)
                .hasMessage(ErrorCode.POST_NO_PERMISSION.getMessage());
    }

    @Test
    @DisplayName("게시글 수정 실패 - 존재하지 않는 카테고리 포함")
    void updatePost_fail_categoryNotFound() {
        // given
        User user = User.createUser("writer4", "writer4@example.com", "encodedPwd");
        user.setUserProfile(new UserProfile(user, "작성자4", null, null, null, 0));
        user.setUserStatus(UserStatus.ACTIVE);
        userRepository.save(user);

        PostCategory category = new PostCategory("공지", CategoryType.SUBJECT);
        postCategoryRepository.save(category);

        Post post = new Post(user, "원래 제목", "원래 내용", null);
        post.updateCategories(List.of(category));
        postRepository.save(post);

        // 실제 DB에는 없는 카테고리 ID 전달
        PostRequest request = new PostRequest("수정된 제목", "수정된 내용", null, List.of(999L), null);

        // when & then
        assertThatThrownBy(() -> postService.updatePost(post.getId(), request, user.getId()))
                .isInstanceOf(CustomException.class)
                .hasMessage(ErrorCode.CATEGORY_NOT_FOUND.getMessage());
    }

    @Test
    @DisplayName("게시글 수정 실패 - 존재하지 않는 파일 ID 포함")
    void updatePost_fail_fileNotFound() {
        // given
        User user = User.createUser("writer2", "writer2@example.com", "encodedPwd");
        user.setUserProfile(new UserProfile(user, "작성자2", null, null, null, 0));
        user.setUserStatus(UserStatus.ACTIVE);
        userRepository.save(user);

        PostCategory category = new PostCategory("공지", CategoryType.SUBJECT);
        postCategoryRepository.save(category);

        Post post = new Post(user, "원래 제목", "원래 내용", null);
        post.updateCategories(List.of(category));
        postRepository.save(post);

        PostRequest request = new PostRequest(
                "수정된 제목", "수정된 내용", null,
                List.of(category.getId()),
                List.of(999L) // 존재하지 않는 파일
        );

        // when & then
        assertThatThrownBy(() -> postService.updatePost(post.getId(), request, user.getId()))
                .isInstanceOf(CustomException.class)
                .hasMessage(ErrorCode.FILE_NOT_FOUND.getMessage());
    }

    // ====================== 게시글 삭제 테스트 ======================

    @Test
    @DisplayName("게시글 삭제 성공 - 첨부 이미지 매핑도 함께 삭제")
    void deletePost_success_withImages() {
        // given
        User user = User.createUser("writer", "writer@example.com", "encodedPwd");
        user.setUserProfile(new UserProfile(user, "작성자", null, null, null, 0));
        user.setUserStatus(UserStatus.ACTIVE);
        userRepository.save(user);

        // 첨부 이미지
        MockMultipartFile imgFile = new MockMultipartFile("file", "del.png", "image/png", "dummy".getBytes());
        FileAttachment img = new FileAttachment("stored_del.png", imgFile, user, "https://cdn.example.com/del.png");
        fileAttachmentRepository.save(img);

        // 게시글 생성
        Post post = new Post(user, "삭제 제목", "삭제 내용", null);
        postRepository.save(post);

        // 이미지 매핑 (EntityType.POST + postId)
        attachmentMappingRepository.save(new AttachmentMapping(img, EntityType.POST, post.getId()));

        Long postId = post.getId();

        // when
        postService.deletePost(postId, user.getId());

        // then
        // 게시글 삭제 확인
        assertThat(postRepository.findById(postId)).isEmpty();

        // 매핑 삭제 확인
        assertThat(attachmentMappingRepository.findAllByEntityTypeAndEntityId(EntityType.POST, postId))
                .isEmpty();
    }

    @Test
    @DisplayName("게시글 삭제 실패 - 게시글 없음")
    void deletePost_fail_postNotFound() {
        // given
        User user = User.createUser("writer2", "writer2@example.com", "encodedPwd");
        user.setUserProfile(new UserProfile(user, "작성자2", null, null, null, 0));
        user.setUserStatus(UserStatus.ACTIVE);
        userRepository.save(user);

        // when & then
        assertThatThrownBy(() -> postService.deletePost(999L, user.getId()))
                .isInstanceOf(CustomException.class)
                .hasMessage(ErrorCode.POST_NOT_FOUND.getMessage());
    }

    @Test
    @DisplayName("게시글 삭제 실패 - 작성자 아님")
    void deletePost_fail_noPermission() {
        // given
        User writer = User.createUser("writer3", "writer3@example.com", "encodedPwd");
        writer.setUserProfile(new UserProfile(writer, "작성자3", null, null, null, 0));
        writer.setUserStatus(UserStatus.ACTIVE);
        userRepository.save(writer);

        User another = User.createUser("other", "other@example.com", "encodedPwd");
        another.setUserProfile(new UserProfile(another, "다른사람", null, null, null, 0));
        another.setUserStatus(UserStatus.ACTIVE);
        userRepository.save(another);

        Post post = new Post(writer, "원래 제목", "원래 내용", null);
        postRepository.save(post);

        // when & then
        assertThatThrownBy(() -> postService.deletePost(post.getId(), another.getId()))
                .isInstanceOf(CustomException.class)
                .hasMessage(ErrorCode.POST_NO_PERMISSION.getMessage());
    }
}
