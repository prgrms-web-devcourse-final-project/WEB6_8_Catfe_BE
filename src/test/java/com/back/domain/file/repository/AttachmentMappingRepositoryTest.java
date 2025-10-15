package com.back.domain.file.repository;

import com.back.domain.file.entity.AttachmentMapping;
import com.back.domain.file.entity.EntityType;
import com.back.domain.file.entity.FileAttachment;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;


import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
@ActiveProfiles("test")
class AttachmentMappingRepositoryTest {
    @Autowired
    private FileAttachmentRepository fileAttachmentRepository;
    @Autowired
    private AttachmentMappingRepository attachmentMappingRepository;
    @Autowired
    private EntityManager em;

    @Test
    void AttachmentMapping_삭제시_FileAttachment_자동삭제_확인() throws Exception {
        // given
        FileAttachment fileAttachment = new FileAttachment(
                "test",
                new MockMultipartFile("test", "test", "image", "test".getBytes()),
                null,
                "test.URL"
        );
        fileAttachmentRepository.save(fileAttachment);
        System.out.println("현재 저장된 파일 개수 : " + fileAttachmentRepository.findAll().size());

        AttachmentMapping attachmentMapping = new AttachmentMapping(
                fileAttachment,
                EntityType.POST,
                1L
        );
        attachmentMappingRepository.save(attachmentMapping);

        // when
        attachmentMappingRepository.deleteAllByEntityTypeAndEntityId(EntityType.POST, 1L);

        // then
        assertThat(fileAttachmentRepository.findAll().size()).isEqualTo(0);
    }
}