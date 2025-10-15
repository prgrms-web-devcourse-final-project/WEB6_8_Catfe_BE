package com.back.domain.user.account.service;

import com.amazonaws.services.s3.AmazonS3;
import com.back.domain.board.comment.dto.MyCommentResponse;
import com.back.domain.board.comment.entity.Comment;
import com.back.domain.board.comment.repository.CommentRepository;
import com.back.domain.board.common.dto.PageResponse;
import com.back.domain.board.post.dto.PostListResponse;
import com.back.domain.board.post.entity.Post;
import com.back.domain.board.post.entity.PostBookmark;
import com.back.domain.board.post.repository.PostBookmarkRepository;
import com.back.domain.board.post.repository.PostRepository;
import com.back.domain.file.entity.AttachmentMapping;
import com.back.domain.file.entity.EntityType;
import com.back.domain.file.entity.FileAttachment;
import com.back.domain.file.repository.AttachmentMappingRepository;
import com.back.domain.file.repository.FileAttachmentRepository;
import com.back.domain.file.service.AttachmentMappingService;
import com.back.domain.user.account.dto.ChangePasswordRequest;
import com.back.domain.user.account.dto.UserProfileRequest;
import com.back.domain.user.account.dto.UserDetailResponse;
import com.back.domain.user.common.entity.User;
import com.back.domain.user.common.entity.UserProfile;
import com.back.domain.user.common.enums.UserStatus;
import com.back.domain.user.common.repository.UserRepository;
import com.back.global.exception.CustomException;
import com.back.global.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
@ActiveProfiles("test")
class AccountServiceTest {

    @Autowired
    private AccountService accountService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private PostBookmarkRepository postBookmarkRepository;

    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private FileAttachmentRepository fileAttachmentRepository;

    @Autowired
    private AttachmentMappingRepository attachmentMappingRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private AttachmentMappingService attachmentMappingService;

    @MockBean
    private AmazonS3 amazonS3; // S3 호출 차단용 mock

    private MultipartFile mockMultipartFile(String filename) {
        return new MockMultipartFile(filename, filename, "image/png", new byte[]{1, 2, 3});
    }

    // ====================== 사용자 정보 조회 테스트 ======================

    @Test
    @DisplayName("정상 유저 정보 조회 성공")
    void getUserInfo_success() {
        // given: 정상 상태의 유저 저장
        User user = User.createUser("testuser", "test@example.com", passwordEncoder.encode("P@ssw0rd!"));
        user.setUserProfile(new UserProfile(user, "홍길동", null, "소개글", LocalDate.of(2000, 1, 1), 1000));
        user.setUserStatus(UserStatus.ACTIVE);
        userRepository.save(user);

        // when: 서비스 호출
        UserDetailResponse response = accountService.getUserInfo(user.getId());

        // then: 응답 값 검증
        assertThat(response.username()).isEqualTo("testuser");
        assertThat(response.profile().nickname()).isEqualTo("홍길동");
    }

    @Test
    @DisplayName("유저 없음 → USER_NOT_FOUND 예외")
    void getUserInfo_userNotFound() {
        // when & then: 존재하지 않는 ID로 조회
        assertThatThrownBy(() -> accountService.getUserInfo(999L))
                .isInstanceOf(CustomException.class)
                .hasMessage(ErrorCode.USER_NOT_FOUND.getMessage());
    }

    @Test
    @DisplayName("탈퇴 유저 조회 → USER_DELETED 예외")
    void getUserInfo_deletedUser() {
        // given: 상태 DELETED 유저 저장
        User user = User.createUser("deleted", "deleted@example.com", passwordEncoder.encode("P@ssw0rd!"));
        user.setUserProfile(new UserProfile(user, "닉네임", null, null, null, 0));
        user.setUserStatus(UserStatus.DELETED);
        userRepository.save(user);

        // when & then
        assertThatThrownBy(() -> accountService.getUserInfo(user.getId()))
                .isInstanceOf(CustomException.class)
                .hasMessage(ErrorCode.USER_DELETED.getMessage());
    }

    @Test
    @DisplayName("정지 유저 조회 → USER_SUSPENDED 예외")
    void getUserInfo_suspendedUser() {
        // given: 상태 SUSPENDED 유저 저장
        User user = User.createUser("suspended", "suspended@example.com", passwordEncoder.encode("P@ssw0rd!"));
        user.setUserProfile(new UserProfile(user, "닉네임", null, null, null, 0));
        user.setUserStatus(UserStatus.SUSPENDED);
        userRepository.save(user);

        // when & then
        assertThatThrownBy(() -> accountService.getUserInfo(user.getId()))
                .isInstanceOf(CustomException.class)
                .hasMessage(ErrorCode.USER_SUSPENDED.getMessage());
    }

    // ====================== 사용자 프로필 수정 테스트 ======================

    @Test
    @DisplayName("프로필 수정 성공")
    void updateUserProfile_success() {
        // given: 정상 유저 저장
        User user = User.createUser("updateuser", "update@example.com", passwordEncoder.encode("P@ssw0rd!"));
        user.setUserProfile(new UserProfile(user, "기존닉", null, null, null, 0));
        user.setUserStatus(UserStatus.ACTIVE);
        userRepository.save(user);

        // 기존 프로필 이미지 매핑 설정
        FileAttachment oldAttachment = new FileAttachment("old_uuid_img.png", mockMultipartFile("old.png"), user, "https://cdn.example.com/old.png");
        fileAttachmentRepository.save(oldAttachment);
        attachmentMappingRepository.save(new AttachmentMapping(oldAttachment, EntityType.PROFILE, user.getUserProfile().getId()));

        // 새 프로필 이미지 업로드된 파일 가정
        FileAttachment newAttachment = new FileAttachment("new_uuid_img.png", mockMultipartFile("new.png"), user, "https://cdn.example.com/new.png");
        fileAttachmentRepository.save(newAttachment);

        UserProfileRequest request = new UserProfileRequest("새닉네임", newAttachment.getPublicURL(), "자기소개", LocalDate.of(1999, 5, 10));

        // when: 서비스 호출
        UserDetailResponse response = accountService.updateUserProfile(user.getId(), request);

        // then
        User updated = userRepository.findById(user.getId()).orElseThrow();
        assertThat(updated.getUserProfile().getNickname()).isEqualTo("새닉네임");
        assertThat(response.profile().nickname()).isEqualTo("새닉네임");

        // 새 매핑이 존재하고 기존 매핑은 삭제되었는지 검증
        List<AttachmentMapping> mappings = attachmentMappingRepository.findAllByEntityTypeAndEntityId(EntityType.PROFILE, user.getUserProfile().getId());
        assertThat(mappings).hasSize(1);
        assertThat(mappings.get(0).getFileAttachment().getPublicURL()).isEqualTo(newAttachment.getPublicURL());

        // 기존 이미지가 삭제되었는지 확인 (테스트 환경에서는 DB 삭제만 검증)
        assertThat(fileAttachmentRepository.findByPublicURL("https://cdn.example.com/old.png")).isEmpty();
    }

    @Test
    @DisplayName("중복 닉네임 수정 → NICKNAME_DUPLICATED 예외")
    void updateUserProfile_duplicateNickname() {
        // given: user1, user2 저장
        User user1 = User.createUser("user1", "user1@example.com", passwordEncoder.encode("P@ssw0rd!"));
        user1.setUserProfile(new UserProfile(user1, "닉1", null, null, null, 0));
        user1.setUserStatus(UserStatus.ACTIVE);
        userRepository.save(user1);

        User user2 = User.createUser("user2", "user2@example.com", passwordEncoder.encode("P@ssw0rd!"));
        user2.setUserProfile(new UserProfile(user2, "닉2", null, null, null, 0));
        user2.setUserStatus(UserStatus.ACTIVE);
        userRepository.save(user2);

        UserProfileRequest request = new UserProfileRequest("닉1", null, null, null);

        // when & then
        assertThatThrownBy(() -> accountService.updateUserProfile(user2.getId(), request))
                .isInstanceOf(CustomException.class)
                .hasMessage(ErrorCode.NICKNAME_DUPLICATED.getMessage());
    }

    @Test
    @DisplayName("탈퇴 유저 프로필 수정 → USER_DELETED 예외")
    void updateUserProfile_deletedUser() {
        // given: 상태 DELETED 유저 저장
        User user = User.createUser("deleted2", "deleted2@example.com", passwordEncoder.encode("P@ssw0rd!"));
        user.setUserProfile(new UserProfile(user, "닉네임", null, null, null, 0));
        user.setUserStatus(UserStatus.DELETED);
        userRepository.save(user);

        UserProfileRequest request = new UserProfileRequest("새닉", null, null, null);

        // when & then
        assertThatThrownBy(() -> accountService.updateUserProfile(user.getId(), request))
                .isInstanceOf(CustomException.class)
                .hasMessage(ErrorCode.USER_DELETED.getMessage());
    }

    @Test
    @DisplayName("정지 유저 프로필 수정 → USER_SUSPENDED 예외")
    void updateUserProfile_suspendedUser() {
        // given: 상태 SUSPENDED 유저 저장
        User user = User.createUser("suspended2", "suspended2@example.com", passwordEncoder.encode("P@ssw0rd!"));
        user.setUserProfile(new UserProfile(user, "닉네임", null, null, null, 0));
        user.setUserStatus(UserStatus.SUSPENDED);
        userRepository.save(user);

        UserProfileRequest request = new UserProfileRequest("새닉", null, null, null);

        // when & then
        assertThatThrownBy(() -> accountService.updateUserProfile(user.getId(), request))
                .isInstanceOf(CustomException.class)
                .hasMessage(ErrorCode.USER_SUSPENDED.getMessage());
    }

    // ====================== 비밀번호 변경 테스트 ======================

    @Test
    @DisplayName("비밀번호 변경 성공")
    void changePassword_success() {
        // given: 정상 유저 저장
        User user = User.createUser("changepw", "changepw@example.com", passwordEncoder.encode("P@ssw0rd!"));
        user.setUserProfile(new UserProfile(user, "닉네임", null, null, null, 0));
        user.setUserStatus(UserStatus.ACTIVE);
        userRepository.save(user);

        ChangePasswordRequest request = new ChangePasswordRequest("P@ssw0rd!", "NewP@ssw0rd!");

        // when
        accountService.changePassword(user.getId(), request);

        // then: DB의 비밀번호가 변경되었는지 확인
        User updated = userRepository.findById(user.getId()).orElseThrow();
        assertThat(passwordEncoder.matches("NewP@ssw0rd!", updated.getPassword())).isTrue();
    }

    @Test
    @DisplayName("현재 비밀번호 불일치 → INVALID_CREDENTIALS 예외")
    void changePassword_invalidCurrentPassword() {
        // given
        User user = User.createUser("wrongpw", "wrongpw@example.com", passwordEncoder.encode("Correct1!"));
        user.setUserProfile(new UserProfile(user, "닉네임", null, null, null, 0));
        user.setUserStatus(UserStatus.ACTIVE);
        userRepository.save(user);

        ChangePasswordRequest request = new ChangePasswordRequest("Wrong1!", "NewP@ssw0rd!");

        // when & then
        assertThatThrownBy(() -> accountService.changePassword(user.getId(), request))
                .isInstanceOf(CustomException.class)
                .hasMessage(ErrorCode.INVALID_CREDENTIALS.getMessage());
    }

    @Test
    @DisplayName("새 비밀번호 정책 위반 → INVALID_PASSWORD 예외")
    void changePassword_invalidNewPassword() {
        // given
        User user = User.createUser("invalidpw", "invalidpw@example.com", passwordEncoder.encode("Valid1!"));
        user.setUserProfile(new UserProfile(user, "닉네임", null, null, null, 0));
        user.setUserStatus(UserStatus.ACTIVE);
        userRepository.save(user);

        // 숫자/특수문자 없는 비밀번호
        ChangePasswordRequest request = new ChangePasswordRequest("Valid1!", "short");

        // when & then
        assertThatThrownBy(() -> accountService.changePassword(user.getId(), request))
                .isInstanceOf(CustomException.class)
                .hasMessage(ErrorCode.INVALID_PASSWORD.getMessage());
    }

    @Test
    @DisplayName("소셜 로그인 회원 비밀번호 변경 시도 → SOCIAL_PASSWORD_CHANGE_FORBIDDEN 예외")
    void changePassword_socialUser() {
        // given
        User user = User.createUser("socialuser", "social@example.com", null);
        user.setProvider("kakao"); // 소셜 로그인 회원
        user.setUserStatus(UserStatus.ACTIVE);
        userRepository.save(user);

        ChangePasswordRequest request = new ChangePasswordRequest("dummy", "NewP@ssw0rd!");

        // when & then
        assertThatThrownBy(() -> accountService.changePassword(user.getId(), request))
                .isInstanceOf(CustomException.class)
                .hasMessage(ErrorCode.SOCIAL_PASSWORD_CHANGE_FORBIDDEN.getMessage());
    }

    @Test
    @DisplayName("탈퇴한 유저 비밀번호 변경 → USER_DELETED 예외")
    void changePassword_deletedUser() {
        // given
        User user = User.createUser("deletedpw", "deletedpw@example.com", passwordEncoder.encode("Valid1!"));
        user.setUserProfile(new UserProfile(user, "닉네임", null, null, null, 0));
        user.setUserStatus(UserStatus.DELETED);
        userRepository.save(user);

        ChangePasswordRequest request = new ChangePasswordRequest("Valid1!", "NewP@ssw0rd!");

        // when & then
        assertThatThrownBy(() -> accountService.changePassword(user.getId(), request))
                .isInstanceOf(CustomException.class)
                .hasMessage(ErrorCode.USER_DELETED.getMessage());
    }

    @Test
    @DisplayName("정지된 유저 비밀번호 변경 → USER_SUSPENDED 예외")
    void changePassword_suspendedUser() {
        // given
        User user = User.createUser("suspendedpw", "suspendedpw@example.com", passwordEncoder.encode("Valid1!"));
        user.setUserProfile(new UserProfile(user, "닉네임", null, null, null, 0));
        user.setUserStatus(UserStatus.SUSPENDED);
        userRepository.save(user);

        ChangePasswordRequest request = new ChangePasswordRequest("Valid1!", "NewP@ssw0rd!");

        // when & then
        assertThatThrownBy(() -> accountService.changePassword(user.getId(), request))
                .isInstanceOf(CustomException.class)
                .hasMessage(ErrorCode.USER_SUSPENDED.getMessage());
    }

    @Test
    @DisplayName("존재하지 않는 유저 비밀번호 변경 → USER_NOT_FOUND 예외")
    void changePassword_userNotFound() {
        // when & then
        ChangePasswordRequest request = new ChangePasswordRequest("dummy", "NewP@ssw0rd!");
        assertThatThrownBy(() -> accountService.changePassword(999L, request))
                .isInstanceOf(CustomException.class)
                .hasMessage(ErrorCode.USER_NOT_FOUND.getMessage());
    }

    // ====================== 사용자 탈퇴 테스트 ======================

    @Test
    @DisplayName("정상 회원 탈퇴 성공")
    void deleteUser_success() {
        // given: 정상 상태의 유저 저장
        User user = User.createUser("deleteuser", "delete@example.com", passwordEncoder.encode("P@ssw0rd!"));
        user.setUserProfile(new UserProfile(user, "홍길동", null, "소개글", LocalDate.of(1995, 3, 15), 500));
        user.setUserStatus(UserStatus.ACTIVE);
        userRepository.save(user);

        // 프로필 이미지 매핑 설정
        FileAttachment attachment = new FileAttachment("profile_uuid_img.png", mockMultipartFile("profile.png"), user, "https://cdn.example.com/profile.png");
        fileAttachmentRepository.save(attachment);
        attachmentMappingRepository.save(new AttachmentMapping(attachment, EntityType.PROFILE, user.getUserProfile().getId()));

        // when: 탈퇴 처리
        accountService.deleteUser(user.getId());

        // then: 상태 및 개인정보 마스킹 검증
        User deleted = userRepository.findById(user.getId()).orElseThrow();
        assertThat(deleted.getUserStatus()).isEqualTo(UserStatus.DELETED);
        assertThat(deleted.getUsername()).startsWith("deleted_");
        assertThat(deleted.getEmail()).startsWith("deleted_");
        assertThat(deleted.getProvider()).startsWith("deleted_");
        assertThat(deleted.getProviderId()).startsWith("deleted_");
        assertThat(deleted.getUserProfile().getNickname()).isEqualTo("탈퇴한 회원");

        UserProfile profile = deleted.getUserProfile();
        assertThat(profile.getNickname()).isEqualTo("탈퇴한 회원");
        assertThat(profile.getProfileImageUrl()).isNull();
        assertThat(profile.getBio()).isNull();
        assertThat(profile.getBirthDate()).isNull();

        // 프로필 이미지 및 매핑 삭제 검증
        assertThat(attachmentMappingRepository.findByEntityTypeAndEntityId(EntityType.PROFILE, user.getUserProfile().getId())).isEmpty();
        assertThat(fileAttachmentRepository.findByPublicURL("https://cdn.example.com/profile.png")).isEmpty();
    }

    @Test
    @DisplayName("이미 탈퇴된 회원 탈퇴 시도 → USER_ALREADY_DELETED 예외")
    void deleteUser_alreadyDeleted() {
        // given: 상태 DELETED 유저 저장
        User user = User.createUser("deleteduser", "deleteduser@example.com", passwordEncoder.encode("P@ssw0rd!"));
        user.setUserProfile(new UserProfile(user, "닉네임", null, null, null, 0));
        user.setUserStatus(UserStatus.DELETED);
        userRepository.save(user);

        // when & then
        assertThatThrownBy(() -> accountService.deleteUser(user.getId()))
                .isInstanceOf(CustomException.class)
                .hasMessage(ErrorCode.USER_DELETED.getMessage());
    }

    @Test
    @DisplayName("정지된 회원 탈퇴 시도 → USER_SUSPENDED 예외")
    void deleteUser_suspendedUser() {
        // given: 상태 SUSPENDED 유저 저장
        User user = User.createUser("suspendeduser", "suspendeduser@example.com", passwordEncoder.encode("P@ssw0rd!"));
        user.setUserProfile(new UserProfile(user, "닉네임", null, null, null, 0));
        user.setUserStatus(UserStatus.SUSPENDED);
        userRepository.save(user);

        // when & then
        assertThatThrownBy(() -> accountService.deleteUser(user.getId()))
                .isInstanceOf(CustomException.class)
                .hasMessage(ErrorCode.USER_SUSPENDED.getMessage());
    }

    @Test
    @DisplayName("존재하지 않는 회원 탈퇴 시도 → USER_NOT_FOUND 예외")
    void deleteUser_notFound() {
        // when & then
        assertThatThrownBy(() -> accountService.deleteUser(999L))
                .isInstanceOf(CustomException.class)
                .hasMessage(ErrorCode.USER_NOT_FOUND.getMessage());
    }

    // ====================== 내 게시글 목록 조회 테스트 ======================

    @Test
    @DisplayName("내 게시글 목록 조회 성공")
    void getMyPosts_success() {
        // given
        User user = User.createUser("writer", "writer@example.com", passwordEncoder.encode("P@ssw0rd!"));
        user.setUserProfile(new UserProfile(user, "닉네임", null, null, null, 0));
        user.setUserStatus(UserStatus.ACTIVE);
        userRepository.save(user);

        // 게시글 2개 작성
        Post post1 = new Post(user, "제목1", "내용1", null);
        Post post2 = new Post(user, "제목2", "내용2", null);
        postRepository.saveAll(List.of(post1, post2));

        Pageable pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt"));

        // when
        PageResponse<PostListResponse> response = accountService.getMyPosts(user.getId(), pageable);

        // then
        assertThat(response.items()).hasSize(2);
        assertThat(response.items().get(0).getTitle()).isEqualTo("제목2"); // 최신순 정렬
        assertThat(response.items().get(1).getTitle()).isEqualTo("제목1");
    }

    @Test
    @DisplayName("존재하지 않는 사용자 ID → USER_NOT_FOUND 예외 발생")
    void getMyPosts_userNotFound() {
        // given
        Pageable pageable = PageRequest.of(0, 10);

        // when & then
        assertThatThrownBy(() -> accountService.getMyPosts(999L, pageable))
                .isInstanceOf(CustomException.class)
                .hasMessage(ErrorCode.USER_NOT_FOUND.getMessage());
    }

    @Test
    @DisplayName("탈퇴된 사용자 → USER_DELETED 예외 발생")
    void getMyPosts_deletedUser() {
        // given
        User user = User.createUser("deleted", "deleted@example.com", passwordEncoder.encode("P@ssw0rd!"));
        user.setUserProfile(new UserProfile(user, "닉네임", null, null, null, 0));
        user.setUserStatus(UserStatus.DELETED);
        userRepository.save(user);

        Pageable pageable = PageRequest.of(0, 10);

        // when & then
        assertThatThrownBy(() -> accountService.getMyPosts(user.getId(), pageable))
                .isInstanceOf(CustomException.class)
                .hasMessage(ErrorCode.USER_DELETED.getMessage());
    }

    @Test
    @DisplayName("정지된 사용자 → USER_SUSPENDED 예외 발생")
    void getMyPosts_suspendedUser() {
        // given
        User user = User.createUser("suspended", "suspended@example.com", passwordEncoder.encode("P@ssw0rd!"));
        user.setUserProfile(new UserProfile(user, "닉네임", null, null, null, 0));
        user.setUserStatus(UserStatus.SUSPENDED);
        userRepository.save(user);

        Pageable pageable = PageRequest.of(0, 10);

        // when & then
        assertThatThrownBy(() -> accountService.getMyPosts(user.getId(), pageable))
                .isInstanceOf(CustomException.class)
                .hasMessage(ErrorCode.USER_SUSPENDED.getMessage());
    }

    // ====================== 내 댓글 목록 조회 테스트 ======================

    @Test
    @DisplayName("내 댓글 목록 조회 성공")
    void getMyComments_success() {
        // given
        User user = User.createUser("commenter", "commenter@example.com", passwordEncoder.encode("P@ssw0rd!"));
        user.setUserProfile(new UserProfile(user, "닉네임", null, null, null, 0));
        user.setUserStatus(UserStatus.ACTIVE);
        userRepository.save(user);

        // 게시글 하나 생성
        Post post = new Post(user, "테스트 게시글", "게시글 내용", null);
        postRepository.save(post);

        // 댓글 2개 작성
        Comment comment1 = new Comment(post, user, "첫 번째 댓글", null);
        Comment comment2 = new Comment(post, user, "두 번째 댓글", null);
        commentRepository.saveAll(List.of(comment1, comment2));

        Pageable pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt"));

        // when
        PageResponse<MyCommentResponse> response = accountService.getMyComments(user.getId(), pageable);

        // then
        assertThat(response.items()).hasSize(2);
        assertThat(response.items().get(0).content()).isEqualTo("두 번째 댓글"); // 최신순 정렬
        assertThat(response.items().get(1).content()).isEqualTo("첫 번째 댓글");
    }

    @Test
    @DisplayName("존재하지 않는 사용자 ID → USER_NOT_FOUND 예외 발생")
    void getMyComments_userNotFound() {
        // given
        Pageable pageable = PageRequest.of(0, 10);

        // when & then
        assertThatThrownBy(() -> accountService.getMyComments(999L, pageable))
                .isInstanceOf(CustomException.class)
                .hasMessage(ErrorCode.USER_NOT_FOUND.getMessage());
    }

    @Test
    @DisplayName("탈퇴된 사용자 → USER_DELETED 예외 발생")
    void getMyComments_deletedUser() {
        // given
        User user = User.createUser("deleted", "deleted@example.com", passwordEncoder.encode("P@ssw0rd!"));
        user.setUserProfile(new UserProfile(user, "닉네임", null, null, null, 0));
        user.setUserStatus(UserStatus.DELETED);
        userRepository.save(user);

        Pageable pageable = PageRequest.of(0, 10);

        // when & then
        assertThatThrownBy(() -> accountService.getMyComments(user.getId(), pageable))
                .isInstanceOf(CustomException.class)
                .hasMessage(ErrorCode.USER_DELETED.getMessage());
    }

    @Test
    @DisplayName("정지된 사용자 → USER_SUSPENDED 예외 발생")
    void getMyComments_suspendedUser() {
        // given
        User user = User.createUser("suspended", "suspended@example.com", passwordEncoder.encode("P@ssw0rd!"));
        user.setUserProfile(new UserProfile(user, "닉네임", null, null, null, 0));
        user.setUserStatus(UserStatus.SUSPENDED);
        userRepository.save(user);

        Pageable pageable = PageRequest.of(0, 10);

        // when & then
        assertThatThrownBy(() -> accountService.getMyComments(user.getId(), pageable))
                .isInstanceOf(CustomException.class)
                .hasMessage(ErrorCode.USER_SUSPENDED.getMessage());
    }

    // ====================== 내 북마크 게시글 목록 조회 테스트 ======================

    @Test
    @DisplayName("내 북마크 게시글 목록 조회 성공")
    void getMyBookmarks_success() {
        // given
        User user = User.createUser("bookmarkUser", "bookmark@example.com", passwordEncoder.encode("P@ssw0rd!"));
        user.setUserProfile(new UserProfile(user, "닉네임", null, null, null, 0));
        user.setUserStatus(UserStatus.ACTIVE);
        userRepository.save(user);

        Post post1 = new Post(user, "JPA 영속성 전이 완벽 정리", "내용1", null);
        Post post2 = new Post(user, "테스트 코드 작성 가이드", "내용2", null);
        postRepository.saveAll(List.of(post1, post2));

        PostBookmark bookmark1 = new PostBookmark(post1, user);
        PostBookmark bookmark2 = new PostBookmark(post2, user);
        postBookmarkRepository.saveAll(List.of(bookmark1, bookmark2));

        Pageable pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt"));

        // when
        PageResponse<PostListResponse> response = accountService.getMyBookmarks(user.getId(), pageable);

        // then
        assertThat(response.items()).hasSize(2);
        assertThat(response.items().get(0).getTitle()).isEqualTo("테스트 코드 작성 가이드"); // 최신순
        assertThat(response.items().get(1).getTitle()).isEqualTo("JPA 영속성 전이 완벽 정리");
    }

    @Test
    @DisplayName("존재하지 않는 사용자 → USER_NOT_FOUND 예외 발생")
    void getMyBookmarks_userNotFound() {
        Pageable pageable = PageRequest.of(0, 10);
        assertThatThrownBy(() -> accountService.getMyBookmarks(999L, pageable))
                .isInstanceOf(CustomException.class)
                .hasMessage(ErrorCode.USER_NOT_FOUND.getMessage());
    }

    @Test
    @DisplayName("탈퇴된 사용자 → USER_DELETED 예외 발생")
    void getMyBookmarks_deletedUser() {
        User user = User.createUser("deleted", "deleted@example.com", passwordEncoder.encode("P@ssw0rd!"));
        user.setUserStatus(UserStatus.DELETED);
        userRepository.save(user);

        Pageable pageable = PageRequest.of(0, 10);
        assertThatThrownBy(() -> accountService.getMyBookmarks(user.getId(), pageable))
                .isInstanceOf(CustomException.class)
                .hasMessage(ErrorCode.USER_DELETED.getMessage());
    }

    @Test
    @DisplayName("정지된 사용자 → USER_SUSPENDED 예외 발생")
    void getMyBookmarks_suspendedUser() {
        User user = User.createUser("suspended", "suspended@example.com", passwordEncoder.encode("P@ssw0rd!"));
        user.setUserStatus(UserStatus.SUSPENDED);
        userRepository.save(user);

        Pageable pageable = PageRequest.of(0, 10);
        assertThatThrownBy(() -> accountService.getMyBookmarks(user.getId(), pageable))
                .isInstanceOf(CustomException.class)
                .hasMessage(ErrorCode.USER_SUSPENDED.getMessage());
    }
}
