package com.back.domain.user.account.service;

import com.back.domain.board.comment.dto.MyCommentResponse;
import com.back.domain.board.comment.repository.CommentRepository;
import com.back.domain.board.common.dto.PageResponse;
import com.back.domain.board.post.dto.PostListResponse;
import com.back.domain.board.post.repository.PostRepository;
import com.back.domain.file.entity.AttachmentMapping;
import com.back.domain.file.entity.EntityType;
import com.back.domain.file.entity.FileAttachment;
import com.back.domain.file.repository.AttachmentMappingRepository;
import com.back.domain.file.repository.FileAttachmentRepository;
import com.back.domain.file.service.FileService;
import com.back.domain.user.account.dto.ChangePasswordRequest;
import com.back.domain.user.account.dto.UserProfileRequest;
import com.back.domain.user.account.dto.UserDetailResponse;
import com.back.domain.user.common.entity.User;
import com.back.domain.user.common.entity.UserProfile;
import com.back.domain.user.common.enums.UserStatus;
import com.back.domain.user.common.repository.UserProfileRepository;
import com.back.domain.user.common.repository.UserRepository;
import com.back.global.exception.CustomException;
import com.back.global.exception.ErrorCode;
import com.back.domain.user.common.util.PasswordValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class AccountService {
    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final CommentRepository commentRepository;
    private final PostRepository postRepository;
    private final FileAttachmentRepository fileAttachmentRepository;
    private final AttachmentMappingRepository attachmentMappingRepository;
    private final FileService fileService;
    private final PasswordEncoder passwordEncoder;

    /**
     * 사용자 정보 조회 서비스
     * 1. 사용자 조회 및 상태 검증
     * 2. UserDetailResponse 변환 및 반환
     */
    public UserDetailResponse getUserInfo(Long userId) {

        // 사용자 조회 및 상태 검증
        User user = getValidUser(userId);

        // UserDetailResponse로 변환하여 반환
        return UserDetailResponse.from(user);
    }

    /**
     * 사용자 프로필 수정 서비스
     * 1. 사용자 조회 및 상태 검증
     * 2. 닉네임 중복 검사
     * 3. UserProfile 업데이트
     * 4. UserDetailResponse 변환 및 반환
     */
    public UserDetailResponse updateUserProfile(Long userId, UserProfileRequest request) {

        // 사용자 조회 및 상태 검증
        User user = getValidUser(userId);

        // 닉네임 중복 검사 (본인 제외)
        if (userProfileRepository.existsByNicknameAndUserIdNot(request.nickname(), userId)) {
            throw new CustomException(ErrorCode.NICKNAME_DUPLICATED);
        }

        // UserProfile 업데이트
        UserProfile profile = user.getUserProfile();
        profile.setNickname(request.nickname());
        profile.setProfileImageUrl(request.profileImageUrl());
        profile.setBio(request.bio());
        profile.setBirthDate(request.birthDate());

        // 프로필 이미지 및 매핑 업데이트
        updateProfileImage(userId, request.profileImageUrl());

        // UserDetailResponse로 변환하여 반환
        return UserDetailResponse.from(user);
    }

    /**
     * 프로필 이미지 및 매핑 교체 로직
     * - 기존 이미지(S3 + FileAttachment + Mapping) 삭제 후 새 매핑 생성
     */
    private void updateProfileImage(Long userId, String newImageUrl) {

        // 기존 매핑 및 파일 삭제
        attachmentMappingRepository.findByEntityTypeAndEntityId(EntityType.PROFILE, userId)
                .ifPresent(existingMapping -> {
                    FileAttachment oldAttachment = existingMapping.getFileAttachment();
                    if (oldAttachment != null) {
                        fileService.deleteFile(oldAttachment.getId(), userId);
                    }
                    attachmentMappingRepository.delete(existingMapping);
                });

        // 새 이미지가 없는 경우
        if (newImageUrl == null || newImageUrl.isBlank()) {
            return;
        }

        // 새 파일 조회 및 검증
        FileAttachment newAttachment = fileAttachmentRepository
                .findByPublicURL(newImageUrl)
                .orElseThrow(() -> new CustomException(ErrorCode.FILE_NOT_FOUND));

        if (!newAttachment.getUser().getId().equals(userId)) {
            throw new CustomException(ErrorCode.FILE_ACCESS_DENIED);
        }

        // 새 매핑 생성 및 저장
        AttachmentMapping newMapping = new AttachmentMapping(newAttachment, EntityType.PROFILE, userId);
        attachmentMappingRepository.save(newMapping);
    }

    /**
     * 비밀번호 변경 서비스
     * 1. 사용자 조회 및 상태 검증
     * 2. 현재 비밀번호 검증
     * 3. 새 비밀번호 정책 검증
     * 4. 비밀번호 변경
     */
    public void changePassword(Long userId, ChangePasswordRequest request) {

        // 사용자 조회 및 상태 검증
        User user = getValidUser(userId);

        // 소셜 로그인 사용자는 비밀번호 변경 불가
        if (user.getProvider() != null) {
            throw new CustomException(ErrorCode.SOCIAL_PASSWORD_CHANGE_FORBIDDEN);
        }

        // 현재 비밀번호 검증
        if (!passwordEncoder.matches(request.currentPassword(), user.getPassword())) {
            throw new CustomException(ErrorCode.INVALID_CREDENTIALS);
        }

        // 새 비밀번호 정책 검증
        PasswordValidator.validate(request.newPassword());

        // 비밀번호 변경
        user.setPassword(passwordEncoder.encode(request.newPassword()));
    }

    /**
     * 사용자 탈퇴 서비스 (soft delete)
     * 1. 사용자 조회 및 상태 검증
     * 2. UserStatus를 DELETED로 변경
     */
    public void deleteUser(Long userId) {

        // 사용자 조회 및 상태 검증
        User user = getValidUser(userId);

        // 프로필 이미지 및 매핑 삭제
        deleteProfileImage(userId);

        // 상태 변경 (soft delete)
        user.setUserStatus(UserStatus.DELETED);

        // 식별 정보 변경 (username, email, provider, providerId)
        user.setUsername("deleted_" + user.getUsername());
        user.setEmail("deleted_" + user.getEmail());
        user.setProvider("deleted_" + user.getProvider());
        user.setProviderId("deleted_" + user.getProviderId());

        // 개인정보 마스킹
        UserProfile profile = user.getUserProfile();
        if (profile != null) {
            profile.setNickname("탈퇴한 회원");
            profile.setProfileImageUrl(null);
            profile.setBio(null);
            profile.setBirthDate(null);
        }
    }

    /**
     * 프로필 이미지 및 매핑 삭제
     */
    private void deleteProfileImage(Long userId) {
        attachmentMappingRepository.findByEntityTypeAndEntityId(EntityType.PROFILE, userId)
                .ifPresent(mapping -> {
                    FileAttachment attachment = mapping.getFileAttachment();
                    if (attachment != null) {
                        fileService.deleteFile(attachment.getId(), userId);
                    }
                    attachmentMappingRepository.delete(mapping);
                });
    }

    /**
     * 내 게시글 목록 조회 서비스
     * 1. 사용자 조회 및 상태 검증
     * 2. 게시글 목록 조회
     * 3. PageResponse 반환
     */
    @Transactional(readOnly = true)
    public PageResponse<PostListResponse> getMyPosts(Long userId, Pageable pageable) {

        // 사용자 조회 및 상태 검증
        User user = getValidUser(userId);

        // 게시글 목록 조회
        Page<PostListResponse> page = postRepository.findPostsByUserId(userId, pageable);

        // 페이지 응답 반환
        return PageResponse.from(page);
    }

    /**
     * 내 댓글 목록 조회 서비스
     * 1. 사용자 조회 및 상태 검증
     * 2. 댓글 목록 조회
     * 3. PageResponse 반환
     */
    @Transactional(readOnly = true)
    public PageResponse<MyCommentResponse> getMyComments(Long userId, Pageable pageable) {

        // 사용자 조회 및 상태 검증
        User user = getValidUser(userId);

        // 댓글 목록 조회
        Page<MyCommentResponse> page = commentRepository.findCommentsByUserId(user.getId(), pageable);

        // 페이지 응답 반환
        return PageResponse.from(page);
    }

    /**
     * 내 북마크 게시글 목록 조회 서비스
     * 1. 사용자 조회 및 상태 검증
     * 2. 북마크 목록 조회
     * 3. PageResponse 반환
     */
    @Transactional(readOnly = true)
    public PageResponse<PostListResponse> getMyBookmarks(Long userId, Pageable pageable) {

        // 사용자 검증
        User user = getValidUser(userId);

        // 북마크된 게시글 조회
        Page<PostListResponse> page = postRepository.findBookmarkedPostsByUserId(user.getId(), pageable);

        // 페이지 응답 반환
        return PageResponse.from(page);
    }

    /**
     * 유효한 사용자 조회 및 상태 검증
     *
     * @param userId 사용자 ID
     * @return user  조회된 사용자 엔티티
     */
    private User getValidUser(Long userId) {

        // userId로 User 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        // UserStatus가 DELETED, SUSPENDED면 예외 처리
        if (user.getUserStatus() == UserStatus.DELETED) {
            throw new CustomException(ErrorCode.USER_DELETED);
        }
        if (user.getUserStatus() == UserStatus.SUSPENDED) {
            throw new CustomException(ErrorCode.USER_SUSPENDED);
        }

        return user;
    }
}
