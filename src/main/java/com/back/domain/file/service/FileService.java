package com.back.domain.file.service;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.DeleteObjectRequest;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.back.domain.file.dto.FileReadResponseDto;
import com.back.domain.file.dto.FileUpdateResponseDto;
import com.back.domain.file.dto.FileUploadResponseDto;
import com.back.domain.file.entity.FileAttachment;
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

    @Transactional
    public FileUploadResponseDto uploadFile(
            MultipartFile multipartFile,
            Long userId
    ) {
        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new CustomException(ErrorCode.USER_NOT_FOUND)
                );

        // S3에 저장할 파일 이름
        String storedFileName = createFileName(multipartFile.getOriginalFilename());

        // S3의 저장된 파일의 PublicURL
        String publicURL = s3Upload(storedFileName, multipartFile);

        // FileAttachment 정보 저장
        FileAttachment fileAttachment = fileAttachmentRepository.save(
                new FileAttachment(
                        storedFileName,
                        multipartFile,
                        user,
                        publicURL
                )
        );

        return new FileUploadResponseDto(fileAttachment.getId(), publicURL);
    }


    @Transactional(readOnly = true)
    public FileReadResponseDto getFile(Long attachmentId) {
        FileAttachment fileAttachment = fileAttachmentRepository.findById(attachmentId)
                .orElseThrow(() ->
                        new CustomException(ErrorCode.FILE_NOT_FOUND)
                );

        String publicURL = fileAttachment.getPublicURL();

        return new FileReadResponseDto(publicURL);
    }

    @Transactional
    public FileUpdateResponseDto updateFile(
            Long attachmentId,
            MultipartFile multipartFile,
            Long userId
    ) {
        FileAttachment fileAttachment = fileAttachmentRepository.findById(attachmentId)
                .orElseThrow(() ->
                        new CustomException(ErrorCode.FILE_NOT_FOUND)
                );

        checkAccessPermission(fileAttachment, userId);

        // 현재 저장된(삭제할) 파일 이름
        String oldStoredName = fileAttachment.getStoredName();

        // S3에 새롭게 저장할 파일 이름
        String newStoredName = createFileName(multipartFile.getOriginalFilename());

        String publicURL = s3Upload(newStoredName, multipartFile);

        s3Delete(oldStoredName);

        // fileAttachment 정보 업데이트
        fileAttachment.update(newStoredName, multipartFile, publicURL);
        return new FileUpdateResponseDto(publicURL);
    }

    @Transactional
    public void deleteFile(Long attachmentId, Long userId) {
        FileAttachment fileAttachment = fileAttachmentRepository.findById(attachmentId)
                .orElseThrow(() ->
                        new CustomException(ErrorCode.FILE_NOT_FOUND)
                );

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
        if (!fileAttachment.getUser().getId().equals(userId)) {
            throw new CustomException(ErrorCode.FILE_ACCESS_DENIED);
        }
    }

    /**
     * URL이 우리 S3 버킷의 파일인지 확인
     * @param url 확인할 URL
     * @return S3 파일이면 true, 외부 URL이면 false
     */
    public boolean isOurS3File(String url) {
        if (url == null || url.trim().isEmpty()) {
            return false;
        }
        
        // S3 URL 패턴 체크
        // 패턴 1: https://bucket-name.s3.amazonaws.com/...
        // 패턴 2: https://s3.amazonaws.com/bucket-name/...
        // 패턴 3: https://bucket-name.s3.ap-northeast-2.amazonaws.com/...
        return url.contains(".s3.") && url.contains(".amazonaws.com") && url.contains(bucket);
    }

    /**
     * S3 URL에서 파일명(Key) 추출
     * @param url S3 전체 URL
     * @return 파일명 (예: "uuid-filename.jpg")
     */
    public String extractFileNameFromUrl(String url) {
        if (url == null || url.trim().isEmpty()) {
            return null;
        }
        
        try {
            // URL에서 마지막 "/" 이후의 파일명 추출
            int lastSlashIndex = url.lastIndexOf('/');
            if (lastSlashIndex >= 0 && lastSlashIndex < url.length() - 1) {
                return url.substring(lastSlashIndex + 1);
            }
        } catch (Exception e) {
            // 추출 실패 시 null 반환
        }
        
        return null;
    }

    /**
     * S3 파일을 파일명으로 삭제
     * RoomService 등 다른 도메인에서 썸네일 삭제 시 사용
     * @param fileName S3에 저장된 파일명
     */
    public void deleteS3FileByName(String fileName) {
        if (fileName == null || fileName.trim().isEmpty()) {
            return;
        }
        
        try {
            s3Delete(fileName);
        } catch (Exception e) {
            // S3 삭제 실패해도 무시 (파일이 이미 없을 수 있음)
            // 로그만 남기고 계속 진행
        }
    }

    /**
     * URL로 S3 파일 삭제 (권한 체크 없음 - 내부 사용용)
     * 우리 S3 파일인지 확인 후 삭제
     * @param url 삭제할 파일의 전체 URL
     */
    public void deleteS3FileByUrl(String url) {
        if (!isOurS3File(url)) {
            // 외부 URL이면 삭제하지 않음
            return;
        }
        
        String fileName = extractFileNameFromUrl(url);
        deleteS3FileByName(fileName);
    }
}