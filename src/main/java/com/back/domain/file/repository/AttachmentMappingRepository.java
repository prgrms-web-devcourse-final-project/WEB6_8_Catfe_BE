package com.back.domain.file.repository;

import com.back.domain.file.entity.AttachmentMapping;
import com.back.domain.file.entity.EntityType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AttachmentMappingRepository extends JpaRepository<AttachmentMapping, Long> {
    Optional<AttachmentMapping> findByEntityTypeAndEntityId(EntityType entityType, Long entityID);
}
