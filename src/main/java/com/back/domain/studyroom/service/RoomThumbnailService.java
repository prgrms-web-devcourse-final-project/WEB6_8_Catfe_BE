package com.back.domain.studyroom.service;

import com.back.domain.file.entity.AttachmentMapping;
import com.back.domain.file.entity.EntityType;
import com.back.domain.file.entity.FileAttachment;
import com.back.domain.file.repository.AttachmentMappingRepository;
import com.back.domain.file.repository.FileAttachmentRepository;
import com.back.domain.file.service.FileService;
import com.back.global.exception.CustomException;
import com.back.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 스터디룸 썸네일 전용 서비스
 * 매핑 전략에 따라 AttachmentMapping을 통해 관리
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RoomThumbnailService {

    private final FileAttachmentRepository fileAttachmentRepository;
    private final AttachmentMappingRepository attachmentMappingRepository;
    private final FileService fileService;

    /**
     * 방 생성 시 썸네일 매핑 생성
     * 
     * @param roomId 방 ID
     * @param thumbnailAttachmentId 썸네일 파일 ID
     * @return 썸네일 URL
     */
    @Transactional
    public String createThumbnailMapping(Long roomId, Long thumbnailAttachmentId) {
        if (thumbnailAttachmentId == null) {
            return null;
        }

        // FileAttachment 조회
        FileAttachment fileAttachment = fileAttachmentRepository.findById(thumbnailAttachmentId)
                .orElseThrow(() -> new CustomException(ErrorCode.FILE_NOT_FOUND));

        // AttachmentMapping 생성
        AttachmentMapping mapping = new AttachmentMapping(
                fileAttachment,
                EntityType.STUDY_ROOM,
                roomId
        );
        
        attachmentMappingRepository.save(mapping);
        
        log.info("썸네일 매핑 생성 - RoomId: {}, AttachmentId: {}, URL: {}", 
                roomId, thumbnailAttachmentId, fileAttachment.getPublicURL());
        
        return fileAttachment.getPublicURL();
    }

    /**
     * 방 수정 시 썸네일 변경
     * 1. 기존 매핑 및 파일 삭제 (S3 + FileAttachment + Mapping)
     * 2. 새 매핑 생성
     * 
     * @param roomId 방 ID
     * @param newThumbnailAttachmentId 새 썸네일 파일 ID (null이면 변경 없음)
     * @param userId 요청자 ID (파일 삭제 권한 검증용)
     * @return 새 썸네일 URL (null이면 변경 없음)
     */
    @Transactional
    public String updateThumbnailMapping(Long roomId, Long newThumbnailAttachmentId, Long userId) {
        if (newThumbnailAttachmentId == null) {
            // null이면 썸네일 변경 없음
            return null;
        }

        // 기존 매핑 및 파일 삭제 (S3 + FileAttachment 포함)
        List<AttachmentMapping> mappings = attachmentMappingRepository
                .findAllByEntityTypeAndEntityId(EntityType.STUDY_ROOM, roomId);
        
        for (AttachmentMapping mapping : mappings) {
            FileAttachment oldAttachment = mapping.getFileAttachment();
            if (oldAttachment != null) {
                // FileService를 사용하여 S3 파일 + FileAttachment 삭제
                try {
                    fileService.deleteFile(oldAttachment.getId(), userId);
                    log.info("기존 썸네일 파일 삭제 - RoomId: {}, AttachmentId: {}", 
                            roomId, oldAttachment.getId());
                } catch (Exception e) {
                    log.error("썸네일 파일 삭제 실패 - RoomId: {}, AttachmentId: {}, Error: {}", 
                            roomId, oldAttachment.getId(), e.getMessage());
                }
            }
        }
        
        // 매핑 삭제
        attachmentMappingRepository.deleteAllByEntityTypeAndEntityId(
                EntityType.STUDY_ROOM, roomId);
        
        log.info("기존 썸네일 매핑 및 파일 삭제 완료 - RoomId: {}", roomId);

        // 새 매핑 생성
        return createThumbnailMapping(roomId, newThumbnailAttachmentId);
    }

    /**
     * 방 삭제 시 썸네일 매핑 및 파일 삭제
     * S3 파일 + FileAttachment + AttachmentMapping 모두 삭제
     * 
     * @param roomId 방 ID
     * @param userId 요청자 ID (파일 삭제 권한 검증용)
     */
    @Transactional
    public void deleteThumbnailMapping(Long roomId, Long userId) {
        // 매핑 조회
        List<AttachmentMapping> mappings = attachmentMappingRepository
                .findAllByEntityTypeAndEntityId(EntityType.STUDY_ROOM, roomId);
        
        // S3 파일 + FileAttachment 삭제
        for (AttachmentMapping mapping : mappings) {
            FileAttachment fileAttachment = mapping.getFileAttachment();
            if (fileAttachment != null) {
                try {
                    fileService.deleteFile(fileAttachment.getId(), userId);
                    log.info("썸네일 파일 삭제 완료 - RoomId: {}, AttachmentId: {}", 
                            roomId, fileAttachment.getId());
                } catch (Exception e) {
                    log.error("썸네일 파일 삭제 실패 - RoomId: {}, AttachmentId: {}, Error: {}", 
                            roomId, fileAttachment.getId(), e.getMessage());
                }
            }
        }
        
        // AttachmentMapping 삭제
        attachmentMappingRepository.deleteAllByEntityTypeAndEntityId(
                EntityType.STUDY_ROOM, roomId);
        
        log.info("방 삭제 - 썸네일 매핑 및 파일 삭제 완료 - RoomId: {}", roomId);
    }

    /**
     * 방의 썸네일 URL 조회
     * 
     * @param roomId 방 ID
     * @return 썸네일 URL (없으면 null)
     */
    @Transactional(readOnly = true)
    public String getThumbnailUrl(Long roomId) {
        List<AttachmentMapping> mappings = attachmentMappingRepository
                .findAllByEntityTypeAndEntityId(EntityType.STUDY_ROOM, roomId);
        
        if (mappings.isEmpty()) {
            return null;
        }
        
        // 첫 번째 매핑의 URL 반환 (썸네일은 단일 파일)
        return mappings.get(0).getFileAttachment().getPublicURL();
    }

    /**
     * 방의 썸네일 FileAttachment 조회
     * 
     * @param roomId 방 ID
     * @return FileAttachment (없으면 null)
     */
    @Transactional(readOnly = true)
    public FileAttachment getThumbnailAttachment(Long roomId) {
        List<AttachmentMapping> mappings = attachmentMappingRepository
                .findAllByEntityTypeAndEntityId(EntityType.STUDY_ROOM, roomId);
        
        if (mappings.isEmpty()) {
            return null;
        }
        
        // 첫 번째 매핑의 FileAttachment 반환 (썸네일은 단일 파일)
        return mappings.get(0).getFileAttachment();
    }
}
