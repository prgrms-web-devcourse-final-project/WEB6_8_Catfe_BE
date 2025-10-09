package com.back.domain.file.service;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.back.domain.file.dto.FileReadResponseDto;
import com.back.domain.file.dto.FileUploadResponseDto;
import com.back.domain.file.entity.AttachmentMapping;
import com.back.domain.file.entity.EntityType;
import com.back.domain.file.entity.FileAttachment;
import com.back.domain.file.repository.AttachmentMappingRepository;
import com.back.domain.file.repository.FileAttachmentRepository;
import com.back.domain.user.entity.User;
import com.back.domain.user.repository.UserRepository;
import com.back.global.exception.CustomException;
import com.back.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FileService {
    @Value("${cloud.aws.s3.bucket}")
    private String bucket;
    private final AmazonS3 amazonS3;
    private final FileAttachmentRepository fileAttachmentRepository;
    private final UserRepository userRepository;
    private final AttachmentMappingRepository attachmentMappingRepository;
    @Transactional
    public FileUploadResponseDto uploadFile(
            MultipartFile multipartFile,
            EntityType entityType,
            Long entityId,
            Long userId
    ) {
        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new CustomException(ErrorCode.USER_NOT_FOUND)
                );

        // S3에 저장할 파일 이름
        String storedFileName = createFileName(multipartFile.getOriginalFilename());

        // 업로드된 파일의 메타 데이터 정보
        ObjectMetadata objectMetadata = new ObjectMetadata();
        objectMetadata.setContentLength(multipartFile.getSize());
        objectMetadata.setContentType(multipartFile.getContentType());

        // S3의 저장된 파일의 public URL
        String filePath = null;
        try (InputStream inputStream = multipartFile.getInputStream()) {
            // S3에 파일을 업로드
            amazonS3.putObject(
                    new PutObjectRequest(
                            bucket,
                            storedFileName,
                            inputStream,
                            objectMetadata
                    )
            );

            filePath = amazonS3.getUrl(bucket, storedFileName).toString();

            // FileAttachment 정보 저장
            fileAttachmentRepository.save(
                    new FileAttachment(
                            storedFileName,
                            multipartFile,
                            user,
                            entityType,
                            entityId,
                            filePath
                    )
            );
        } catch (IOException e) {
            // 업로드 실패 시, 예외처리
            throw new CustomException(ErrorCode.FILE_UPLOAD_FAILED);
        }

        return new FileUploadResponseDto(filePath);
    }


    @Transactional(readOnly = true)
    public FileReadResponseDto getFile(
            EntityType entityType,
            Long entityId
    ) {
        AttachmentMapping attachmentMapping = attachmentMappingRepository
                .findByEntityTypeAndEntityId(entityType, entityId)
                .orElseThrow(() -> new CustomException(ErrorCode.ATTACHMENT_MAPPING_NOT_FOUND));

        String filePath = attachmentMapping.getFileAttachment().getFilePath();

        return new FileReadResponseDto(filePath);
    }

    // 파일 이름을 난수화 하기 위한 함수
    private String createFileName(String fileName) {
        return UUID.randomUUID().toString().concat(fileName);
    }
}
