package com.back.global.initData;

import com.back.domain.board.comment.entity.Comment;
import com.back.domain.board.comment.repository.CommentRepository;
import com.back.domain.board.post.entity.Post;
import com.back.domain.board.post.entity.PostCategory;
import com.back.domain.board.post.repository.PostCategoryRepository;
import com.back.domain.board.post.repository.PostRepository;
import com.back.domain.user.entity.User;
import com.back.domain.user.entity.UserProfile;
import com.back.domain.user.entity.UserStatus;
import com.back.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Configuration
@Profile("default")
@RequiredArgsConstructor
public class DevInitData {
    private final UserRepository userRepository;
    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final PostCategoryRepository postCategoryRepository;
    private final PasswordEncoder passwordEncoder;
    private final Environment environment;

    @Bean
    ApplicationRunner DevInitDataApplicationRunner() {
        return args -> {
            initUsersAndPostsAndComments();
        };
    }

    @Transactional
    public void initUsersAndPostsAndComments() {
        if (userRepository.count() == 0) {
            // -------------------- 유저 --------------------
            User admin = User.createAdmin("admin", "admin@example.com", passwordEncoder.encode("12345678!"));
            admin.setUserProfile(new UserProfile(admin, "관리자", null, null, null, 0));
            userRepository.save(admin);

            User user1 = User.createUser("user1", "user1@example.com", passwordEncoder.encode("12345678!"));
            user1.setUserProfile(new UserProfile(user1, "사용자1", null, null, null, 0));
            user1.setUserStatus(UserStatus.ACTIVE);
            userRepository.save(user1);

            User user2 = User.createUser("user2", "user2@example.com", passwordEncoder.encode("12345678!"));
            user2.setUserProfile(new UserProfile(user2, "사용자2", null, null, null, 0));
            user2.setUserStatus(UserStatus.ACTIVE);
            userRepository.save(user2);

            User user3 = User.createUser("user3", "user3@example.com", passwordEncoder.encode("12345678!"));
            user3.setUserProfile(new UserProfile(user3, "사용자3", null, null, null, 0));
            user3.setUserStatus(UserStatus.ACTIVE);
            userRepository.save(user3);

            // -------------------- 게시글 --------------------
            createSamplePosts(user1, user2, user3);
        }
    }

    private void createSamplePosts(User user1, User user2, User user3) {
        Post post1 = new Post(user1,
                "[백엔드] 같이 스프링 공부하실 분 구해요!",
                "매주 토요일 오후 2시에 온라인으로 스터디 진행합니다.\n교재는 '스프링 완전정복'을 사용할 예정입니다.");
        attachCategories(post1, List.of("백엔드", "직장인", "5~10명"));

        Post post2 = new Post(user2,
                "[프론트엔드] 리액트 입문 스터디원 모집",
                "리액트 교재를 같이 읽고 실습해보는 스터디입니다. GitHub로 코드 리뷰도 진행합니다.");
        attachCategories(post2, List.of("프론트엔드", "대학생", "2~4명"));

        Post post3 = new Post(user2,
                "[CS] 컴퓨터 구조 스터디",
                "운영체제, 네트워크, 컴퓨터 구조 기본 개념을 함께 정리해요.\n스터디원 5명 정도 모집합니다.");
        attachCategories(post3, List.of("CS", "취준생", "5~10명"));

        Post post4 = new Post(user3,
                "[알고리즘] 백준 골드 도전 스터디",
                "매주 3문제씩 풀이, 코드 리뷰 및 전략 공유합니다.\n실력 향상을 목표로 합니다!");
        attachCategories(post4, List.of("알고리즘", "대학생", "5~10명"));

        Post post5 = new Post(user1,
                "[영어 회화] 직장인 아침 스터디",
                "출근 전 30분, 영어회화 연습 스터디입니다.\n줌으로 진행하고 서로 피드백 나눠요 :)");
        attachCategories(post5, List.of("영어 회화", "직장인", "2~4명"));

        postRepository.saveAll(List.of(post1, post2, post3, post4, post5));

        // -------------------- 댓글 --------------------
        createSampleComments(user1, user2, user3, post1, post2, post3);
    }

    private void createSampleComments(User user1, User user2, User user3, Post post1, Post post2, Post post3) {
        // Post1에 댓글
        Comment comment1 = new Comment(post1, user2, "저도 참여하고 싶어요!");
        Comment reply1 = new Comment(post1, user1, "좋아요 :) 디스코드 링크 드릴게요.", comment1);

        // Post2에 댓글
        Comment comment2 = new Comment(post2, user3, "스터디 모집 기간은 언제까지인가요?");
        Comment reply2 = new Comment(post2, user2, "이번 주 일요일까지 받을 예정이에요.", comment2);

        // Post3에 댓글
        Comment comment3 = new Comment(post3, user1, "CS는 항상 중요하죠 💪");

        commentRepository.saveAll(List.of(comment1, reply1, comment2, reply2, comment3));

        // 게시글 commentCount 반영
        post1.increaseCommentCount();
        post2.increaseCommentCount();
        post3.increaseCommentCount();

        postRepository.saveAll(List.of(post1, post2, post3));
    }

    private void attachCategories(Post post, List<String> categoryNames) {
        List<PostCategory> categories = postCategoryRepository.findAllByNameIn(categoryNames);
        if (!categories.isEmpty()) {
            post.updateCategories(categories);
        }
    }
}
