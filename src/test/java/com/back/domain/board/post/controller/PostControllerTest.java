package com.back.domain.board.post.controller;

import com.back.domain.board.post.dto.PostRequest;
import com.back.domain.board.post.entity.Post;
import com.back.domain.board.post.entity.PostCategory;
import com.back.domain.board.post.enums.CategoryType;
import com.back.domain.board.post.repository.PostCategoryRepository;
import com.back.domain.board.post.repository.PostRepository;
import com.back.domain.file.entity.FileAttachment;
import com.back.domain.file.repository.FileAttachmentRepository;
import com.back.domain.file.service.FileService;
import com.back.domain.user.common.entity.User;
import com.back.domain.user.common.entity.UserProfile;
import com.back.domain.user.common.enums.UserStatus;
import com.back.domain.user.common.repository.UserRepository;
import com.back.fixture.TestJwtTokenProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class PostControllerTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private PostCategoryRepository postCategoryRepository;

    @Autowired
    private FileAttachmentRepository fileAttachmentRepository;

    @Autowired
    private TestJwtTokenProvider testJwtTokenProvider;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private FileService fileService;

    private String generateAccessToken(User user) {
        return testJwtTokenProvider.createAccessToken(user.getId(), user.getUsername(), user.getRole().name());
    }

    // ====================== 게시글 생성 테스트 ======================

    @Test
    @DisplayName("게시글 생성 성공 → 201 Created")
    void createPost_success() throws Exception {
        // given: 정상 유저 생성
        User user = User.createUser("writer", "writer@example.com", passwordEncoder.encode("P@ssw0rd!"));
        user.setUserProfile(new UserProfile(user, "홍길동", null, "소개글", LocalDate.of(2000, 1, 1), 1000));
        user.setUserStatus(UserStatus.ACTIVE);
        userRepository.save(user);

        String accessToken = generateAccessToken(user);

        // 카테고리 등록
        PostCategory c1 = new PostCategory("공지사항", CategoryType.SUBJECT);
        postCategoryRepository.save(c1);

        PostCategory c2 = new PostCategory("자유게시판", CategoryType.SUBJECT);
        postCategoryRepository.save(c2);

        // 이미지 등록
        MockMultipartFile file = new MockMultipartFile("file", "thumb.png", "image/png", "dummy".getBytes());
        FileAttachment attachment = new FileAttachment("stored_thumb.png", file, user, "https://cdn.example.com/thumb.png");
        fileAttachmentRepository.save(attachment);

        // 요청 구성
        PostRequest request = new PostRequest(
                "첫 번째 게시글",
                "안녕하세요, 첫 글입니다!",
                null,
                List.of(c1.getId(), c2.getId()),
                List.of(attachment.getId())
        );

        // when
        ResultActions resultActions = mvc.perform(
                post("/api/posts")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
        ).andDo(print());

        // then
        resultActions
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value("SUCCESS_200"))
                .andExpect(jsonPath("$.data.title").value("첫 번째 게시글"))
                .andExpect(jsonPath("$.data.author.nickname").value("홍길동"))
                .andExpect(jsonPath("$.data.categories.length()").value(2))
                .andExpect(jsonPath("$.data.images.length()").value(1))
                .andExpect(jsonPath("$.data.images[0].url").value("https://cdn.example.com/thumb.png"));
    }

    @Test
    @DisplayName("게시글 생성 실패 - 존재하지 않는 사용자 → 404 Not Found")
    void createPost_userNotFound() throws Exception {
        // given: 토큰만 발급(실제 DB엔 없음)
        String fakeToken = testJwtTokenProvider.createAccessToken(999L, "ghost", "USER");

        PostRequest request = new PostRequest("제목", "내용", null, List.of(), List.of());

        // when & then
        mvc.perform(post("/api/posts")
                        .header("Authorization", "Bearer " + fakeToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("USER_001"))
                .andExpect(jsonPath("$.message").value("존재하지 않는 사용자입니다."));
    }

    @Test
    @DisplayName("게시글 생성 실패 - 존재하지 않는 카테고리 → 404 Not Found")
    void createPost_categoryNotFound() throws Exception {
        // given: 정상 유저
        User user = User.createUser("writer2", "writer2@example.com", passwordEncoder.encode("P@ssw0rd!"));
        user.setUserProfile(new UserProfile(user, "작성자", null, null, null, 0));
        user.setUserStatus(UserStatus.ACTIVE);
        userRepository.save(user);

        String accessToken = generateAccessToken(user);

        // 존재하지 않는 카테고리 ID
        PostRequest request = new PostRequest("제목", "내용", null, List.of(999L), List.of());

        // when & then
        mvc.perform(post("/api/posts")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("POST_003"))
                .andExpect(jsonPath("$.message").value("존재하지 않는 카테고리입니다."));
    }

    @Test
    @DisplayName("게시글 생성 실패 - 존재하지 않는 이미지 → 404 Not Found")
    void createPost_fail_imageNotFound() throws Exception {
        // given
        User user = User.createUser("writer", "writer@example.com", passwordEncoder.encode("P@ssw0rd!"));
        user.setUserProfile(new UserProfile(user, "홍길동", null, "소개글", LocalDate.of(2000, 1, 1), 1000));
        user.setUserStatus(UserStatus.ACTIVE);
        userRepository.save(user);

        String accessToken = generateAccessToken(user);

        PostCategory category = new PostCategory("공지사항", CategoryType.SUBJECT);
        postCategoryRepository.save(category);

        // 존재하지 않는 이미지 ID
        PostRequest request = new PostRequest("이미지 테스트", "없는 이미지입니다", null,
                List.of(category.getId()), List.of(999L));

        // when & then
        mvc.perform(post("/api/posts")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("FILE_004"))
                .andExpect(jsonPath("$.message").value("파일 정보를 찾을 수 없습니다."));
    }

    @Test
    @DisplayName("게시글 생성 실패 - 잘못된 요청(필드 누락) → 400 Bad Request")
    void createPost_badRequest() throws Exception {
        // given: 정상 유저 생성
        User user = User.createUser("writer", "writer@example.com", passwordEncoder.encode("P@ssw0rd!"));
        user.setUserProfile(new UserProfile(user, "홍길동", null, "소개글", LocalDate.of(2000, 1, 1), 1000));
        user.setUserStatus(UserStatus.ACTIVE);
        userRepository.save(user);

        String accessToken = generateAccessToken(user);

        // given: title 누락
        String invalidJson = """
                {
                  "content": "본문만 있음"
                }
                """;

        // when & then
        mvc.perform(post("/api/posts")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON_400"))
                .andExpect(jsonPath("$.message").value("잘못된 요청입니다."));
    }

    @Test
    @DisplayName("게시글 생성 실패 - 토큰 없음 → 401 Unauthorized")
    void createPost_noToken() throws Exception {
        // given
        PostRequest request = new PostRequest("제목", "내용", null, List.of(), List.of());

        // when & then
        mvc.perform(post("/api/posts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_001"))
                .andExpect(jsonPath("$.message").value("인증이 필요합니다."));
    }

    // ====================== 게시글 조회 테스트 ======================

    @Test
    @DisplayName("게시글 다건 조회 성공 → 200 OK")
    void getPosts_success() throws Exception {
        // given: 유저 + 카테고리 + 게시글 2개
        User user = User.createUser("reader", "reader@example.com", passwordEncoder.encode("P@ssw0rd!"));
        user.setUserProfile(new UserProfile(user, "홍길동", null, null, null, 0));
        user.setUserStatus(UserStatus.ACTIVE);
        userRepository.save(user);

        PostCategory c1 = new PostCategory("공지사항", CategoryType.SUBJECT);
        PostCategory c2 = new PostCategory("자유게시판", CategoryType.SUBJECT);
        postCategoryRepository.saveAll(List.of(c1, c2));

        Post post1 = new Post(user, "첫 글", "내용1", null);
        post1.updateCategories(List.of(c1));
        postRepository.save(post1);

        Post post2 = new Post(user, "두 번째 글", "내용2", null);
        post2.updateCategories(List.of(c2));
        postRepository.save(post2);

        // when
        mvc.perform(get("/api/posts")
                        .param("page", "0")
                        .param("size", "10")
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                // then
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value("SUCCESS_200"))
                .andExpect(jsonPath("$.data.items.length()").value(2))
                .andExpect(jsonPath("$.data.items[0].author.nickname").value("홍길동"));
    }

    @Test
    @DisplayName("게시글 단건 조회 성공 → 200 OK")
    void getPost_success() throws Exception {
        // given
        User user = User.createUser("writer", "writer@example.com", passwordEncoder.encode("P@ssw0rd!"));
        user.setUserProfile(new UserProfile(user, "이몽룡", null, null, null, 0));
        user.setUserStatus(UserStatus.ACTIVE);
        userRepository.save(user);

        PostCategory c1 = new PostCategory("공지사항", CategoryType.SUBJECT);
        postCategoryRepository.save(c1);

        Post post = new Post(user, "조회 테스트 글", "조회 테스트 내용", null);
        post.updateCategories(List.of(c1));
        postRepository.save(post);

        // when
        mvc.perform(get("/api/posts/{postId}", post.getId())
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                // then
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value("SUCCESS_200"))
                .andExpect(jsonPath("$.data.postId").value(post.getId()))
                .andExpect(jsonPath("$.data.title").value("조회 테스트 글"))
                .andExpect(jsonPath("$.data.author.nickname").value("이몽룡"))
                .andExpect(jsonPath("$.data.categories[0].name").value("공지사항"));
    }

    @Test
    @DisplayName("게시글 단건 조회 실패 - 존재하지 않는 게시글 → 404 Not Found")
    void getPost_fail_notFound() throws Exception {
        mvc.perform(get("/api/posts/{postId}", 999L)
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("POST_001"))
                .andExpect(jsonPath("$.message").value("존재하지 않는 게시글입니다."));
    }

    // ====================== 게시글 수정 테스트 ======================

    @Test
    @DisplayName("게시글 수정 성공 → 200 OK")
    void updatePost_success() throws Exception {
        // given
        User user = User.createUser("writer", "writer@example.com", passwordEncoder.encode("P@ssw0rd!"));
        user.setUserProfile(new UserProfile(user, "홍길동", null, null, null, 0));
        user.setUserStatus(UserStatus.ACTIVE);
        userRepository.save(user);

        PostCategory c1 = new PostCategory("공지사항", CategoryType.SUBJECT);
        postCategoryRepository.save(c1);

        Post post = new Post(user, "원래 제목", "원래 내용", null);
        post.updateCategories(List.of(c1));
        postRepository.save(post);

        String accessToken = generateAccessToken(user);

        PostCategory c2 = new PostCategory("자유게시판", CategoryType.SUBJECT);
        postCategoryRepository.save(c2);

        MockMultipartFile file = new MockMultipartFile("file", "thumb.png", "image/png", "dummy".getBytes());
        FileAttachment attachment = new FileAttachment("stored_thumb.png", file, user, "https://cdn.example.com/thumb.png");
        fileAttachmentRepository.save(attachment);

        PostRequest request = new PostRequest(
                "수정된 게시글",
                "안녕하세요, 수정했습니다!",
                null,
                List.of(c1.getId(), c2.getId()),
                List.of(attachment.getId())
        );

        // when & then
        mvc.perform(put("/api/posts/{postId}", post.getId())
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value("SUCCESS_200"))
                .andExpect(jsonPath("$.data.title").value("수정된 게시글"))
                .andExpect(jsonPath("$.data.categories.length()").value(2))
                .andExpect(jsonPath("$.data.images.length()").value(1))
                .andExpect(jsonPath("$.data.images[0].url").value("https://cdn.example.com/thumb.png"));
    }

    @Test
    @DisplayName("게시글 수정 실패 - 게시글 없음 → 404 Not Found")
    void updatePost_fail_notFound() throws Exception {
        // given
        User user = User.createUser("writer2", "writer2@example.com", passwordEncoder.encode("P@ssw0rd!"));
        user.setUserProfile(new UserProfile(user, "작성자2", null, null, null, 0));
        user.setUserStatus(UserStatus.ACTIVE);
        userRepository.save(user);

        String accessToken = generateAccessToken(user);

        PostRequest request = new PostRequest("수정된 제목", "내용", null, List.of(), List.of());

        // when & then
        mvc.perform(put("/api/posts/{postId}", 999L)
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("POST_001"))
                .andExpect(jsonPath("$.message").value("존재하지 않는 게시글입니다."));
    }

    @Test
    @DisplayName("게시글 수정 실패 - 작성자 아님 → 403 Forbidden")
    void updatePost_fail_noPermission() throws Exception {
        // given
        User writer = User.createUser("writer3", "writer3@example.com", passwordEncoder.encode("P@ssw0rd!"));
        writer.setUserProfile(new UserProfile(writer, "작성자3", null, null, null, 0));
        writer.setUserStatus(UserStatus.ACTIVE);
        userRepository.save(writer);

        User another = User.createUser("other", "other@example.com", passwordEncoder.encode("P@ssw0rd!"));
        another.setUserProfile(new UserProfile(another, "다른사람", null, null, null, 0));
        another.setUserStatus(UserStatus.ACTIVE);
        userRepository.save(another);

        PostCategory c1 = new PostCategory("공지사항", CategoryType.SUBJECT);
        postCategoryRepository.save(c1);

        Post post = new Post(writer, "원래 제목", "원래 내용", null);
        post.updateCategories(List.of(c1));
        postRepository.save(post);

        String accessToken = generateAccessToken(another);

        PostRequest request = new PostRequest("수정된 제목", "수정된 내용", null, List.of(c1.getId()), List.of());

        // when & then
        mvc.perform(put("/api/posts/{postId}", post.getId())
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("POST_002"))
                .andExpect(jsonPath("$.message").value("게시글 작성자만 수정/삭제할 수 있습니다."));
    }

    @Test
    @DisplayName("게시글 수정 실패 - 존재하지 않는 카테고리 → 404 Not Found")
    void updatePost_fail_categoryNotFound() throws Exception {
        // given
        User user = User.createUser("writer4", "writer4@example.com", passwordEncoder.encode("P@ssw0rd!"));
        user.setUserProfile(new UserProfile(user, "작성자4", null, null, null, 0));
        user.setUserStatus(UserStatus.ACTIVE);
        userRepository.save(user);

        PostCategory c1 = new PostCategory("공지사항", CategoryType.SUBJECT);
        postCategoryRepository.save(c1);

        Post post = new Post(user, "원래 제목", "원래 내용", null);
        post.updateCategories(List.of(c1));
        postRepository.save(post);

        String accessToken = generateAccessToken(user);

        // 존재하지 않는 카테고리 ID
        PostRequest request = new PostRequest("수정된 제목", "수정된 내용", null, List.of(999L), List.of());

        // when & then
        mvc.perform(put("/api/posts/{postId}", post.getId())
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("POST_003"))
                .andExpect(jsonPath("$.message").value("존재하지 않는 카테고리입니다."));
    }

    @Test
    @DisplayName("게시글 수정 실패 - 존재하지 않는 이미지 → 404 Not Found")
    void updatePost_fail_imageNotFound() throws Exception {
        // given
        User user = User.createUser("writer", "writer@example.com", passwordEncoder.encode("P@ssw0rd!"));
        user.setUserProfile(new UserProfile(user, "홍길동", null, null, null, 0));
        user.setUserStatus(UserStatus.ACTIVE);
        userRepository.save(user);

        String accessToken = generateAccessToken(user);

        // 카테고리 등록
        PostCategory c1 = new PostCategory("공지사항", CategoryType.SUBJECT);
        postCategoryRepository.save(c1);

        // 게시글 생성
        Post post = new Post(user, "수정 테스트 제목", "수정 테스트 내용", null);
        postRepository.save(post);

        // 존재하지 않는 이미지 ID
        Long invalidImageId = 999L;

        // 수정 요청
        PostRequest request = new PostRequest(
                "수정된 제목",
                "수정된 내용",
                null,
                List.of(c1.getId()),
                List.of(invalidImageId)
        );

        // when & then
        mvc.perform(put("/api/posts/{postId}", post.getId())
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("FILE_004"))
                .andExpect(jsonPath("$.message").value("파일 정보를 찾을 수 없습니다."));
    }

    @Test
    @DisplayName("게시글 수정 실패 - 잘못된 요청(필드 누락) → 400 Bad Request")
    void updatePost_fail_badRequest() throws Exception {
        // given
        User user = User.createUser("writer5", "writer5@example.com", passwordEncoder.encode("P@ssw0rd!"));
        user.setUserProfile(new UserProfile(user, "작성자5", null, null, null, 0));
        user.setUserStatus(UserStatus.ACTIVE);
        userRepository.save(user);

        String accessToken = generateAccessToken(user);

        String invalidJson = """
                {
                  "content": "본문만 있음"
                }
                """;

        // when & then
        mvc.perform(put("/api/posts/{postId}", 1L)
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON_400"))
                .andExpect(jsonPath("$.message").value("잘못된 요청입니다."));
    }

    @Test
    @DisplayName("게시글 수정 실패 - 인증 없음 → 401 Unauthorized")
    void updatePost_fail_unauthorized() throws Exception {
        // given
        PostRequest request = new PostRequest("제목", "내용", null, List.of(), List.of());

        // when & then
        mvc.perform(put("/api/posts/{postId}", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_001"))
                .andExpect(jsonPath("$.message").value("인증이 필요합니다."));
    }

    // ====================== 게시글 삭제 테스트 ======================

    @Test
    @DisplayName("게시글 삭제 성공 → 200 OK")
    void deletePost_success() throws Exception {
        // given
        User user = User.createUser("writer", "writer@example.com", passwordEncoder.encode("P@ssw0rd!"));
        user.setUserProfile(new UserProfile(user, "홍길동", null, null, null, 0));
        user.setUserStatus(UserStatus.ACTIVE);
        userRepository.save(user);

        Post post = new Post(user, "삭제할 제목", "삭제할 내용", null);
        postRepository.save(post);

        String accessToken = generateAccessToken(user);

        // when & then
        mvc.perform(delete("/api/posts/{postId}", post.getId())
                        .header("Authorization", "Bearer " + accessToken))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value("SUCCESS_200"))
                .andExpect(jsonPath("$.message").value("게시글이 삭제되었습니다."))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    @DisplayName("게시글 삭제 실패 - 게시글 없음 → 404 Not Found")
    void deletePost_fail_postNotFound() throws Exception {
        // given
        User user = User.createUser("writer2", "writer2@example.com", passwordEncoder.encode("P@ssw0rd!"));
        user.setUserProfile(new UserProfile(user, "작성자", null, null, null, 0));
        user.setUserStatus(UserStatus.ACTIVE);
        userRepository.save(user);

        String accessToken = generateAccessToken(user);

        // when & then
        mvc.perform(delete("/api/posts/{postId}", 999L)
                        .header("Authorization", "Bearer " + accessToken))
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("POST_001"))
                .andExpect(jsonPath("$.message").value("존재하지 않는 게시글입니다."));
    }

    @Test
    @DisplayName("게시글 삭제 실패 - 작성자 아님 → 403 Forbidden")
    void deletePost_fail_noPermission() throws Exception {
        // given
        User writer = User.createUser("writer3", "writer3@example.com", passwordEncoder.encode("P@ssw0rd!"));
        writer.setUserProfile(new UserProfile(writer, "작성자3", null, null, null, 0));
        writer.setUserStatus(UserStatus.ACTIVE);
        userRepository.save(writer);

        User another = User.createUser("other", "other@example.com", passwordEncoder.encode("P@ssw0rd!"));
        another.setUserProfile(new UserProfile(another, "다른사람", null, null, null, 0));
        another.setUserStatus(UserStatus.ACTIVE);
        userRepository.save(another);

        Post post = new Post(writer, "원래 제목", "원래 내용", null);
        postRepository.save(post);

        String accessToken = generateAccessToken(another);

        // when & then
        mvc.perform(delete("/api/posts/{postId}", post.getId())
                        .header("Authorization", "Bearer " + accessToken))
                .andDo(print())
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("POST_002"))
                .andExpect(jsonPath("$.message").value("게시글 작성자만 수정/삭제할 수 있습니다."));
    }

    @Test
    @DisplayName("게시글 삭제 실패 - 인증 없음 → 401 Unauthorized")
    void deletePost_fail_unauthorized() throws Exception {
        // given
        Post post = new Post(); // 굳이 저장 안 해도 됨, 그냥 요청만 보냄

        // when & then
        mvc.perform(delete("/api/posts/{postId}", 1L))
                .andDo(print())
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_001"))
                .andExpect(jsonPath("$.message").value("인증이 필요합니다."));
    }
}
