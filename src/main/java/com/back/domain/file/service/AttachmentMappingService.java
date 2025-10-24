package com.back.domain.file.service;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.DeleteObjectRequest;
import com.back.domain.file.entity.AttachmentMapping;
import com.back.domain.file.entity.EntityType;
import com.back.domain.file.entity.FileAttachment;
import com.back.domain.file.repository.AttachmentMappingRepository;
import com.back.domain.file.repository.FileAttachmentRepository;
import com.back.global.exception.CustomException;
import com.back.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AttachmentMappingService {
    @Value("${cloud.aws.s3.bucket}")
    private String bucket;
    private final AmazonS3 amazonS3;
    private final AttachmentMappingRepository attachmentMappingRepository;
    private final FileAttachmentRepository fileAttachmentRepository;

    /**
     * 특정 엔티티에 첨부파일 매핑 생성 (게시글, 프로필 등 공통 사용)
     * - 첨부파일 ID 유효성 검증
     * - 업로더(userId) 일치 여부 확인
     * - 매핑(AttachmentMapping) 엔티티 생성 및 저장
     *
     * @param entityType    엔티티 종류 (POST, PROFILE 등)
     * @param entityId      엔티티 ID
     * @param userId        파일 업로더 검증용
     * @param attachmentIds 파일 ID 리스트 (null 또는 빈 리스트면 생성 없음)
     */
    @Transactional
    public List<FileAttachment> createAttachments(
            EntityType entityType,
            Long entityId,
            Long userId,
            List<Long> attachmentIds
    ) {
        if (attachmentIds == null || attachmentIds.isEmpty()) {
            return List.of();
        }

        // 파일 ID 유효성 검증 및 업로더 확인
        List<FileAttachment> attachments = fileAttachmentRepository.findAllById(attachmentIds);
        if (attachments.size() != attachmentIds.size()) {
            throw new CustomException(ErrorCode.FILE_NOT_FOUND);
        }

        // 업로더 검증
        boolean hasInvalid = attachments.stream()
                .anyMatch(attachment -> !attachment.getUser().getId().equals(userId));
        if (hasInvalid) {
            throw new CustomException(ErrorCode.FILE_ACCESS_DENIED);
        }

        // 매핑 생성
        for (FileAttachment attachment : attachments) {
            attachmentMappingRepository.save(new AttachmentMapping(attachment, entityType, entityId));
        }

        return attachments;
    }

    /**
     * 특정 엔티티의 첨부파일 매핑 갱신 (게시글, 프로필 등 공통 사용)
     * 기존 매핑 및 파일 삭제 후, 새 첨부파일 목록으로 교체
     *
     * @param entityType       엔티티 종류 (POST, PROFILE 등)
     * @param entityId         엔티티 ID
     * @param userId           파일 업로더 검증용
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

        if (newAttachmentIds == null || newAttachmentIds.isEmpty()) {
            return;
        }

        List<FileAttachment> attachments = fileAttachmentRepository.findAllById(newAttachmentIds);
        if (attachments.size() != newAttachmentIds.size()) {
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
     * 특정 엔티티의 첨부파일 매핑 갱신 (차이 기반 업데이트)
     * - 변경 없음 → 유지
     * - 새 첨부 추가 → 매핑 생성
     * - 기존 첨부 제거 → 매핑 및 S3 파일 삭제
     *
     * @param entityType       엔티티 종류 (POST, PROFILE 등)
     * @param entityId         엔티티 ID
     * @param userId           파일 업로더 검증용
     * @param newAttachmentIds 새 파일 ID 리스트 (null 또는 빈 리스트면 삭제만 수행)
     */
    @Transactional
    public List<FileAttachment> updateAttachments(
            EntityType entityType,
            Long entityId,
            Long userId,
            List<Long> newAttachmentIds
    ) {
        if (newAttachmentIds == null || newAttachmentIds.isEmpty()) {
            deleteAttachments(entityType, entityId, userId);
            return List.of();
        }

        // 기존 매핑 조회
        List<AttachmentMapping> existingMappings =
                attachmentMappingRepository.findAllByEntityTypeAndEntityId(entityType, entityId);
        List<Long> existingIds = existingMappings.stream()
                .map(m -> m.getFileAttachment().getId())
                .toList();

        // 변경 없음 → 그대로 반환
        if (existingIds.equals(newAttachmentIds)) {
            return existingMappings.stream()
                    .map(AttachmentMapping::getFileAttachment)
                    .toList();
        }

        // 제거된 매핑 삭제
        List<Long> removedIds = existingIds.stream()
                .filter(id -> !newAttachmentIds.contains(id))
                .toList();
        deleteAttachmentsByIds(userId, removedIds);

        // 새로 추가된 매핑 생성
        List<Long> addedIds = newAttachmentIds.stream()
                .filter(id -> !existingIds.contains(id))
                .toList();
        createAttachments(entityType, entityId, userId, addedIds);

        // 최신 매핑 반환
        return attachmentMappingRepository.findAllByEntityTypeAndEntityId(entityType, entityId)
                .stream()
                .map(AttachmentMapping::getFileAttachment)
                .toList();
    }

    /**
     * 특정 EntityType과 entityId에 연결된 첨부 파일을 모두 삭제
     * - S3 객체 삭제
     * - 매핑 테이블 + 파일 정보 삭제
     */
    @Transactional
    public void deleteAttachments(EntityType entityType, Long entityId, Long userId) {
        List<AttachmentMapping> mappings = attachmentMappingRepository.findAllByEntityTypeAndEntityId(
                entityType,
                entityId
        );

        for (AttachmentMapping mapping : mappings) {
            FileAttachment attachment = mapping.getFileAttachment();

            if (attachment != null) {
                // S3 오브젝트 삭제
                s3Delete(attachment.getStoredName());
            }
        }

        // 매핑 테이블 + 파일 정보 삭제
        attachmentMappingRepository.deleteAllByEntityTypeAndEntityId(entityType, entityId);
    }

    /**
     * 특정 첨부파일 ID 목록만 삭제
     * - S3 객체 삭제
     * - 매핑 테이블에서 제거
     *
     * @param userId        파일 업로더 검증용
     * @param attachmentIds 삭제할 파일 ID 리스트
     */
    @Transactional
    public void deleteAttachmentsByIds(Long userId, List<Long> attachmentIds) {
        if (attachmentIds == null || attachmentIds.isEmpty()) return;

        // 삭제할 매핑만 조회
        List<AttachmentMapping> mappings = attachmentMappingRepository.findAllByFileAttachmentIdIn(attachmentIds);

        // 유효성 및 삭제 처리
        for (AttachmentMapping mapping : mappings) {
            FileAttachment attachment = mapping.getFileAttachment();

            if (!attachment.getUser().getId().equals(userId)) {
                throw new CustomException(ErrorCode.FILE_ACCESS_DENIED);
            }

            s3Delete(attachment.getStoredName());
            attachmentMappingRepository.delete(mapping);
        }
    }

    private void s3Delete(String fileName) {
        amazonS3.deleteObject(new DeleteObjectRequest(bucket, fileName));
    }
}

