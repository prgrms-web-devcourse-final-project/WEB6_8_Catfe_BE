package com.back.domain.file.service;

import com.back.domain.file.entity.AttachmentMapping;
import com.back.domain.file.entity.EntityType;
import com.back.domain.file.entity.FileAttachment;
import com.back.domain.file.repository.AttachmentMappingRepository;
import com.back.domain.file.repository.FileAttachmentRepository;
import com.back.global.exception.CustomException;
import com.back.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AttachmentMappingService {
    private final AttachmentMappingRepository attachmentMappingRepository;
    private final FileService fileService;
    private final FileAttachmentRepository fileAttachmentRepository;

    /**
     * 특정 엔티티의 첨부파일 매핑 갱신 (게시글, 프로필 등 공통 사용)
     * 기존 매핑 및 파일 삭제 후, 새 첨부파일 목록으로 교체
     *
     * @param entityType  엔티티 종류 (POST, PROFILE 등)
     * @param entityId    엔티티 ID
     * @param userId      파일 업로더 검증용
     * @param newAttachmentIds 새 파일 ID 리스트 (null 또는 빈 리스트면 삭제만 수행)
     */
    @Transactional
    public void replaceAttachments(
            EntityType entityType,
            Long entityId,
            Long userId,
            List<Long> newAttachmentIds
    ) {
        // 기존 매핑 및 파일 삭제
        deleteAttachments(entityType, entityId, userId);

        if(newAttachmentIds == null || newAttachmentIds.isEmpty()) {
            return;
        }

        List<FileAttachment> attachments = fileAttachmentRepository.findAllById(newAttachmentIds);
        if(attachments.size() != newAttachmentIds.size()) {
            throw new CustomException(ErrorCode.FILE_NOT_FOUND);
        }

        for (FileAttachment attachment : attachments) {
            if (!attachment.getUser().getId().equals(userId)) {
                throw new CustomException(ErrorCode.FILE_ACCESS_DENIED);
            }
            attachmentMappingRepository.save(new AttachmentMapping(attachment, entityType, entityId));
        }
    }

    // URL로 갱신하는 경우
    @Transactional
    public void replaceAttachmentByUrl(
            EntityType entityType,
            Long entityId,
            Long userId,
            String newImageUrl
    ) {
        deleteAttachments(entityType, entityId, userId);

        if (newImageUrl == null || newImageUrl.isBlank()) return;

        FileAttachment attachment = fileAttachmentRepository
                .findByPublicURL(newImageUrl)
                .orElseThrow(() -> new CustomException(ErrorCode.FILE_NOT_FOUND));

        if (!attachment.getUser().getId().equals(userId)) {
            throw new CustomException(ErrorCode.FILE_ACCESS_DENIED);
        }

        attachmentMappingRepository.save(new AttachmentMapping(attachment, entityType, entityId));
    }

    /**
     * 특정 EntityType과 entityId에 연결된 첨부 파일을 모두 삭제
     * - 매핑 테이블(AttachmentMapping) 삭제
     * - 실제 파일(FileAttachment + S3 객체) 삭제
     */
    @Transactional
    public void deleteAttachments(EntityType entityType, Long entityId, Long userId) {
        List<AttachmentMapping> mappings = attachmentMappingRepository.findAllByEntityTypeAndEntityId(
          entityType,
          entityId
        );

        for(AttachmentMapping mapping : mappings) {
            FileAttachment attachment = mapping.getFileAttachment();

            if(attachment != null) {
                // fileAttachment 테이블 및 연관된 S3 오브젝트 삭제
                fileService.deleteFile(attachment.getId(), userId);
            }
        }

        // 매핑 테이블 삭제
        attachmentMappingRepository.deleteAllByEntityTypeAndEntityId(entityType, entityId);
    }
}
