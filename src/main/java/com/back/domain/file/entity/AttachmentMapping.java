package com.back.domain.file.entity;

import com.back.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor
public class AttachmentMapping extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "attachment_id")
    private FileAttachment fileAttachment;

    @Enumerated(EnumType.STRING)
    private EntityType entityType;

    private Long entityId;
}
