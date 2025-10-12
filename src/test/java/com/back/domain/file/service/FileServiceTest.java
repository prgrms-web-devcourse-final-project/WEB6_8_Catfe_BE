package com.back.domain.file.service;

import com.back.domain.board.post.entity.Post;
import com.back.domain.board.post.repository.PostRepository;
import com.back.domain.file.config.S3MockConfig;
import com.back.domain.file.dto.FileReadResponseDto;
import com.back.domain.file.dto.FileUploadResponseDto;
import com.back.domain.file.entity.EntityType;
import com.back.domain.user.entity.User;
import com.back.domain.user.entity.UserProfile;
import com.back.domain.user.entity.UserStatus;
import com.back.domain.user.repository.UserRepository;
import com.back.global.exception.CustomException;
import com.back.global.exception.ErrorCode;
import io.findify.s3mock.S3Mock;
import jakarta.persistence.EntityManager;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.io.FileNotFoundException;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@Import(S3MockConfig.class)
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class FileServiceTest {
    @Autowired
    private S3Mock s3Mock;

    @Autowired
    private FileService fileService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @AfterEach
    public void tearDown() {
        s3Mock.stop();
    }

    @Test
    void uploadFile() {
        // given
        User user = User.createUser("writer", "writer@example.com", passwordEncoder.encode("P@ssw0rd!"));
        user.setUserProfile(new UserProfile(user, "홍길동", null, "소개글", LocalDate.of(2000, 1, 1), 1000));
        user.setUserStatus(UserStatus.ACTIVE);
        userRepository.save(user);

        String path = "test.png";
        String contentType = "image/png";
        String dirName = "test";

        MockMultipartFile file = new MockMultipartFile("test", path, contentType, "test".getBytes());

        // when
        FileUploadResponseDto res = fileService.uploadFile(file, user.getId());

        // then
        assertThat(res.getAttachmentId()).isPositive();
        assertThat(res.getImageUrl()).contains(path);
        assertThat(res.getImageUrl()).contains(dirName);
    }

    @Test
    void readFile() {
        // given
        User user = User.createUser("writer", "writer@example.com", passwordEncoder.encode("P@ssw0rd!"));
        user.setUserProfile(new UserProfile(user, "홍길동", null, "소개글", LocalDate.of(2000, 1, 1), 1000));
        user.setUserStatus(UserStatus.ACTIVE);
        userRepository.save(user);

        String path = "test.png";
        String contentType = "image/png";
        String dirName = "test";
        MockMultipartFile file = new MockMultipartFile("test", path, contentType, "test".getBytes());
        Long attachmentId = fileService.uploadFile(file, user.getId()).getAttachmentId();

        //when
        FileReadResponseDto res = fileService.getFile(attachmentId);

        // then
        assertThat(res.getImageUrl()).contains(path);
        assertThat(res.getImageUrl()).contains(dirName);
    }

    @Test
    void updateFile() {
        // given
        User user = User.createUser("writer", "writer@example.com", passwordEncoder.encode("P@ssw0rd!"));
        user.setUserProfile(new UserProfile(user, "홍길동", null, "소개글", LocalDate.of(2000, 1, 1), 1000));
        user.setUserStatus(UserStatus.ACTIVE);
        userRepository.save(user);

        // 기존(삭제할) 파일 정보
        String path = "test.png";
        String contentType = "image/png";
        MockMultipartFile oldFile = new MockMultipartFile("test", path, contentType, "test".getBytes());
        Long attachmentId = fileService.uploadFile(oldFile, user.getId()).getAttachmentId();

        // 새 파일 정보
        String newPath = "newTest.png";
        String newDirName = "newTest";
        MockMultipartFile newFile = new MockMultipartFile("newTest", newPath, contentType, "newTest".getBytes());

        // when
        fileService.updateFile(attachmentId, newFile, user.getId());
        FileReadResponseDto res = fileService.getFile(attachmentId);

        // then
        assertThat(res.getImageUrl()).contains(newPath);
        assertThat(res.getImageUrl()).contains(newDirName);
    }

    @Test
    void deleteFile() {
        // given
        User user = User.createUser("writer", "writer@example.com", passwordEncoder.encode("P@ssw0rd!"));
        user.setUserProfile(new UserProfile(user, "홍길동", null, "소개글", LocalDate.of(2000, 1, 1), 1000));
        user.setUserStatus(UserStatus.ACTIVE);
        userRepository.save(user);

        // 기존(삭제할) 파일 정보
        String path = "test.png";
        String contentType = "image/png";
        String dirName = "test";
        MockMultipartFile oldFile = new MockMultipartFile("test", path, contentType, "test".getBytes());
        Long attachmentId = fileService.uploadFile(oldFile, user.getId()).getAttachmentId();


        // when
        fileService.deleteFile(attachmentId, user.getId());
        CustomException exception = assertThrows(CustomException.class, () -> {
            fileService.getFile(attachmentId);
        });

        // then
        assertEquals(ErrorCode.FILE_NOT_FOUND, exception.getErrorCode());
    }
}