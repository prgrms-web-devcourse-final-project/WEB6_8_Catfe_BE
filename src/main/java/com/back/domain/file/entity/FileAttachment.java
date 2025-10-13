package com.back.domain.file.entity;

import com.back.domain.user.entity.User;
import com.back.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@NoArgsConstructor
public class FileAttachment extends BaseEntity {
    private String storedName;

    private String originalName;

    private String publicURL;

    private long fileSize;

    private String contentType;

    // 업로드 유저
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uploaded_by")
    private User user;

    @OneToMany(mappedBy = "fileAttachment", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<AttachmentMapping> attachmentMappings = new ArrayList<>();

    public FileAttachment(
            String storedName,
            MultipartFile multipartFile,
            User user,
            String publicURL
    ) {
        this.storedName = storedName;
        this.originalName = multipartFile.getOriginalFilename();
        this.publicURL = publicURL;
        this.fileSize = multipartFile.getSize();
        this.contentType = multipartFile.getContentType();
        this.user = user;
    }

    public void update(String storedName, MultipartFile multipartFile, String publicURL) {
        this.storedName = storedName;
        this.originalName = multipartFile.getOriginalFilename();
        this.publicURL = publicURL;
        this.fileSize = multipartFile.getSize();
        this.contentType = multipartFile.getContentType();
    }
}
