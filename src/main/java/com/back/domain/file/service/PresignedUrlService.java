package com.back.domain.file.service;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.DeleteObjectRequest;
import com.back.domain.file.dto.PresignedUrlResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;

import java.time.Duration;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PresignedUrlService {
    private final S3Presigner s3Presigner;

    private final AmazonS3 amazonS3;

    @Value("${cloud.aws.s3.bucket}")
    private String bucket;

    @Value("${cloud.aws.region.static}")
    private String region;

    public PresignedUrlResponseDto generateUploadPresignedUrl(String folder, String originalFileName) {
        String key = folder + "/" + UUID.randomUUID() + "-" + originalFileName;

        PutObjectRequest objectRequest = PutObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .contentType("image/jpeg")
                .build();

        // Presigned URL 발급 요청
        PresignedPutObjectRequest presignedRequest = s3Presigner.presignPutObject(
                request -> request.putObjectRequest(objectRequest)
                        .signatureDuration(Duration.ofMinutes(5L))
        );

        String objectUrl = String.format(
                "https://%s.s3.%s.amazonaws.com/%s",
                bucket,
                region,
                key
        );

        return new PresignedUrlResponseDto(presignedRequest.url().toString(), objectUrl);
    }

    public void deleteFile(String objectUrl) {
        String key = extractKeyFromUrl(objectUrl);
        amazonS3.deleteObject(new DeleteObjectRequest(bucket, key));
    }

    /** S3 URL -> Key을 반환하는 함수
     * https://bucket.s3.amazonaws.com/images/test.jpg -> images/test.jpg
    **/
    private String extractKeyFromUrl(String url) {
        int idx = url.indexOf(".amazonaws.com/") + ".amazonaws.com/".length();
        return url.substring(idx);
    }
}