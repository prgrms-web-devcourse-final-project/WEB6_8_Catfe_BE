package com.back.domain.file.service;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.DeleteObjectRequest;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.back.domain.file.dto.FileReadResponseDto;
import com.back.domain.file.dto.FileUploadResponseDto;
import com.back.domain.file.entity.AttachmentMapping;
import com.back.domain.file.entity.EntityType;
import com.back.domain.file.entity.FileAttachment;
import com.back.domain.file.repository.AttachmentMappingRepository;
import com.back.domain.file.repository.FileAttachmentRepository;
import com.back.domain.file.util.EntityValidator;
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
    private final EntityValidator entityValidator;

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

        // S3의 저장된 파일의 PublicURL
        String filePath = s3Upload(storedFileName, multipartFile);

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

        return new FileUploadResponseDto(filePath);
    }


    @Transactional(readOnly = true)
    public FileReadResponseDto getFile(
            EntityType entityType,
            Long entityId
    ) {
        FileAttachment fileAttachment = getFileAttachmentOrThrow(entityType, entityId);

        String filePath = fileAttachment.getFilePath();

        return new FileReadResponseDto(filePath);
    }

    @Transactional
    public void updateFile(
            MultipartFile multipartFile,
            EntityType entityType,
            Long entityId,
            Long userId
    ) {
        entityValidator.validate(entityType, entityId);

        FileAttachment fileAttachment = getFileAttachmentOrThrow(entityType, entityId);

        checkAccessPermission(fileAttachment, userId);

        // 현재 저장된(삭제할) 파일 이름
        String oldStoredName = fileAttachment.getStoredName();

        // S3에 새롭게 저장할 파일 이름
        String newStoredName = createFileName(multipartFile.getOriginalFilename());

        String filePath = s3Upload(newStoredName, multipartFile);

        s3Delete(oldStoredName);

        // fileAttachment 정보 업데이트
        fileAttachment.update(newStoredName, multipartFile, filePath);
    }

    @Transactional
    public void deleteFile(EntityType entityType, Long entityId, Long userId) {
        entityValidator.validate(entityType, entityId);

        AttachmentMapping attachmentMapping = attachmentMappingRepository
                .findByEntityTypeAndEntityId(entityType, entityId)
                .orElseThrow(() ->
                        new CustomException(ErrorCode.ATTACHMENT_MAPPING_NOT_FOUND)
                );

        FileAttachment fileAttachment = attachmentMapping.getFileAttachment();

        checkAccessPermission(fileAttachment, userId);

        s3Delete(fileAttachment.getStoredName());

        // fileAttachment 정보 삭제
        fileAttachmentRepository.delete(fileAttachment);
    }

    // S3 오브젝트 생성
    private String s3Upload(
            String storedFileName,
            MultipartFile multipartFile
    ) {
        // 업로드된 파일의 메타 데이터 정보
        ObjectMetadata objectMetadata = new ObjectMetadata();
        objectMetadata.setContentLength(multipartFile.getSize());
        objectMetadata.setContentType(multipartFile.getContentType());

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
        } catch (IOException e) {
            // 업로드 실패 시, 예외처리
            throw new CustomException(ErrorCode.FILE_UPLOAD_FAILED);
        }

        return filePath;
    }

    // S3 오브젝트 삭제
    private void s3Delete(String fileName) {
        amazonS3.deleteObject(new DeleteObjectRequest(bucket, fileName));
    }

    // 파일 이름을 난수화
    private String createFileName(String fileName) {
        return UUID.randomUUID().toString().concat(fileName);
    }

    // 파일 접근 권한 체크
    private void checkAccessPermission(FileAttachment fileAttachment, Long userId) {
        if (fileAttachment.getUser().getId() != userId) {
            throw new CustomException(ErrorCode.FILE_ACCESS_DENIED);
        }
    }

    // AttachmentMapping -> fileAttachment 추출
    private FileAttachment getFileAttachmentOrThrow(EntityType entityType, Long entityId) {
        AttachmentMapping attachmentMapping = attachmentMappingRepository
                .findByEntityTypeAndEntityId(entityType, entityId)
                .orElseThrow(() ->
                        new CustomException(ErrorCode.ATTACHMENT_MAPPING_NOT_FOUND)
                );

        return attachmentMapping.getFileAttachment();
    }
}