package com.back.domain.file.controller;

import com.back.domain.board.post.entity.Post;
import com.back.domain.board.post.repository.PostRepository;
import com.back.domain.file.config.S3MockConfig;
import com.back.domain.user.entity.User;
import com.back.domain.user.entity.UserProfile;
import com.back.domain.user.entity.UserStatus;
import com.back.domain.user.repository.UserRepository;
import com.back.fixture.TestJwtTokenProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@Import(S3MockConfig.class)
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Transactional
class FileControllerTest {
    @Autowired
    private MockMvc mvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private TestJwtTokenProvider testJwtTokenProvider;

    private String generateAccessToken(User user) {
        return testJwtTokenProvider.createAccessToken(
                user.getId(),
                user.getUsername(),
                user.getRole().name()
        );
    }

    @Test
    @DisplayName("파일 업로드 성공")
    void uploadFile_success() throws Exception {
        // given
        User user = User.createUser("writer", "writer@example.com", passwordEncoder.encode("P@ssw0rd!"));
        user.setUserProfile(new UserProfile(user, "홍길동", null, "소개글", LocalDate.of(2000, 1, 1), 1000));
        user.setUserStatus(UserStatus.ACTIVE);
        userRepository.save(user);

        String accessToken = generateAccessToken(user);

        Post post = new Post(user, "첫 글", "내용", null);
        postRepository.save(post);

        MockMultipartFile multipartFile = new MockMultipartFile(
                "multipartFile",
                "test.png",
                "image/png",
                "test".getBytes()
        );

        // when
        ResultActions resultActions = mvc.perform(
                multipart("/api/file/upload") // 👈 post() 대신 multipart() 사용
                        .file(multipartFile)  // 파일 필드
                        .param("entityType", "POST") // DTO 필드 매핑
                        .param("entityId", post.getId().toString())
                        .header("Authorization", "Bearer " + accessToken)
                        .characterEncoding("UTF-8")
        );

        resultActions.andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS_200"))
                .andExpect(jsonPath("$.message").value("파일 업로드 성공"))
                .andDo(print());
    }

    @Test
    @DisplayName("파일 업로드 실패 - 파일이 없는 경우")
    void uploadFile_fail_noFile() throws Exception {
        // given
        User user = User.createUser("writer", "writer@example.com", passwordEncoder.encode("P@ssw0rd!"));
        user.setUserProfile(new UserProfile(user, "홍길동", null, "소개글", LocalDate.of(2000, 1, 1), 1000));
        user.setUserStatus(UserStatus.ACTIVE);
        userRepository.save(user);

        String accessToken = generateAccessToken(user);

        Post post = new Post(user, "첫 글", "내용", null);
        postRepository.save(post);

        // when
        ResultActions resultActions = mvc.perform(
                multipart("/api/file/upload") // 👈 post() 대신 multipart() 사용
                        .param("entityType", "POST") // DTO 필드 매핑
                        .param("entityId", post.getId().toString())
                        .header("Authorization", "Bearer " + accessToken)
                        .characterEncoding("UTF-8")
        );

        // then
        resultActions.andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON_400"))
                .andExpect(jsonPath("$.message").value("잘못된 요청입니다."))
                .andDo(print());
    }
}