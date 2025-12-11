package com.back.domain.file.service;

import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

@ExtendWith(MockitoExtension.class)
class PresignedUrlServiceTest {
    @Mock
    private S3Presigner s3Presigner;

    @InjectMocks
    private PresignedUrlService presignedUrlService;

    // 성능체크 이후 테스트 코드는 이후
//    @Test
//    @DisplayName("업로드용 Presigned URL 발급 - 성공")
//    void generate_presigned_url_success() throws Exception {
//        // given
//        String folder = "images";
//        String filename = "test.jpg";
//        String expectedKey = folder + "/fake-uuid-" + filename;
//
//        // Mocked URL 생성
//        URL fakeUrl = new URL(
//                "https://my-test-bucket.s3.ap-northeast-2.amazonaws.com/" + expectedKey
//        );
//
//        PresignedPutObjectRequest fakePresignedRequest = PresignedPutObjectRequest.builder()
//
//        // Mock 동작 정의
//        when(s3Presigner.presignPutObject(any(PutObjectPresignRequest.class)))
//                .thenReturn(fakePresignedRequest);
//
//
//        // when
//        PresignedUrlResponseDto dto =
//                presignedUrlService.generateUploadPresignedUrl(folder, filename);
//
//
//        // then
//
//        // 발급된 Presigned URL 형식 검증
//        assertThat(dto.getUploadUrl()).contains("X-Amz-Algorithm");
//        assertThat(dto.getUploadUrl()).contains("X-Amz-Signature");
//
//        // 예상 S3 오브젝트 파일 형식 검증
//        assertThat(dto.getObjectUrl()).contains(folder);
//        assertThat(dto.getObjectUrl()).contains(filename);
//    }
}