package com.back.domain.file.controller;

import com.back.domain.file.config.S3MockConfig;
import com.back.domain.file.dto.FileUploadResponseDto;
import com.back.domain.file.service.FileService;
import com.back.domain.user.common.entity.User;
import com.back.domain.user.common.entity.UserProfile;
import com.back.domain.user.common.enums.UserStatus;
import com.back.domain.user.common.repository.UserRepository;
import com.back.fixture.TestJwtTokenProvider;
import io.findify.s3mock.S3Mock;
import org.junit.jupiter.api.AfterEach;
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

import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
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
    private S3Mock s3Mock;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private TestJwtTokenProvider testJwtTokenProvider;

    @Autowired
    private FileService fileService;

    private String generateAccessToken(User user) {
        return testJwtTokenProvider.createAccessToken(
                user.getId(),
                user.getUsername(),
                user.getRole().name()
        );
    }

    @AfterEach
    public void tearDown() {
        s3Mock.stop();
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
                        .header("Authorization", "Bearer " + accessToken)
                        .characterEncoding("UTF-8")
        );

        resultActions.andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS_200"))
                .andExpect(jsonPath("$.message").value("파일 업로드 성공"))
                .andDo(print());
    }

    @Test
    @DisplayName("파일 업로드 실패 - 파일 입력이 없는 경우")
    void uploadFile_fail_noFile() throws Exception {
        // given
        User user = User.createUser("writer", "writer@example.com", passwordEncoder.encode("P@ssw0rd!"));
        user.setUserProfile(new UserProfile(user, "홍길동", null, "소개글", LocalDate.of(2000, 1, 1), 1000));
        user.setUserStatus(UserStatus.ACTIVE);
        userRepository.save(user);

        String accessToken = generateAccessToken(user);

        // when
        ResultActions resultActions = mvc.perform(
                multipart("/api/file/upload") // 👈 post() 대신 multipart() 사용
                        .header("Authorization", "Bearer " + accessToken)
                        .characterEncoding("UTF-8")
        );

        // then
        resultActions.andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON_400"))
                .andExpect(jsonPath("$.message").value("잘못된 요청입니다."))
                .andDo(print());
    }

    @Test
    @DisplayName("파일 조회 성공")
    void readFile_success() throws Exception {
        // given
        User user = User.createUser("writer", "writer@example.com", passwordEncoder.encode("P@ssw0rd!"));
        user.setUserProfile(new UserProfile(user, "홍길동", null, "소개글", LocalDate.of(2000, 1, 1), 1000));
        user.setUserStatus(UserStatus.ACTIVE);
        userRepository.save(user);

        String accessToken = generateAccessToken(user);

        MockMultipartFile multipartFile = new MockMultipartFile(
                "multipartFile",
                "test.png",
                "image/png",
                "test".getBytes()
        );

        FileUploadResponseDto fileUploadResponseDto = fileService.uploadFile(multipartFile, user.getId());

        // when
        ResultActions resultActions = mvc.perform(
                get("/api/file/read/" + fileUploadResponseDto.getAttachmentId())
                        .header("Authorization", "Bearer " + accessToken)
        );

        // then
        resultActions.andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS_200"))
                .andExpect(jsonPath("$.message").value("파일 조회 성공"))
                .andExpect(jsonPath("$.data.publicURL").value(fileUploadResponseDto.getPublicURL()))
                .andDo(print());

    }

    @Test
    @DisplayName("파일 조회 실패 - 없는 파일 정보 조회")
    void readFile_failWhenFileNotFound() throws Exception {
        // given
        User user = User.createUser("writer", "writer@example.com", passwordEncoder.encode("P@ssw0rd!"));
        user.setUserProfile(new UserProfile(user, "홍길동", null, "소개글", LocalDate.of(2000, 1, 1), 1000));
        user.setUserStatus(UserStatus.ACTIVE);
        userRepository.save(user);

        String accessToken = generateAccessToken(user);

        Long attachmentId = 10000000L;

        // when
        ResultActions resultActions = mvc.perform(
                get("/api/file/read/" + attachmentId)
                        .header("Authorization", "Bearer " + accessToken)
        );

        // then
        resultActions.andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("FILE_004"))
                .andExpect(jsonPath("$.message").value("파일 정보를 찾을 수 없습니다."))
                .andDo(print());
    }

    @Test
    @DisplayName("파일 수정 성공")
    void updateFile_success() throws Exception {
        // given
        User user = User.createUser("writer", "writer@example.com", passwordEncoder.encode("P@ssw0rd!"));
        user.setUserProfile(new UserProfile(user, "홍길동", null, "소개글", LocalDate.of(2000, 1, 1), 1000));
        user.setUserStatus(UserStatus.ACTIVE);
        userRepository.save(user);

        String accessToken = generateAccessToken(user);

        // 기존(삭제할) 파일 정보
        String path = "test.png";
        String contentType = "image/png";
        MockMultipartFile oldFile = new MockMultipartFile("test", path, contentType, "test".getBytes());
        FileUploadResponseDto fileUploadResponseDto = fileService.uploadFile(oldFile, user.getId());

        // 새 파일 정보
        String newPath = "newTest.png";
        MockMultipartFile newFile = new MockMultipartFile("multipartFile", newPath, contentType, "newTest".getBytes());

        // when
        ResultActions resultActions = mvc.perform(
                multipart("/api/file/update/" + fileUploadResponseDto.getAttachmentId())
                        .file(newFile)  // 파일 필드
                        .header("Authorization", "Bearer " + accessToken)
                        .characterEncoding("UTF-8")
                        .with(request -> {
                            request.setMethod("PUT");
                            return request;
                        }) // PUT 매핑인 것을 나타낸다.
        );

        // then
        resultActions.andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS_200"))
                .andExpect(jsonPath("$.message").value("파일 업데이트 성공"))
                .andExpect(jsonPath("$.data.publicURL", not(fileUploadResponseDto.getPublicURL())))
                .andDo(print());
    }

    @Test
    @DisplayName("파일 수정 실패 - 없는 아이디 조회")
    void updateFile_failWhenFileNotFound() throws Exception {
        // given
        User user = User.createUser("writer", "writer@example.com", passwordEncoder.encode("P@ssw0rd!"));
        user.setUserProfile(new UserProfile(user, "홍길동", null, "소개글", LocalDate.of(2000, 1, 1), 1000));
        user.setUserStatus(UserStatus.ACTIVE);
        userRepository.save(user);

        String accessToken = generateAccessToken(user);

        // 새 파일 정보
        String newPath = "newTest.png";
        String contentType = "image/png";
        MockMultipartFile newFile = new MockMultipartFile("multipartFile", newPath, contentType, "newTest".getBytes());

        Long attachmentId = 1000000L;

        // when
        ResultActions resultActions = mvc.perform(
                multipart("/api/file/update/" + attachmentId)
                        .file(newFile)  // 파일 필드
                        .header("Authorization", "Bearer " + accessToken)
                        .characterEncoding("UTF-8")
                        .with(request -> {
                            request.setMethod("PUT");
                            return request;
                        }) // PUT 매핑인 것을 나타낸다.
        );

        // then
        resultActions.andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("FILE_004"))
                .andExpect(jsonPath("$.message").value("파일 정보를 찾을 수 없습니다."))
                .andDo(print());
    }

    @Test
    @DisplayName("파일 수정 실패 - 파일 입력이 없는 경우")
    void updateFile_fail_noFile() throws Exception {
        // given
        User user = User.createUser("writer", "writer@example.com", passwordEncoder.encode("P@ssw0rd!"));
        user.setUserProfile(new UserProfile(user, "홍길동", null, "소개글", LocalDate.of(2000, 1, 1), 1000));
        user.setUserStatus(UserStatus.ACTIVE);
        userRepository.save(user);

        String accessToken = generateAccessToken(user);

        // 기존(삭제할) 파일 정보
        String path = "test.png";
        String contentType = "image/png";
        MockMultipartFile oldFile = new MockMultipartFile("test", path, contentType, "test".getBytes());
        FileUploadResponseDto fileUploadResponseDto = fileService.uploadFile(oldFile, user.getId());

        // when
        ResultActions resultActions = mvc.perform(
                multipart("/api/file/update/" + fileUploadResponseDto.getAttachmentId())
                        .header("Authorization", "Bearer " + accessToken)
                        .characterEncoding("UTF-8")
                        .with(request -> {
                            request.setMethod("PUT");
                            return request;
                        }) // PUT 매핑인 것을 나타낸다.
        );

        // then
        resultActions.andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON_400"))
                .andExpect(jsonPath("$.message").value("잘못된 요청입니다."))
                .andDo(print());
    }

    @Test
    @DisplayName("파일 수정 성공 - 파일 접근 권한 없음")
    void updateFile_failWhenAccessDenied() throws Exception {
        // given
        User writer = User.createUser("writer", "writer@example.com", passwordEncoder.encode("P@ssw0rd!"));
        writer.setUserProfile(new UserProfile(writer, "홍길동", null, "소개글", LocalDate.of(2000, 1, 1), 1000));
        writer.setUserStatus(UserStatus.ACTIVE);
        userRepository.save(writer);

        User reader = User.createUser("reader", "reader@example.com", passwordEncoder.encode("P@ssw0rd!"));
        reader.setUserProfile(new UserProfile(reader, "홍길순", null, "소개글", LocalDate.of(2000, 1, 1), 1000));
        reader.setUserStatus(UserStatus.ACTIVE);
        userRepository.save(reader);

        String accessToken = generateAccessToken(reader);

        // 기존(삭제할) 파일 정보
        String path = "test.png";
        String contentType = "image/png";
        MockMultipartFile oldFile = new MockMultipartFile("test", path, contentType, "test".getBytes());
        FileUploadResponseDto fileUploadResponseDto = fileService.uploadFile(oldFile, writer.getId());

        // 새 파일 정보
        String newPath = "newTest.png";
        MockMultipartFile newFile = new MockMultipartFile("multipartFile", newPath, contentType, "newTest".getBytes());

        // when
        ResultActions resultActions = mvc.perform(
                multipart("/api/file/update/" + fileUploadResponseDto.getAttachmentId())
                        .file(newFile)  // 파일 필드
                        .header("Authorization", "Bearer " + accessToken)
                        .characterEncoding("UTF-8")
                        .with(request -> {
                            request.setMethod("PUT");
                            return request;
                        }) // PUT 매핑인 것을 나타낸다.
        );

        // then
        resultActions.andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FILE_003"))
                .andExpect(jsonPath("$.message").value("파일을 접근할 권한이 없습니다."))
                .andDo(print());
    }

    @Test
    @DisplayName("파일 삭제 성공")
    void deleteFile_success() throws Exception {
        // given
        User user = User.createUser("writer", "writer@example.com", passwordEncoder.encode("P@ssw0rd!"));
        user.setUserProfile(new UserProfile(user, "홍길동", null, "소개글", LocalDate.of(2000, 1, 1), 1000));
        user.setUserStatus(UserStatus.ACTIVE);
        userRepository.save(user);

        String accessToken = generateAccessToken(user);

        // 기존(삭제할) 파일 정보
        String path = "test.png";
        String contentType = "image/png";
        MockMultipartFile oldFile = new MockMultipartFile("test", path, contentType, "test".getBytes());
        FileUploadResponseDto fileUploadResponseDto = fileService.uploadFile(oldFile, user.getId());

        // when
        ResultActions resultActions = mvc.perform(
                delete("/api/file/delete/" + fileUploadResponseDto.getAttachmentId())
                        .header("Authorization", "Bearer " + accessToken)
        );

        // then
        resultActions.andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS_200"))
                .andExpect(jsonPath("$.message").value("파일 삭제 성공"))
                .andDo(print());
    }

    @Test
    @DisplayName("파일 삭제 실패 - 파일 접근 권한 없음")
    void deleteFile_failWhenAccessDenied() throws Exception {
        // given
        User writer = User.createUser("writer", "writer@example.com", passwordEncoder.encode("P@ssw0rd!"));
        writer.setUserProfile(new UserProfile(writer, "홍길동", null, "소개글", LocalDate.of(2000, 1, 1), 1000));
        writer.setUserStatus(UserStatus.ACTIVE);
        userRepository.save(writer);

        User reader = User.createUser("reader", "reader@example.com", passwordEncoder.encode("P@ssw0rd!"));
        reader.setUserProfile(new UserProfile(reader, "홍길순", null, "소개글", LocalDate.of(2000, 1, 1), 1000));
        reader.setUserStatus(UserStatus.ACTIVE);
        userRepository.save(reader);

        String accessToken = generateAccessToken(reader);

        // 기존(삭제할) 파일 정보
        String path = "test.png";
        String contentType = "image/png";
        MockMultipartFile oldFile = new MockMultipartFile("test", path, contentType, "test".getBytes());
        FileUploadResponseDto fileUploadResponseDto = fileService.uploadFile(oldFile, writer.getId());

        // when
        ResultActions resultActions = mvc.perform(
                delete("/api/file/delete/" + fileUploadResponseDto.getAttachmentId())
                        .header("Authorization", "Bearer " + accessToken)
        );

        // then
        resultActions.andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FILE_003"))
                .andExpect(jsonPath("$.message").value("파일을 접근할 권한이 없습니다."))
                .andDo(print());
    }

    @Test
    @DisplayName("파일 삭제 실패 - 없는 파일 정보 조회")
    void deleteFile_failWhenFileNotFound() throws Exception {
        // given
        User user = User.createUser("writer", "writer@example.com", passwordEncoder.encode("P@ssw0rd!"));
        user.setUserProfile(new UserProfile(user, "홍길동", null, "소개글", LocalDate.of(2000, 1, 1), 1000));
        user.setUserStatus(UserStatus.ACTIVE);
        userRepository.save(user);

        String accessToken = generateAccessToken(user);

        Long attachmentId = 1000000L;

        // when
        ResultActions resultActions = mvc.perform(
                delete("/api/file/delete/" + attachmentId)
                        .header("Authorization", "Bearer " + accessToken)
        );

        // then
        resultActions.andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("FILE_004"))
                .andExpect(jsonPath("$.message").value("파일 정보를 찾을 수 없습니다."))
                .andDo(print());
    }
}