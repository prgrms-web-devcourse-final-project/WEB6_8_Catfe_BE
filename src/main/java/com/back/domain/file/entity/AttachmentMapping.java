package com.back.domain.file.entity;

import com.back.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor
public class AttachmentMapping extends BaseEntity {
    @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "attachment_id", nullable = false)
    private FileAttachment fileAttachment;

    @Enumerated(EnumType.STRING)
    private EntityType entityType;

    private Long entityId;

    public AttachmentMapping(FileAttachment fileAttachment, EntityType entityType, Long entityId) {
        this.fileAttachment = fileAttachment;
        this.entityType = entityType;
        this.entityId = entityId;
    }
}
