package com.back.domain.file.repository;

import com.back.domain.file.entity.FileAttachment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FileRepository extends JpaRepository<FileAttachment, Long> {
}
