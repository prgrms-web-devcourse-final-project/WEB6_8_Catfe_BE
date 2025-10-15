package com.back.domain.file.repository;

import com.back.domain.file.entity.FileAttachment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface FileAttachmentRepository extends JpaRepository<FileAttachment, Long> {
    Optional<FileAttachment> findByPublicURL(String publicUrl);
}