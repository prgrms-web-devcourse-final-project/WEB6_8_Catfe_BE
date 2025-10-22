package com.back.domain.file.repository;

import com.back.domain.file.entity.AttachmentMapping;
import com.back.domain.file.entity.EntityType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AttachmentMappingRepository extends JpaRepository<AttachmentMapping, Long> {
    @EntityGraph(attributePaths = "fileAttachment")
    List<AttachmentMapping> findAllByFileAttachmentIdIn(List<Long> attachmentIds);

    @EntityGraph(attributePaths = "fileAttachment")
    List<AttachmentMapping> findAllByEntityTypeAndEntityId(EntityType entityType, Long entityId);

    Optional<AttachmentMapping> findByEntityTypeAndEntityId(EntityType entityType, Long entityId);

    void deleteAllByEntityTypeAndEntityId(EntityType entityType, Long entityId);
}