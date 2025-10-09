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

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;

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

    @Autowired
    private PostRepository postRepository;

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

        Post post = new Post(user, "제목", "내용");
        postRepository.save(post);

        String path = "test.png";
        String contentType = "image/png";
        String dirName = "test";

        MockMultipartFile file = new MockMultipartFile("test", path, contentType, "test".getBytes());

        // when
        FileUploadResponseDto res = fileService.uploadFile(file, EntityType.POST, post.getId(), user.getId());

        // then
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

        Post post = new Post(user, "제목", "내용");
        postRepository.save(post);

        String path = "test.png";
        String contentType = "image/png";
        String dirName = "test";
        MockMultipartFile file = new MockMultipartFile("test", path, contentType, "test".getBytes());
        fileService.uploadFile(file, EntityType.POST, post.getId(), user.getId());

        //when
        FileReadResponseDto res = fileService.getFile(EntityType.POST, post.getId());

        // then
        assertThat(res.getImageUrl()).contains(path);
        assertThat(res.getImageUrl()).contains(dirName);
    }
}