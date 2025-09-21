package com.back.domain.file.entity;

import com.back.domain.user.entity.User;
import com.back.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@NoArgsConstructor
public class FileAttachment extends BaseEntity {
    private String storedName;

    private String originalName;

    private String filePath;

    private int fileSize;

    @Enumerated(EnumType.STRING)
    private MimeType mimeType;

    // 업로드 유저
    @ManyToOne
    @JoinColumn(name = "uploaded_by")
    private User user;

    @OneToMany(mappedBy = "fileAttachment", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<AttachmentMapping> attachmentMappings = new ArrayList<>();
}
