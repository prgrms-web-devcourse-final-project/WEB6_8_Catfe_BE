package com.back.domain.file.service;

import com.back.domain.file.config.S3MockConfig;
import com.back.domain.file.dto.FileUploadResponseDto;
import com.back.domain.file.entity.AttachmentMapping;
import com.back.domain.file.entity.EntityType;
import com.back.domain.file.entity.FileAttachment;
import com.back.domain.file.repository.AttachmentMappingRepository;
import com.back.domain.file.repository.FileAttachmentRepository;
import com.back.domain.user.common.entity.User;
import com.back.domain.user.common.entity.UserProfile;
import com.back.domain.user.common.enums.UserStatus;
import com.back.domain.user.common.repository.UserRepository;
import io.findify.s3mock.S3Mock;
import jakarta.persistence.EntityManager;
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
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Import(S3MockConfig.class)
@SpringBootTest
@Transactional
@ActiveProfiles("test")
class AttachmentMappingServiceTest {
    @Autowired
    private S3Mock s3Mock;

    @Autowired
    private FileService fileService;

    @Autowired
    private AttachmentMappingService attachmentMappingService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private FileAttachmentRepository fileAttachmentRepository;

    @Autowired
    private AttachmentMappingRepository attachmentMappingRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @AfterEach
    public void tearDown() {
        s3Mock.stop();
    }

    @Test
    void createAttachments_success() throws Exception {
        // given
        User user = User.createUser("writer", "writer@example.com", passwordEncoder.encode("P@ssw0rd!"));
        user.setUserProfile(new UserProfile(user, "홍길동", null, "소개글", LocalDate.of(2000, 1, 1), 1000));
        user.setUserStatus(UserStatus.ACTIVE);
        userRepository.save(user);

        MockMultipartFile file1 = new MockMultipartFile("file1", "a.png", "image/png", "aaa".getBytes());
        MockMultipartFile file2 = new MockMultipartFile("file2", "b.png", "image/png", "bbb".getBytes());

        Long id1 = fileService.uploadFile(file1, user.getId()).getAttachmentId();
        Long id2 = fileService.uploadFile(file2, user.getId()).getAttachmentId();

        // when
        List<FileAttachment> attachments = attachmentMappingService.createAttachments(
                EntityType.POST,
                1L,
                user.getId(),
                List.of(id1, id2)
        );

        // then
        assertThat(attachments).hasSize(2);
        List<AttachmentMapping> mappings =
                attachmentMappingRepository.findAllByEntityTypeAndEntityId(EntityType.POST, 1L);
        assertThat(mappings).hasSize(2);
        assertThat(mappings.get(0).getFileAttachment().getUser().getId()).isEqualTo(user.getId());
    }

    @Test
    void deleteAttachments_success() throws Exception {
        // given
        User user = User.createUser("writer", "writer@example.com", passwordEncoder.encode("P@ssw0rd!"));
        user.setUserProfile(new UserProfile(user, "홍길동", null, "소개글", LocalDate.of(2000, 1, 1), 1000));
        user.setUserStatus(UserStatus.ACTIVE);
        userRepository.save(user);

        String path = "test.png";
        String contentType = "image/png";

        MockMultipartFile file = new MockMultipartFile("test", path, contentType, "test".getBytes());

        FileUploadResponseDto res = fileService.uploadFile(file, user.getId());
        FileAttachment fileAttachment = fileAttachmentRepository.findById(res.getAttachmentId()).orElse(null);

        AttachmentMapping attachmentMapping = new AttachmentMapping(fileAttachment, EntityType.POST, 1L);
        attachmentMappingRepository.save(attachmentMapping);

        // when
        attachmentMappingService.deleteAttachments(EntityType.POST, 1L, user.getId());

        // then
        assertThat(attachmentMappingRepository.findAllByEntityTypeAndEntityId(EntityType.POST, 1L)
                .size()).isEqualTo(0);
        assertThat(fileAttachmentRepository.findAll().size()).isEqualTo(0);
    }

    @Test
    void replaceAttachments_success() throws Exception {
        // given
        User user = User.createUser("writer", "writer@example.com", passwordEncoder.encode("P@ssw0rd!"));
        user.setUserProfile(new UserProfile(user, "홍길동", null, "소개글", LocalDate.of(2000, 1, 1), 1000));
        user.setUserStatus(UserStatus.ACTIVE);
        userRepository.save(user);

        // 기존(삭제할) 파일 정보
        String path = "test.png";
        String contentType = "image/png";
        MockMultipartFile oldFile = new MockMultipartFile("test", path, contentType, "test".getBytes());
        Long oldAttachmentId = fileService.uploadFile(oldFile, user.getId()).getAttachmentId();

        // 새 파일 정보
        String newPath = "newTest.png";
        MockMultipartFile newFile = new MockMultipartFile("newTest", newPath, contentType, "newTest".getBytes());
        Long newAttachmentId = fileService.uploadFile(newFile, user.getId()).getAttachmentId();

        FileAttachment fileAttachment = fileAttachmentRepository.findById(oldAttachmentId).orElse(null);
        AttachmentMapping attachmentMapping = new AttachmentMapping(fileAttachment, EntityType.POST, 1L);
        attachmentMappingRepository.save(attachmentMapping);

        // when
        attachmentMappingService.replaceAttachments(EntityType.POST, 1L, user.getId(), List.of(newAttachmentId));

        // then
        AttachmentMapping findMapping = attachmentMappingRepository.findByEntityTypeAndEntityId(EntityType.POST, 1L).orElse(null);
        assertThat(findMapping.getFileAttachment().getId()).isEqualTo(newAttachmentId);
    }

    @Test
    void replaceAttachmentsUrl_success() throws Exception {
        // given
        User user = User.createUser("writer", "writer@example.com", passwordEncoder.encode("P@ssw0rd!"));
        user.setUserProfile(new UserProfile(user, "홍길동", null, "소개글", LocalDate.of(2000, 1, 1), 1000));
        user.setUserStatus(UserStatus.ACTIVE);
        userRepository.save(user);

        // 기존(삭제할) 파일 정보
        String path = "test.png";
        String contentType = "image/png";
        MockMultipartFile oldFile = new MockMultipartFile("test", path, contentType, "test".getBytes());
        Long oldAttachmentId = fileService.uploadFile(oldFile, user.getId()).getAttachmentId();

        // 새 파일 정보
        String newPath = "newTest.png";
        MockMultipartFile newFile = new MockMultipartFile("newTest", newPath, contentType, "newTest".getBytes());
        Long newAttachmentId = fileService.uploadFile(newFile, user.getId()).getAttachmentId();

        FileAttachment fileAttachment = fileAttachmentRepository.findById(oldAttachmentId).orElse(null);
        AttachmentMapping attachmentMapping = new AttachmentMapping(fileAttachment, EntityType.POST, 1L);
        attachmentMappingRepository.save(attachmentMapping);

        // when
        String newPublicURL = fileAttachmentRepository
                .findById(newAttachmentId)
                .orElse(null)
                .getPublicURL();

        attachmentMappingService.replaceAttachmentByUrl(EntityType.POST, 1L, user.getId(), newPublicURL);

        // then
        AttachmentMapping findMapping = attachmentMappingRepository.findByEntityTypeAndEntityId(EntityType.POST, 1L).orElse(null);
        assertThat(findMapping.getFileAttachment().getId()).isEqualTo(newAttachmentId);
    }

    @Test
    void updateAttachments_success() throws Exception {
        // given
        User user = User.createUser("writer", "writer@example.com", passwordEncoder.encode("P@ssw0rd!"));
        user.setUserProfile(new UserProfile(user, "홍길동", null, "소개글", LocalDate.of(2000, 1, 1), 1000));
        user.setUserStatus(UserStatus.ACTIVE);
        userRepository.save(user);

        // 초기 파일 2개 업로드
        Long id1 = fileService.uploadFile(new MockMultipartFile("a", "a.png", "image/png", "a".getBytes()), user.getId()).getAttachmentId();
        Long id2 = fileService.uploadFile(new MockMultipartFile("b", "b.png", "image/png", "b".getBytes()), user.getId()).getAttachmentId();

        attachmentMappingService.createAttachments(EntityType.POST, 1L, user.getId(), List.of(id1, id2));

        // 새로운 파일 업로드 (id3 추가)
        Long id3 = fileService.uploadFile(new MockMultipartFile("c", "c.png", "image/png", "c".getBytes()), user.getId()).getAttachmentId();

        // when (id2 제거, id3 추가)
        List<FileAttachment> result = attachmentMappingService.updateAttachments(
                EntityType.POST,
                1L,
                user.getId(),
                List.of(id1, id3)
        );

        // then
        assertThat(result).hasSize(2);
        List<Long> ids = result.stream().map(FileAttachment::getId).toList();
        assertThat(ids).containsExactlyInAnyOrder(id1, id3);

        // 매핑 테이블도 갱신됨
        List<AttachmentMapping> mappings =
                attachmentMappingRepository.findAllByEntityTypeAndEntityId(EntityType.POST, 1L);
        assertThat(mappings).hasSize(2);
    }

    @Test
    void deleteAttachmentsByIds_success() throws Exception {
        // given
        User user = User.createUser("writer", "writer@example.com", passwordEncoder.encode("P@ssw0rd!"));
        user.setUserProfile(new UserProfile(user, "홍길동", null, "소개글", LocalDate.of(2000, 1, 1), 1000));
        user.setUserStatus(UserStatus.ACTIVE);
        userRepository.save(user);

        Long id1 = fileService.uploadFile(new MockMultipartFile("a", "a.png", "image/png", "a".getBytes()), user.getId()).getAttachmentId();
        Long id2 = fileService.uploadFile(new MockMultipartFile("b", "b.png", "image/png", "b".getBytes()), user.getId()).getAttachmentId();

        attachmentMappingService.createAttachments(EntityType.PROFILE, 100L, user.getId(), List.of(id1, id2));

        // when (id1만 삭제)
        attachmentMappingService.deleteAttachmentsByIds(user.getId(), List.of(id1));

        // then
        List<AttachmentMapping> remaining =
                attachmentMappingRepository.findAllByEntityTypeAndEntityId(EntityType.PROFILE, 100L);
        assertThat(remaining).hasSize(1);
        assertThat(remaining.get(0).getFileAttachment().getId()).isEqualTo(id2);

        // 파일 DB에서도 id1은 제거되어야 함
        assertThat(fileAttachmentRepository.findById(id1)).isEmpty();
    }
}